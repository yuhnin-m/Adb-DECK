package com.adbdeck.feature.deeplinks.handlers

import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.adb.api.intents.LaunchResult
import com.adbdeck.feature.deeplinks.models.DeepLinksState
import com.adbdeck.feature.deeplinks.models.LaunchHistoryEntry
import java.util.UUID

internal class DeepLinksLaunchHandler(
    private val intentLaunchClient: IntentLaunchClient,
    private val maxHistorySize: Int,
) {

    fun computePreview(state: DeepLinksState, deviceId: String, adbPath: String): String =
        when (state.mode) {
            LaunchMode.DEEP_LINK -> intentLaunchClient.buildDeepLinkCommand(
                deviceId = deviceId,
                adbPath = adbPath,
                params = state.toDeepLinkParams(),
            )

            LaunchMode.INTENT -> intentLaunchClient.buildIntentCommand(
                deviceId = deviceId,
                adbPath = adbPath,
                params = state.toIntentParams(),
            )
        }

    suspend fun launch(state: DeepLinksState, deviceId: String, adbPath: String): Result<LaunchResult> =
        when (state.mode) {
            LaunchMode.DEEP_LINK -> intentLaunchClient.launchDeepLink(
                deviceId = deviceId,
                adbPath = adbPath,
                params = state.toDeepLinkParams(),
            )

            LaunchMode.INTENT -> intentLaunchClient.launchIntent(
                deviceId = deviceId,
                adbPath = adbPath,
                params = state.toIntentParams(),
            )
        }

    fun createHistoryEntry(state: DeepLinksState, launchResult: LaunchResult): LaunchHistoryEntry =
        LaunchHistoryEntry(
            id = UUID.randomUUID().toString(),
            mode = state.mode,
            deepLinkParams = if (state.mode == LaunchMode.DEEP_LINK) state.toDeepLinkParams() else null,
            intentParams = if (state.mode == LaunchMode.INTENT) state.toIntentParams() else null,
            launchedAt = System.currentTimeMillis(),
            commandPreview = launchResult.commandPreview,
            isSuccess = launchResult.isSuccess,
        )

    fun appendHistory(
        currentHistory: List<LaunchHistoryEntry>,
        newEntry: LaunchHistoryEntry,
    ): List<LaunchHistoryEntry> = (listOf(newEntry) + currentHistory).take(maxHistorySize)

    fun createFailureResult(state: DeepLinksState, error: Throwable): LaunchResult = LaunchResult(
        exitCode = -1,
        stdout = "",
        stderr = error.message ?: "Неизвестная ошибка",
        commandPreview = state.commandPreview,
    )
}
