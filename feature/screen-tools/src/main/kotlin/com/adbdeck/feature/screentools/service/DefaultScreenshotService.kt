package com.adbdeck.feature.screentools.service

import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.screen.ScreenshotOptions

/**
 * Реализация [ScreenshotService] поверх [ScreenToolsClient].
 */
class DefaultScreenshotService(
    private val screenToolsClient: ScreenToolsClient,
) : ScreenshotService {

    override suspend fun capture(
        deviceId: String,
        localOutputPath: String,
        adbPath: String,
        options: ScreenshotOptions,
    ): Result<Unit> =
        screenToolsClient.takeScreenshot(
            deviceId = deviceId,
            localOutputPath = localOutputPath,
            adbPath = adbPath,
            options = options,
        )
}
