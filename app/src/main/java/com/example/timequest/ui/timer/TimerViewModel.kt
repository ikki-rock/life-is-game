package com.example.timequest.ui.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timequest.data.QuestReward
import com.example.timequest.data.Store
import com.example.timequest.data.TimerSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerMode(val label: String) {
    FOCUS("집중"),
    SHORT_BREAK("짧은 휴식"),
    LONG_BREAK("긴 휴식"),
}

data class TimerUiState(
    val mode: TimerMode = TimerMode.FOCUS,
    val totalMs: Long = 25 * 60_000L,
    val remainingMs: Long = 25 * 60_000L,
    val running: Boolean = false,
    /** 현재 사이클에서 끝낸 집중 횟수 */
    val completedInCycle: Int = 0,
) {
    val progress: Float
        get() = if (totalMs <= 0) 0f else (1f - remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

/** 타이머가 끝났을 때 UI(소리/진동/안내)에 전달하는 이벤트 */
data class TimerFinished(val finishedMode: TimerMode, val nextMode: TimerMode, val reward: QuestReward?)

class TimerViewModel(private val store: Store) : ViewModel() {

    private val _state = MutableStateFlow(initialState(store.settings.value))
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    val settings: StateFlow<TimerSettings> = store.settings
    val progress = store.progress

    private val _events = Channel<TimerFinished>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var tickJob: Job? = null

    private fun initialState(s: TimerSettings): TimerUiState {
        val ms = s.focusMin * 60_000L
        return TimerUiState(mode = TimerMode.FOCUS, totalMs = ms, remainingMs = ms)
    }

    private fun durationOf(mode: TimerMode, s: TimerSettings = store.settings.value): Long = when (mode) {
        TimerMode.FOCUS -> s.focusMin
        TimerMode.SHORT_BREAK -> s.shortBreakMin
        TimerMode.LONG_BREAK -> s.longBreakMin
    } * 60_000L

    fun toggle() = if (_state.value.running) pause() else start()

    fun start() {
        if (_state.value.running) return
        val endAt = SystemClock.elapsedRealtime() + _state.value.remainingMs
        _state.update { it.copy(running = true) }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            // 실제 경과 시간 기준으로 계산해서 백그라운드에서도 오차가 쌓이지 않게 한다.
            while (true) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    finish()
                    break
                }
                _state.update { it.copy(remainingMs = left) }
                delay(200)
            }
        }
    }

    fun pause() {
        tickJob?.cancel()
        _state.update { it.copy(running = false) }
    }

    /** 현재 모드의 시간을 처음으로 되돌린다. */
    fun reset() {
        tickJob?.cancel()
        val total = durationOf(_state.value.mode)
        _state.update { it.copy(running = false, totalMs = total, remainingMs = total) }
    }

    /** 보상 없이 다음 단계로 넘어간다. */
    fun skip() {
        tickJob?.cancel()
        moveToNext(countCycle = false)
    }

    fun selectMode(mode: TimerMode) {
        tickJob?.cancel()
        val total = durationOf(mode)
        _state.update { it.copy(mode = mode, running = false, totalMs = total, remainingMs = total) }
    }

    fun updateSettings(newSettings: TimerSettings) {
        store.updateSettings(newSettings)
        // 멈춰 있을 때만 즉시 반영(진행 중인 타이머는 건드리지 않음)
        if (!_state.value.running) reset()
    }

    private suspend fun finish() {
        val finished = _state.value.mode
        val reward = if (finished == TimerMode.FOCUS) store.completeFocusSession() else null
        val next = moveToNext(countCycle = true)
        _events.send(TimerFinished(finished, next, reward))
    }

    private fun moveToNext(countCycle: Boolean): TimerMode {
        val s = store.settings.value
        val cur = _state.value
        var cycle = cur.completedInCycle
        val next: TimerMode
        if (cur.mode == TimerMode.FOCUS) {
            if (countCycle) cycle += 1
            next = if (cycle >= s.sessionsBeforeLongBreak) TimerMode.LONG_BREAK else TimerMode.SHORT_BREAK
        } else {
            if (cur.mode == TimerMode.LONG_BREAK) cycle = 0
            next = TimerMode.FOCUS
        }
        val total = durationOf(next, s)
        _state.value = TimerUiState(
            mode = next,
            totalMs = total,
            remainingMs = total,
            running = false,
            completedInCycle = cycle,
        )
        return next
    }
}
