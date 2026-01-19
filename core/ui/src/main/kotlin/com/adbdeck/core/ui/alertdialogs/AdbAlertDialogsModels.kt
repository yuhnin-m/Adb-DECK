package com.adbdeck.core.ui.alertdialogs

import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType

/**
 * Конфигурация action-кнопки для [AdbAlertDialog].
 *
 * @param text Текст кнопки.
 * @param onClick Callback нажатия.
 * @param enabled Флаг доступности.
 * @param loading Показывать loader внутри кнопки.
 * @param type Цветовой тип кнопки.
 * @param size Размер кнопки.
 * @param cornerRadius Радиус скругления.
 */
data class AdbAlertDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val type: AdbButtonType = AdbButtonType.NEUTRAL,
    val size: AdbButtonSize = AdbButtonSize.MEDIUM,
    val cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
)
