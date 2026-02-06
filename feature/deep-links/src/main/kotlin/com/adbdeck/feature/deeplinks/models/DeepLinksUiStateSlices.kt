package com.adbdeck.feature.deeplinks.models

import androidx.compose.runtime.Immutable
import com.adbdeck.core.adb.api.intents.IntentExtra
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.adb.api.intents.LaunchResult

@Immutable
data class DeepLinksFormUiState(
    val mode: LaunchMode,
    val dlUri: String,
    val dlAction: String,
    val dlPackage: String,
    val dlComponent: String,
    val dlCategory: String,
    val itAction: String,
    val itDataUri: String,
    val itPackage: String,
    val itComponent: String,
    val itCategories: List<String>,
    val itFlags: String,
    val itSelectedFlags: Set<String>,
    val itFlagsValidationMessage: String?,
    val itExtras: List<IntentExtra>,
    val activeDeviceId: String?,
    val isLaunching: Boolean,
    val packageSuggestions: List<String>,
    val isPackageSuggestionsLoading: Boolean,
    val isIntentFlagsDialogOpen: Boolean,
)

@Immutable
data class DeepLinksCommandResultUiState(
    val commandPreview: String,
    val isLaunching: Boolean,
    val lastResult: LaunchResult?,
)

@Immutable
data class DeepLinksRightPanelUiState(
    val rightTab: DeepLinksTab,
    val commandResult: DeepLinksCommandResultUiState,
    val history: List<LaunchHistoryEntry>,
    val templates: List<IntentTemplate>,
)

@Immutable
data class DeepLinksStatusBarUiState(
    val activeDeviceId: String?,
    val historyCount: Int,
    val templatesCount: Int,
)

internal fun DeepLinksState.toFormUiState(): DeepLinksFormUiState = DeepLinksFormUiState(
    mode = mode,
    dlUri = dlUri,
    dlAction = dlAction,
    dlPackage = dlPackage,
    dlComponent = dlComponent,
    dlCategory = dlCategory,
    itAction = itAction,
    itDataUri = itDataUri,
    itPackage = itPackage,
    itComponent = itComponent,
    itCategories = itCategories,
    itFlags = itFlags,
    itSelectedFlags = itSelectedFlags,
    itFlagsValidationMessage = itFlagsValidationMessage,
    itExtras = itExtras,
    activeDeviceId = activeDeviceId,
    isLaunching = isLaunching,
    packageSuggestions = packageSuggestions,
    isPackageSuggestionsLoading = isPackageSuggestionsLoading,
    isIntentFlagsDialogOpen = isIntentFlagsDialogOpen,
)

internal fun DeepLinksState.toRightPanelUiState(): DeepLinksRightPanelUiState = DeepLinksRightPanelUiState(
    rightTab = rightTab,
    commandResult = DeepLinksCommandResultUiState(
        commandPreview = commandPreview,
        isLaunching = isLaunching,
        lastResult = lastResult,
    ),
    history = history,
    templates = templates,
)

internal fun DeepLinksState.toStatusBarUiState(): DeepLinksStatusBarUiState = DeepLinksStatusBarUiState(
    activeDeviceId = activeDeviceId,
    historyCount = history.size,
    templatesCount = templates.size,
)
