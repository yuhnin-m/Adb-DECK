package com.adbdeck.feature.deeplinks.handlers

import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.feature.deeplinks.models.DeepLinksState
import com.adbdeck.feature.deeplinks.models.LaunchHistoryEntry

internal class DeepLinksHistoryHandler {

    fun restoreFromHistory(state: DeepLinksState, entry: LaunchHistoryEntry): DeepLinksState =
        when (entry.mode) {
            LaunchMode.DEEP_LINK -> {
                val params = entry.deepLinkParams ?: return state
                state.withDeepLinkParams(params)
            }

            LaunchMode.INTENT -> {
                val params = entry.intentParams ?: return state
                state.withIntentParams(params)
            }
        }

    fun delete(history: List<LaunchHistoryEntry>, id: String): List<LaunchHistoryEntry> =
        history.filter { entry -> entry.id != id }

    fun clear(): List<LaunchHistoryEntry> = emptyList()
}
