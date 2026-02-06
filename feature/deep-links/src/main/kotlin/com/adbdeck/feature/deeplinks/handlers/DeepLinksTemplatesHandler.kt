package com.adbdeck.feature.deeplinks.handlers

import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.feature.deeplinks.models.DeepLinksState
import com.adbdeck.feature.deeplinks.models.IntentTemplate
import java.util.UUID

internal class DeepLinksTemplatesHandler {

    fun createTemplate(state: DeepLinksState, name: String): IntentTemplate =
        IntentTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            mode = state.mode,
            deepLinkParams = if (state.mode == LaunchMode.DEEP_LINK) state.toDeepLinkParams() else null,
            intentParams = if (state.mode == LaunchMode.INTENT) state.toIntentParams() else null,
        )

    fun append(templates: List<IntentTemplate>, template: IntentTemplate): List<IntentTemplate> =
        templates + template

    fun restoreFromTemplate(state: DeepLinksState, template: IntentTemplate): DeepLinksState =
        when (template.mode) {
            LaunchMode.DEEP_LINK -> {
                val params = template.deepLinkParams ?: return state
                state.withDeepLinkParams(params)
            }

            LaunchMode.INTENT -> {
                val params = template.intentParams ?: return state
                state.withIntentParams(params)
            }
        }

    fun delete(templates: List<IntentTemplate>, id: String): List<IntentTemplate> =
        templates.filter { template -> template.id != id }
}
