package com.libsvpn.tunnel.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.libsvpn.tunnel.model.AccentColor
import com.libsvpn.tunnel.model.ThemeMode

private data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
)

private fun palette(accent: AccentColor) = when (accent) {
    AccentColor.GREEN -> AccentPalette(
        Color(0xFF006C49), Color.White, Color(0xFF9FF6C8), Color(0xFF002114),
        Color(0xFF83D9AC), Color(0xFF003824), Color(0xFF005237), Color(0xFF9FF6C8)
    )
    AccentColor.BLUE -> AccentPalette(
        Color(0xFF0B57D0), Color.White, Color(0xFFD9E2FF), Color(0xFF001946),
        Color(0xFFA8C7FA), Color(0xFF002D6C), Color(0xFF0842A0), Color(0xFFD9E2FF)
    )
    AccentColor.PURPLE -> AccentPalette(
        Color(0xFF6750A4), Color.White, Color(0xFFE9DDFF), Color(0xFF22005D),
        Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378A), Color(0xFFE9DDFF)
    )
    AccentColor.ORANGE -> AccentPalette(
        Color(0xFF9A4522), Color.White, Color(0xFFFFDBCF), Color(0xFF380D00),
        Color(0xFFFFB59B), Color(0xFF5D1900), Color(0xFF7A2E0C), Color(0xFFFFDBCF)
    )
    AccentColor.RED -> AccentPalette(
        Color(0xFFB3261E), Color.White, Color(0xFFFFDAD6), Color(0xFF410002),
        Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFF93000A), Color(0xFFFFDAD6)
    )
    AccentColor.TEAL -> AccentPalette(
        Color(0xFF006A6A), Color.White, Color(0xFF9CF1F1), Color(0xFF002020),
        Color(0xFF80D5D5), Color(0xFF003737), Color(0xFF004F4F), Color(0xFF9CF1F1)
    )
    AccentColor.PINK -> AccentPalette(
        Color(0xFF9C3D70), Color.White, Color(0xFFFFD8E9), Color(0xFF3E0024),
        Color(0xFFFFACD3), Color(0xFF5C123A), Color(0xFF7B2953), Color(0xFFFFD8E9)
    )
}

@Composable
fun LibsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.GREEN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val p = palette(accent)
            if (dark) darkColorScheme(
                primary = p.darkPrimary,
                onPrimary = p.darkOnPrimary,
                primaryContainer = p.darkContainer,
                onPrimaryContainer = p.darkOnContainer,
                secondary = Color(0xFFB4CCBD),
                secondaryContainer = Color(0xFF354B3F),
                tertiary = Color(0xFFA5CDDD),
                background = Color(0xFF101413),
                surface = Color(0xFF131817),
                surfaceVariant = Color(0xFF414943),
                outline = Color(0xFF89938C),
                error = Color(0xFFFFB4AB)
            ) else lightColorScheme(
                primary = p.lightPrimary,
                onPrimary = p.lightOnPrimary,
                primaryContainer = p.lightContainer,
                onPrimaryContainer = p.lightOnContainer,
                secondary = Color(0xFF4D6357),
                secondaryContainer = Color(0xFFD0E8D8),
                tertiary = Color(0xFF3D6472),
                background = Color(0xFFF7FAF8),
                surface = Color(0xFFFCFEFD),
                surfaceVariant = Color(0xFFDDE5DF),
                outline = Color(0xFF6F7973),
                error = Color(0xFFBA1A1A)
            )
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
