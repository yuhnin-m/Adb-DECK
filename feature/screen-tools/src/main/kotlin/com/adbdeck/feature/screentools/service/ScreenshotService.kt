package com.adbdeck.feature.screentools.service

import com.adbdeck.core.adb.api.screen.ScreenshotOptions

/**
 * Сервис создания скриншотов Android-устройства.
 */
interface ScreenshotService {

    /**
     * Снять скриншот с [deviceId] и сохранить в [localOutputPath].
     */
    suspend fun capture(
        deviceId: String,
        localOutputPath: String,
        adbPath: String,
        options: ScreenshotOptions,
    ): Result<Unit>
}
