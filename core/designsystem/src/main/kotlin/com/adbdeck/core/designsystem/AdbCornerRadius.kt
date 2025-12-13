package com.adbdeck.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Набор фиксированных радиусов скругления для компонентов интерфейса.
 *
 * Используется как единый источник радиусов:
 * - в теме Material ([AdbDeckTheme]);
 * - в универсальных компонентах `core/ui`.
 *
 * @param value Радиус в dp.
 */
enum class AdbCornerRadius(val value: Dp) {
    /** Без скругления. */
    NONE(0.dp),

    /** Небольшое скругление. */
    SMALL(6.dp),

    /** Базовое скругление по умолчанию. */
    MEDIUM(10.dp),

    /** Выраженное скругление. */
    LARGE(14.dp),

    /** Сильное скругление. */
    XLARGE(18.dp),
}
