package com.example.timequest

import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timequest.ui.quest.QuestScreen
import com.example.timequest.ui.quest.QuestViewModel
import com.example.timequest.ui.theme.TimeQuestTheme
import com.example.timequest.ui.timer.TimerMode
import com.example.timequest.ui.timer.TimerScreen
import com.example.timequest.ui.timer.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as TimeQuestApp).store
        setContent {
            TimeQuestTheme {
                MainScreen(store = store)
            }
        }
    }

    private fun alertTimerFinished() {
        // 진동
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 500), -1))
        // 알림음
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        }
    }

    @Composable
    private fun MainScreen(store: com.example.timequest.data.Store) {
        val timerVm: TimerViewModel = viewModel { TimerViewModel(store) }
        val questVm: QuestViewModel = viewModel { QuestViewModel(store) }
        val snackbarHost = remember { SnackbarHostState() }
        var tab by rememberSaveable { mutableIntStateOf(0) }

        // 타이머가 켜져 있는 동안 화면이 꺼지지 않도록 유지
        val timerState by timerVm.state.collectAsState()
        val context = LocalContext.current
        DisposableEffect(timerState.running) {
            val window = (context as? ComponentActivity)?.window
            if (timerState.running) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }

        // 타이머 종료 알림
        LaunchedEffect(Unit) {
            timerVm.events.collect { e ->
                alertTimerFinished()
                val msg = when (e.finishedMode) {
                    TimerMode.FOCUS -> {
                        val reward = e.reward
                        val xp = if (reward != null) " +${reward.xpDelta} XP" else ""
                        val lvl = if (reward?.leveledUp == true) " 🎉 Lv.${reward.newLevel}!" else ""
                        "집중 완료!$xp$lvl ${e.nextMode.label}을 시작해 보세요"
                    }
                    else -> "휴식 끝! 다시 집중할 시간이에요"
                }
                snackbarHost.currentSnackbarData?.dismiss()
                snackbarHost.showSnackbar(msg)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Text("🍅") },
                        label = { Text("타이머") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Text("⚔️") },
                        label = { Text("퀘스트") },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> TimerScreen(viewModel = timerVm)
                    else -> QuestScreen(viewModel = questVm, snackbarHost = snackbarHost)
                }
            }
        }
    }
}
