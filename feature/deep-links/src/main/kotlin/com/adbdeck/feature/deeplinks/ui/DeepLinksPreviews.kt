package com.adbdeck.feature.deeplinks.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.adb.api.intents.LaunchResult
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.DeepLinksState
import com.adbdeck.feature.deeplinks.DeepLinksTab
import com.adbdeck.feature.deeplinks.IntentTemplate
import com.adbdeck.feature.deeplinks.LaunchHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

// ── Preview-заглушка компонента ───────────────────────────────────────────────

private class PreviewDeepLinksComponent(
    initialState: DeepLinksState = DeepLinksState(),
) : DeepLinksComponent {
    override val state: StateFlow<DeepLinksState> = MutableStateFlow(initialState)

    override fun onModeChanged(mode: LaunchMode) = Unit
    override fun onDlUriChanged(value: String) = Unit
    override fun onDlActionChanged(value: String) = Unit
    override fun onDlPackageChanged(value: String) = Unit
    override fun onDlComponentChanged(value: String) = Unit
    override fun onDlCategoryChanged(value: String) = Unit
    override fun onItActionChanged(value: String) = Unit
    override fun onItDataUriChanged(value: String) = Unit
    override fun onItPackageChanged(value: String) = Unit
    override fun onItComponentChanged(value: String) = Unit
    override fun onItCategoryAdd(category: String) = Unit
    override fun onItCategoryRemove(index: Int) = Unit
    override fun onItFlagsChanged(value: String) = Unit
    override fun onItExtraAdd() = Unit
    override fun onItExtraRemove(index: Int) = Unit
    override fun onItExtraKeyChanged(index: Int, key: String) = Unit
    override fun onItExtraTypeChanged(index: Int, type: ExtraType) = Unit
    override fun onItExtraValueChanged(index: Int, value: String) = Unit
    override fun onLaunch() = Unit
    override fun onRightTabChanged(tab: DeepLinksTab) = Unit
    override fun onRestoreFromHistory(entry: LaunchHistoryEntry) = Unit
    override fun onDeleteHistoryEntry(id: String) = Unit
    override fun onClearHistory() = Unit
    override fun onShowSaveTemplateDialog() = Unit
    override fun onSaveTemplateNameChanged(name: String) = Unit
    override fun onConfirmSaveTemplate() = Unit
    override fun onDismissSaveTemplateDialog() = Unit
    override fun onLaunchTemplate(template: IntentTemplate) = Unit
    override fun onRestoreFromTemplate(template: IntentTemplate) = Unit
    override fun onDeleteTemplate(id: String) = Unit
    override fun prefillDeepLinkUri(uri: String) = Unit
}

// ── Previews ──────────────────────────────────────────────────────────────────

/**
 * Предпросмотр: экран Deep Link с заполненной формой и активным устройством.
 */
@Preview
@Composable
private fun DeepLinksScreenDeepLinkPreview() {
    DeepLinksScreen(
        component = PreviewDeepLinksComponent(
            DeepLinksState(
                mode             = LaunchMode.DEEP_LINK,
                dlUri            = "https://example.com/product?id=123",
                dlAction         = "android.intent.action.VIEW",
                commandPreview   = "adb -s emulator-5554 shell am start -a android.intent.action.VIEW -d \"https://example.com/product?id=123\"",
                activeDeviceId   = "emulator-5554",
            )
        )
    )
}

/**
 * Предпросмотр: режим Intent с extras, результатом и историей.
 */
@Preview
@Composable
private fun DeepLinksScreenIntentWithResultPreview() {
    DeepLinksScreen(
        component = PreviewDeepLinksComponent(
            DeepLinksState(
                mode           = LaunchMode.INTENT,
                itAction       = "android.intent.action.SEND",
                itPackage      = "com.example.myapp",
                itComponent    = "com.example.myapp/.MainActivity",
                commandPreview = "adb -s emulator-5554 shell am start -a android.intent.action.SEND -p com.example.myapp",
                activeDeviceId = "emulator-5554",
                lastResult     = LaunchResult(
                    exitCode       = 0,
                    stdout         = "Starting: Intent { act=android.intent.action.SEND pkg=com.example.myapp }",
                    stderr         = "",
                    commandPreview = "adb -s emulator-5554 shell am start -a android.intent.action.SEND -p com.example.myapp",
                ),
                history = listOf(
                    LaunchHistoryEntry(
                        id             = "1",
                        mode           = LaunchMode.DEEP_LINK,
                        launchedAt     = System.currentTimeMillis() - 60_000,
                        commandPreview = "adb -s emulator-5554 shell am start -a android.intent.action.VIEW -d \"https://example.com\"",
                        isSuccess      = true,
                    ),
                ),
            )
        )
    )
}

/**
 * Предпросмотр: вкладка История.
 */
@Preview
@Composable
private fun DeepLinksHistoryTabPreview() {
    DeepLinksScreen(
        component = PreviewDeepLinksComponent(
            DeepLinksState(
                rightTab       = DeepLinksTab.HISTORY,
                activeDeviceId = "emulator-5554",
                history        = listOf(
                    LaunchHistoryEntry(
                        id             = "1",
                        mode           = LaunchMode.DEEP_LINK,
                        launchedAt     = System.currentTimeMillis() - 120_000,
                        commandPreview = "adb -s emulator-5554 shell am start -a android.intent.action.VIEW -d \"https://example.com/path\"",
                        isSuccess      = true,
                    ),
                    LaunchHistoryEntry(
                        id             = "2",
                        mode           = LaunchMode.INTENT,
                        launchedAt     = System.currentTimeMillis() - 30_000,
                        commandPreview = "adb -s emulator-5554 shell am start -a android.intent.action.SEND -n com.example/.Main",
                        isSuccess      = false,
                    ),
                ),
            )
        )
    )
}

/**
 * Предпросмотр: вкладка Шаблоны.
 */
@Preview
@Composable
private fun DeepLinksTemplatesTabPreview() {
    DeepLinksScreen(
        component = PreviewDeepLinksComponent(
            DeepLinksState(
                rightTab       = DeepLinksTab.TEMPLATES,
                activeDeviceId = "emulator-5554",
                templates      = listOf(
                    IntentTemplate(
                        id   = "t1",
                        name = "Открыть главную страницу",
                        mode = LaunchMode.DEEP_LINK,
                    ),
                    IntentTemplate(
                        id   = "t2",
                        name = "Запуск Main Activity",
                        mode = LaunchMode.INTENT,
                    ),
                ),
            )
        )
    )
}
