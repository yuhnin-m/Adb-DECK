package com.adbdeck.feature.fileexplorer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Показать краткосрочный баннер обратной связи. */
internal fun DefaultFileExplorerComponent.showFeedback(message: String, isError: Boolean) {
    feedbackJob?.cancel()
    _state.update { it.copy(feedback = ExplorerFeedback(message = message, isError = isError)) }

    feedbackJob = scope.launch {
        delay(3_000L)
        _state.update { current ->
            if (current.feedback?.message == message) {
                current.copy(feedback = null)
            } else {
                current
            }
        }
    }
}

internal fun DefaultFileExplorerComponent.showFeedbackResource(
    messageRes: StringResource,
    isError: Boolean,
    vararg args: Any,
) {
    scope.launch {
        val message = getString(messageRes, *args)
        showFeedback(message = message, isError = isError)
    }
}
