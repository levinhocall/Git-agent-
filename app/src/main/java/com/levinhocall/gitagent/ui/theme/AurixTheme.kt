package com.levinhocall.gitagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AurixDarkScheme = darkColorScheme(
    primary = Color(0xFF8AA4FF),
    onPrimary = Color(0xFF04112E),
    secondary = Color(0xFF89D5FF),
    background = Color(0xFF05070E),
    onBackground = Color(0xFFDCE7FF),
    surface = Color(0xFF121A2B),
    onSurface = Color(0xFFDCE7FF),
    error = Color(0xFFFF8A80)
)

@Composable
fun AurixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AurixDarkScheme,
        content = content
    )
}
