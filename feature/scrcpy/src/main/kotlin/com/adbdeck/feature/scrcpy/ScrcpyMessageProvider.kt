package com.adbdeck.feature.scrcpy

import adbdeck.feature.scrcpy.generated.resources.Res
import adbdeck.feature.scrcpy.generated.resources.scrcpy_error_not_configured
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_crashed
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_no_device
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_start_failed
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_started
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_stop_failed
import adbdeck.feature.scrcpy.generated.resources.scrcpy_feedback_stopped
import adbdeck.feature.scrcpy.generated.resources.scrcpy_validation_window_height_range
import adbdeck.feature.scrcpy.generated.resources.scrcpy_validation_window_width_range
import org.jetbrains.compose.resources.getString

/**
 * Провайдер локализованных сообщений для [DefaultScrcpyComponent].
 *
 * Выделен отдельно, чтобы компонент можно было тестировать без инициализации Compose/Skiko.
 */
interface ScrcpyMessageProvider {
    suspend fun noDevice(): String
    suspend fun notConfigured(): String
    suspend fun started(): String
    suspend fun stopped(): String
    suspend fun startFailed(reason: String): String
    suspend fun stopFailed(reason: String): String
    suspend fun crashed(reason: String): String
    suspend fun windowWidthRange(min: Int, max: Int): String
    suspend fun windowHeightRange(min: Int, max: Int): String
}

internal object ResourceScrcpyMessageProvider : ScrcpyMessageProvider {
    override suspend fun noDevice(): String = getString(Res.string.scrcpy_feedback_no_device)

    override suspend fun notConfigured(): String = getString(Res.string.scrcpy_error_not_configured)

    override suspend fun started(): String = getString(Res.string.scrcpy_feedback_started)

    override suspend fun stopped(): String = getString(Res.string.scrcpy_feedback_stopped)

    override suspend fun startFailed(reason: String): String =
        getString(Res.string.scrcpy_feedback_start_failed, reason)

    override suspend fun stopFailed(reason: String): String =
        getString(Res.string.scrcpy_feedback_stop_failed, reason)

    override suspend fun crashed(reason: String): String =
        getString(Res.string.scrcpy_feedback_crashed, reason)

    override suspend fun windowWidthRange(min: Int, max: Int): String =
        getString(Res.string.scrcpy_validation_window_width_range, min, max)

    override suspend fun windowHeightRange(min: Int, max: Int): String =
        getString(Res.string.scrcpy_validation_window_height_range, min, max)
}
