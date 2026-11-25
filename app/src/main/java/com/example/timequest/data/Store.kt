package com.example.timequest.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * 퀘스트 / 경험치 / 타이머 설정을 SharedPreferences(JSON)에 저장하는 단일 저장소.
 * 타이머 탭과 퀘스트 탭이 같은 경험치를 공유한다.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("timequest", Context.MODE_PRIVATE)

    private val _quests = MutableStateFlow(loadQuests())
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _progress = MutableStateFlow(loadProgress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()

    // ---------- 퀘스트 ----------

    fun addQuest(title: String, difficulty: Difficulty): Quest? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val quest = Quest(id = UUID.randomUUID().toString(), title = trimmed, difficulty = difficulty)
        // 새 퀘스트는 목록 맨 위에 추가
        saveQuests(listOf(quest) + _quests.value)
        return quest
    }

    fun updateQuest(id: String, title: String, difficulty: Difficulty) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        saveQuests(_quests.value.map {
            if (it.id == id) it.copy(title = trimmed, difficulty = difficulty) else it
        })
    }

    fun setDone(id: String, done: Boolean): QuestReward? {
        val quest = _quests.value.firstOrNull { it.id == id } ?: return null
        if (quest.done == done) return null

        val before = _progress.value
        val delta: Int
        if (done) {
            delta = quest.difficulty.xp
            saveQuests(_quests.value.map { if (it.id == id) it.copy(done = true, awardedXp = delta) else it })
        } else {
            delta = -quest.awardedXp
            saveQuests(_quests.value.map { if (it.id == id) it.copy(done = false, awardedXp = 0) else it })
        }
        val newXp = (before.xp + delta).coerceAtLeast(0)
        saveProgress(before.copy(xp = newXp))
        val after = _progress.value
        return QuestReward(delta, leveledUp = after.level > before.level, newLevel = after.level)
    }

    fun deleteQuest(id: String) {
        saveQuests(_quests.value.filterNot { it.id == id })
    }

    /** 삭제 취소(되돌리기)용. 원래 위치에 복원한다. */
    fun restoreQuest(quest: Quest, index: Int) {
        if (_quests.value.any { it.id == quest.id }) return
        val list = _quests.value.toMutableList()
        list.add(index.coerceIn(0, list.size), quest)
        saveQuests(list)
    }

    fun clearCompleted(): Int {
        val remaining = _quests.value.filterNot { it.done }
        val removed = _quests.value.size - remaining.size
        if (removed > 0) saveQuests(remaining)
        return removed
    }

    // ---------- 경험치 / 타이머 ----------

    fun completeFocusSession(): QuestReward {
        val before = refreshedProgress()
        saveProgress(before.copy(xp = before.xp + Progress.FOCUS_REWARD_XP, focusToday = before.focusToday + 1))
        val after = _progress.value
        return QuestReward(Progress.FOCUS_REWARD_XP, after.level > before.level, after.level)
    }

    fun updateSettings(settings: TimerSettings) {
        val fixed = TimerSettings(
            focusMin = settings.focusMin.coerceIn(1, 120),
            shortBreakMin = settings.shortBreakMin.coerceIn(1, 60),
            longBreakMin = settings.longBreakMin.coerceIn(1, 90),
            sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak.coerceIn(2, 8),
        )
        _settings.value = fixed
        prefs.edit()
            .putInt(K_FOCUS, fixed.focusMin)
            .putInt(K_SHORT, fixed.shortBreakMin)
            .putInt(K_LONG, fixed.longBreakMin)
            .putInt(K_CYCLE, fixed.sessionsBeforeLongBreak)
            .apply()
    }

    // ---------- 내부 저장/로드 ----------

    private fun saveQuests(list: List<Quest>) {
        _quests.value = list
        val arr = JSONArray()
        list.forEach { q ->
            arr.put(
                JSONObject()
                    .put("id", q.id)
                    .put("title", q.title)
                    .put("difficulty", q.difficulty.name)
                    .put("done", q.done)
                    .put("awardedXp", q.awardedXp)
                    .put("createdAt", q.createdAt)
            )
        }
        prefs.edit().putString(K_QUESTS, arr.toString()).apply()
    }

    private fun loadQuests(): List<Quest> {
        val raw = prefs.getString(K_QUESTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Quest(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    difficulty = runCatching { Difficulty.valueOf(o.getString("difficulty")) }
                        .getOrDefault(Difficulty.NORMAL),
                    done = o.optBoolean("done", false),
                    awardedXp = o.optInt("awardedXp", 0),
                    createdAt = o.optLong("createdAt", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun refreshedProgress(): Progress {
        val today = LocalDate.now().toString()
        val current = _progress.value
        val savedDate = prefs.getString(K_FOCUS_DATE, today)
        return if (savedDate == today) current else current.copy(focusToday = 0)
    }

    private fun saveProgress(p: Progress) {
        _progress.value = p
        prefs.edit()
            .putInt(K_XP, p.xp)
            .putInt(K_FOCUS_TODAY, p.focusToday)
            .putString(K_FOCUS_DATE, LocalDate.now().toString())
            .apply()
    }

    private fun loadProgress(): Progress {
        val today = LocalDate.now().toString()
        val focusToday = if (prefs.getString(K_FOCUS_DATE, today) == today) prefs.getInt(K_FOCUS_TODAY, 0) else 0
        return Progress(xp = prefs.getInt(K_XP, 0), focusToday = focusToday)
    }

    private fun loadSettings() = TimerSettings(
        focusMin = prefs.getInt(K_FOCUS, 25),
        shortBreakMin = prefs.getInt(K_SHORT, 5),
        longBreakMin = prefs.getInt(K_LONG, 15),
        sessionsBeforeLongBreak = prefs.getInt(K_CYCLE, 4),
    )

    private companion object {
        const val K_QUESTS = "quests"
        const val K_XP = "xp"
        const val K_FOCUS_TODAY = "focusToday"
        const val K_FOCUS_DATE = "focusDate"
        const val K_FOCUS = "focusMin"
        const val K_SHORT = "shortMin"
        const val K_LONG = "longMin"
        const val K_CYCLE = "cycle"
    }
}
