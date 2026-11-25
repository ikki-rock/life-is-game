package com.example.timequest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FocusColor = Color(0xFFE5484D)
val ShortBreakColor = Color(0xFF2E9E5B)
val LongBreakColor = Color(0xFF3E7BFA)
val XpGold = Color(0xFFF5A524)

private val LightColors = lightColorScheme(
    primary = FocusColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3E1),
    onPrimaryContainer = Color(0xFF5C1013),
    secondary = Color(0xFFB4413F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFF6E9E6),
    surfaceContainer = Color(0xFFFBEDEA),
    surfaceContainerHigh = Color(0xFFF6E9E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A8E),
    onPrimary = Color(0xFF4A0A0E),
    primaryContainer = Color(0xFF6B1D22),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFFFB3AD),
    onSecondary = Color(0xFF571E1B),
    secondaryContainer = Color(0xFF73332F),
    onSecondaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B1413),
    surface = Color(0xFF1B1413),
    surfaceVariant = Color(0xFF2E2321),
    surfaceContainer = Color(0xFF261D1B),
    surfaceContainerHigh = Color(0xFF2E2321),
)

@Composable
fun TimeQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
