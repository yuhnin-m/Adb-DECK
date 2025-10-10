package com.adbdeck.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ────────────────────────────────────────────────────────────────
// Базовая цветовая палитра ADB Deck
// ────────────────────────────────────────────────────────────────

/** Основной акцентный цвет — насыщенный синий */
val AdbDeckBlue = Color(0xFF1976D2)

/** Вторичный цвет */
val AdbDeckTeal = Color(0xFF00897B)

/** Цвет ошибки */
val AdbDeckRed = Color(0xFFD32F2F)

/** Цвет успеха */
val AdbDeckGreen = Color(0xFF388E3C)

/** Цвет предупреждения */
val AdbDeckAmber = Color(0xFFF57C00)

// ────────────────────────────────────────────────────────────────
// Светлая тема
// ────────────────────────────────────────────────────────────────

/** ColorScheme для светлой темы. */
val LightColorScheme = lightColorScheme(
    primary = AdbDeckBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),

    secondary = AdbDeckTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),

    error = AdbDeckRed,
    onError = Color.White,

    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1C1E),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),

    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Color(0xFF424242),

    outline = Color(0xFFBDBDBD),
)

// ────────────────────────────────────────────────────────────────
// Тёмная тема
// ────────────────────────────────────────────────────────────────

/** ColorScheme для тёмной темы. */
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF004D40),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFB2DFDB),

    error = Color(0xFFEF9A9A),
    onError = Color(0xFF7F0000),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),

    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),

    outline = Color(0xFF616161),
)
