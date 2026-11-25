package com.example.timequest.data

/** 퀘스트 난이도. 완료 시 받는 경험치(XP)가 달라진다. */
enum class Difficulty(val label: String, val emoji: String, val xp: Int) {
    EASY("쉬움", "🌱", 10),
    NORMAL("보통", "⚔️", 20),
    HARD("어려움", "🔥", 40),
}

data class Quest(
    val id: String,
    val title: String,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val done: Boolean = false,
    /** 완료 시점에 실제로 지급된 XP. 체크 해제 시 정확히 회수하기 위해 저장한다. */
    val awardedXp: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 뽀모도로 타이머 설정(분 단위). */
data class TimerSettings(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    /** 몇 번 집중하면 긴 휴식을 가질지 */
    val sessionsBeforeLongBreak: Int = 4,
)

data class Progress(
    val xp: Int = 0,
    val focusToday: Int = 0,
) {
    val level: Int get() = xp / XP_PER_LEVEL + 1
    val xpInLevel: Int get() = xp % XP_PER_LEVEL

    val title: String
        get() = when {
            level >= 20 -> "전설의 용사"
            level >= 15 -> "영웅"
            level >= 10 -> "베테랑 모험가"
            level >= 5 -> "숙련 모험가"
            level >= 3 -> "견습 모험가"
            else -> "새내기 모험가"
        }

    companion object {
        const val XP_PER_LEVEL = 100
        /** 집중 세션 1회 완료 보상 */
        const val FOCUS_REWARD_XP = 15
    }
}

/** 퀘스트를 체크/해제했을 때의 결과(UI 피드백용). */
data class QuestReward(val xpDelta: Int, val leveledUp: Boolean, val newLevel: Int)
