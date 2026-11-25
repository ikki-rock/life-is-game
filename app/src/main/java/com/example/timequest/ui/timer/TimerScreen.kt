package com.example.timequest.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timequest.data.Progress
import com.example.timequest.data.TimerSettings
import com.example.timequest.ui.theme.FocusColor
import com.example.timequest.ui.theme.LongBreakColor
import com.example.timequest.ui.theme.ShortBreakColor
import java.util.Locale

private fun TimerMode.color(): Color = when (this) {
    TimerMode.FOCUS -> FocusColor
    TimerMode.SHORT_BREAK -> ShortBreakColor
    TimerMode.LONG_BREAK -> LongBreakColor
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms + 999) / 1000 // 올림: 0초가 되는 순간 끝나도록
    return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
}

@Composable
fun TimerScreen(viewModel: TimerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val progress by viewModel.progress.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🍅 뽀모도로",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showSettings = true }) { Text("⚙️ 시간 설정") }
        }

        Spacer(Modifier.height(8.dp))

        // 모드 선택
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { viewModel.selectMode(mode) },
                    label = { Text(mode.label) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        TimerRing(state = state)

        Spacer(Modifier.height(20.dp))

        CycleDots(done = state.completedInCycle, total = settings.sessionsBeforeLongBreak, color = state.mode.color())

        Spacer(Modifier.height(24.dp))

        // 컨트롤
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = viewModel::reset) { Text("처음으로") }
            Button(
                onClick = viewModel::toggle,
                modifier = Modifier
                    .widthIn(min = 140.dp)
                    .height(56.dp),
            ) {
                Text(
                    text = when {
                        state.running -> "일시정지"
                        state.remainingMs < state.totalMs -> "계속"
                        else -> "시작"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            OutlinedButton(onClick = viewModel::skip) { Text("건너뛰기") }
        }

        Spacer(Modifier.height(28.dp))

        TodayCard(progress = progress)
    }

    if (showSettings) {
        SettingsDialog(
            current = settings,
            onDismiss = { showSettings = false },
            onSave = {
                viewModel.updateSettings(it)
                showSettings = false
            },
        )
    }
}

@Composable
private fun TimerRing(state: TimerUiState) {
    val color = state.mode.color()
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            // 남은 시간만큼 호가 줄어든다
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (1f - state.progress),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.mode.label,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatTime(state.remainingMs),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CycleDots(done: Int, total: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (i < done) color else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun TodayCard(progress: Progress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "오늘 완료한 집중 ${progress.focusToday}회",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "집중 1회 완료 시 +${Progress.FOCUS_REWARD_XP} XP를 받아요. 퀘스트 탭에서 경험치를 확인하세요!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsDialog(
    current: TimerSettings,
    onDismiss: () -> Unit,
    onSave: (TimerSettings) -> Unit,
) {
    var s by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper("집중", s.focusMin, "분", 1..120, 5) { s = s.copy(focusMin = it) }
                Stepper("짧은 휴식", s.shortBreakMin, "분", 1..60, 1) { s = s.copy(shortBreakMin = it) }
                Stepper("긴 휴식", s.longBreakMin, "분", 1..90, 5) { s = s.copy(longBreakMin = it) }
                Stepper("긴 휴식 주기", s.sessionsBeforeLongBreak, "회", 2..8, 1) {
                    s = s.copy(sessionsBeforeLongBreak = it)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(s) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

/** − / + 버튼으로 값을 조절하는 간단한 스테퍼 */
@Composable
private fun Stepper(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton(
            onClick = { onChange((value - step).coerceAtLeast(range.first)) },
            enabled = value > range.first,
        ) { Text("−", fontSize = 20.sp) }
        Text(
            text = "$value$unit",
            modifier = Modifier.widthIn(min = 56.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + step).coerceAtMost(range.last)) },
            enabled = value < range.last,
        ) { Text("+", fontSize = 20.sp) }
    }
}
