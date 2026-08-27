package com.vironite.virogallery.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // Keep the player/gallery visually close to the supplied dark UI while
    // allowing the Settings page to control the shared UI colors.
    val context = androidx.compose.ui.platform.LocalContext.current
    val p = context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE)
    val background = Color(p.getInt("bg_color", 0xFF101114.toInt()))
    val card = Color(p.getInt("card_color", 0xFF1F1F1F.toInt()))
    val accent = Color(p.getInt("accent_color", 0xFF4D90FF.toInt()))
    val scheme = darkColorScheme(
        primary = accent,
        secondary = accent,
        background = background,
        surface = card,
        surfaceVariant = card,
        primaryContainer = accent.copy(alpha = .20f)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
