package com.adbdeck.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Размеры и отступы, используемые в ADB Deck.
 *
 * Centralизованные константы упрощают единообразие интерфейса
 * и позволяют при необходимости масштабировать UI.
 */
object Dimensions {

    // ── Отступы ────────────────────────────────────────────────
    /** Минимальный отступ (4 dp). */
    val paddingXSmall = 4.dp

    /** Маленький отступ (8 dp). */
    val paddingSmall = 8.dp

    /** Средний отступ (12 dp). */
    val paddingMedium = 12.dp

    /** Стандартный отступ (16 dp). */
    val paddingDefault = 16.dp

    /** Большой отступ (24 dp). */
    val paddingLarge = 24.dp

    /** Очень большой отступ (32 dp). */
    val paddingXLarge = 32.dp

    // ── Размеры элементов ──────────────────────────────────────
    /** Ширина боковой панели. */
    val sidebarWidth = 220.dp

    /** Высота верхней панели. */
    val topBarHeight = 48.dp

    /** Высота строки состояния. */
    val statusBarHeight = 28.dp

    /** Высота пункта бокового меню. */
    val navItemHeight = 44.dp

    /** Радиус скругления карточек. */
    val cardCornerRadius = AdbCornerRadius.LARGE.value

    /** Радиус скругления кнопок и чипов. */
    val buttonCornerRadius = AdbCornerRadius.MEDIUM.value

    // ── Иконки ────────────────────────────────────────────────
    /** Стандартный размер иконок в навигации. */
    val iconSizeNav = 20.dp

    /** Стандартный размер иконок в карточках. */
    val iconSizeCard = 24.dp

    /** Большие иконки (для пустых состояний). */
    val iconSizeLarge = 48.dp
}
