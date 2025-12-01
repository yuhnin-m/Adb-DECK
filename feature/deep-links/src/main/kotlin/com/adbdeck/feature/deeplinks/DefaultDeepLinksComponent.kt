package com.adbdeck.feature.deeplinks

import com.adbdeck.core.adb.api.DeepLinkParams
import com.adbdeck.core.adb.api.ExtraType
import com.adbdeck.core.adb.api.IntentExtra
import com.adbdeck.core.adb.api.IntentLaunchClient
import com.adbdeck.core.adb.api.IntentParams
import com.adbdeck.core.adb.api.LaunchMode
import com.adbdeck.core.adb.api.LaunchResult
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.deeplinks.storage.DeepLinksStorage
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Реализация [DeepLinksComponent].
 *
 * Архитектурные паттерны:
 * - Подписка на [DeviceManager.selectedDeviceFlow] для получения активного устройства.
 * - [updateField] — атомарное обновление состояния с автоматическим пересчётом превью команды.
 * - История ограничена 50 записями; сохранение/загрузка в [DeepLinksStorage].
 *
 * @param componentContext  Контекст Decompose-компонента.
 * @param deviceManager     Менеджер ADB-устройств.
 * @param intentLaunchClient Клиент для запуска deep link / intent.
 * @param settingsRepository Репозиторий настроек (adbPath).
 */
class DefaultDeepLinksComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val intentLaunchClient: IntentLaunchClient,
    private val settingsRepository: SettingsRepository,
    /** URI для предзаполнения формы Deep Link при открытии из экрана Notifications. */
    initialDeepLinkUri: String? = null,
) : DeepLinksComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private val storage = DeepLinksStorage()

    private val _state = MutableStateFlow(DeepLinksState())
    override val state: StateFlow<DeepLinksState> = _state.asStateFlow()

    init {
        // Загрузить историю и шаблоны из файла
        scope.launch {
            val (history, templates) = storage.load()
            _state.update { it.copy(history = history, templates = templates) }
        }

        // Предзаполнить URI из внешнего источника (например, Notifications)
        if (!initialDeepLinkUri.isNullOrBlank()) {
            updateField { copy(mode = LaunchMode.DEEP_LINK, dlUri = initialDeepLinkUri) }
        }

        // Подписка на изменение активного устройства
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                _state.update { st ->
                    val updated = st.copy(activeDeviceId = device?.deviceId)
                    updated.copy(commandPreview = computePreview(updated))
                }
            }
        }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /**
     * Атомарное обновление состояния с пересчётом предпросмотра команды.
     * Все setters формы используют этот метод.
     */
    private fun updateField(update: DeepLinksState.() -> DeepLinksState) {
        _state.update { st ->
            val updated = st.update()
            updated.copy(commandPreview = computePreview(updated))
        }
    }

    /**
     * Вычислить строку предпросмотра команды для текущего состояния формы.
     * Если устройство не выбрано — показывает `<device>` как плейсхолдер.
     */
    private fun computePreview(st: DeepLinksState): String {
        val deviceId = st.activeDeviceId ?: "<device>"
        return when (st.mode) {
            LaunchMode.DEEP_LINK -> intentLaunchClient.buildDeepLinkCommand(
                deviceId = deviceId,
                adbPath  = "adb",
                params   = st.toDeepLinkParams(),
            )
            LaunchMode.INTENT -> intentLaunchClient.buildIntentCommand(
                deviceId = deviceId,
                adbPath  = "adb",
                params   = st.toIntentParams(),
            )
        }
    }

    private fun DeepLinksState.toDeepLinkParams() = DeepLinkParams(
        uri         = dlUri,
        action      = dlAction,
        packageName = dlPackage,
        component   = dlComponent,
        category    = dlCategory,
    )

    private fun DeepLinksState.toIntentParams() = IntentParams(
        action      = itAction,
        dataUri     = itDataUri,
        packageName = itPackage,
        component   = itComponent,
        categories  = itCategories,
        flags       = itFlags,
        extras      = itExtras,
    )

    private fun persistAsync() {
        scope.launch {
            val st = _state.value
            storage.save(st.history, st.templates)
        }
    }

    // ── Режим ────────────────────────────────────────────────────────────────

    override fun onModeChanged(mode: LaunchMode) = updateField { copy(mode = mode) }

    // ── Форма Deep Link ───────────────────────────────────────────────────────

    override fun onDlUriChanged(value: String)       = updateField { copy(dlUri = value) }
    override fun onDlActionChanged(value: String)    = updateField { copy(dlAction = value) }
    override fun onDlPackageChanged(value: String)   = updateField { copy(dlPackage = value) }
    override fun onDlComponentChanged(value: String) = updateField { copy(dlComponent = value) }
    override fun onDlCategoryChanged(value: String)  = updateField { copy(dlCategory = value) }

    // ── Форма Intent ──────────────────────────────────────────────────────────

    override fun onItActionChanged(value: String)    = updateField { copy(itAction = value) }
    override fun onItDataUriChanged(value: String)   = updateField { copy(itDataUri = value) }
    override fun onItPackageChanged(value: String)   = updateField { copy(itPackage = value) }
    override fun onItComponentChanged(value: String) = updateField { copy(itComponent = value) }
    override fun onItFlagsChanged(value: String)     = updateField { copy(itFlags = value) }

    override fun onItCategoryAdd(category: String) {
        if (category.isBlank()) return
        updateField { copy(itCategories = itCategories + category) }
    }

    override fun onItCategoryRemove(index: Int) = updateField {
        copy(itCategories = itCategories.toMutableList().also { it.removeAt(index) })
    }

    override fun onItExtraAdd() = updateField {
        copy(itExtras = itExtras + IntentExtra())
    }

    override fun onItExtraRemove(index: Int) = updateField {
        copy(itExtras = itExtras.toMutableList().also { it.removeAt(index) })
    }

    override fun onItExtraKeyChanged(index: Int, key: String) = updateField {
        copy(itExtras = itExtras.mapIndexed { i, e -> if (i == index) e.copy(key = key) else e })
    }

    override fun onItExtraTypeChanged(index: Int, type: ExtraType) = updateField {
        copy(itExtras = itExtras.mapIndexed { i, e -> if (i == index) e.copy(type = type) else e })
    }

    override fun onItExtraValueChanged(index: Int, value: String) = updateField {
        copy(itExtras = itExtras.mapIndexed { i, e -> if (i == index) e.copy(value = value) else e })
    }

    // ── Запуск ───────────────────────────────────────────────────────────────

    override fun onLaunch() {
        val st = _state.value
        val deviceId = st.activeDeviceId ?: return
        if (st.isLaunching) return

        scope.launch {
            val adbPath = runCatching {
                settingsRepository.settingsFlow.first().adbPath.ifBlank { "adb" }
            }.getOrDefault("adb")

            _state.update { it.copy(isLaunching = true, lastResult = null) }

            val currentSt = _state.value
            val result: Result<LaunchResult> = when (currentSt.mode) {
                LaunchMode.DEEP_LINK -> intentLaunchClient.launchDeepLink(
                    deviceId = deviceId,
                    adbPath  = adbPath,
                    params   = currentSt.toDeepLinkParams(),
                )
                LaunchMode.INTENT -> intentLaunchClient.launchIntent(
                    deviceId = deviceId,
                    adbPath  = adbPath,
                    params   = currentSt.toIntentParams(),
                )
            }

            result.fold(
                onSuccess = { launchResult ->
                    val historyEntry = LaunchHistoryEntry(
                        id             = UUID.randomUUID().toString(),
                        mode           = currentSt.mode,
                        deepLinkParams = if (currentSt.mode == LaunchMode.DEEP_LINK) currentSt.toDeepLinkParams() else null,
                        intentParams   = if (currentSt.mode == LaunchMode.INTENT) currentSt.toIntentParams() else null,
                        launchedAt     = System.currentTimeMillis(),
                        commandPreview = launchResult.commandPreview,
                        isSuccess      = launchResult.isSuccess,
                    )
                    val newHistory = (listOf(historyEntry) + _state.value.history).take(MAX_HISTORY)
                    _state.update { it.copy(
                        isLaunching = false,
                        lastResult  = launchResult,
                        history     = newHistory,
                        rightTab    = DeepLinksTab.COMMAND_RESULT,
                    ) }
                    persistAsync()
                },
                onFailure = { error ->
                    val failResult = LaunchResult(
                        exitCode       = -1,
                        stdout         = "",
                        stderr         = error.message ?: "Неизвестная ошибка",
                        commandPreview = currentSt.commandPreview,
                    )
                    _state.update { it.copy(
                        isLaunching = false,
                        lastResult  = failResult,
                        rightTab    = DeepLinksTab.COMMAND_RESULT,
                    ) }
                },
            )
        }
    }

    // ── Правая панель ────────────────────────────────────────────────────────

    override fun onRightTabChanged(tab: DeepLinksTab) {
        _state.update { it.copy(rightTab = tab) }
    }

    // ── История ───────────────────────────────────────────────────────────────

    override fun onRestoreFromHistory(entry: LaunchHistoryEntry) {
        _state.update { st ->
            val updated = when (entry.mode) {
                LaunchMode.DEEP_LINK -> {
                    val p = entry.deepLinkParams ?: return@update st
                    st.copy(
                        mode        = LaunchMode.DEEP_LINK,
                        dlUri       = p.uri,
                        dlAction    = p.action,
                        dlPackage   = p.packageName,
                        dlComponent = p.component,
                        dlCategory  = p.category,
                    )
                }
                LaunchMode.INTENT -> {
                    val p = entry.intentParams ?: return@update st
                    st.copy(
                        mode         = LaunchMode.INTENT,
                        itAction     = p.action,
                        itDataUri    = p.dataUri,
                        itPackage    = p.packageName,
                        itComponent  = p.component,
                        itCategories = p.categories,
                        itFlags      = p.flags,
                        itExtras     = p.extras,
                    )
                }
            }
            updated.copy(commandPreview = computePreview(updated))
        }
    }

    override fun onDeleteHistoryEntry(id: String) {
        _state.update { it.copy(history = it.history.filter { e -> e.id != id }) }
        persistAsync()
    }

    override fun onClearHistory() {
        _state.update { it.copy(history = emptyList()) }
        persistAsync()
    }

    // ── Шаблоны ───────────────────────────────────────────────────────────────

    override fun onShowSaveTemplateDialog() {
        _state.update { it.copy(isSaveTemplateDialogOpen = true, saveTemplateName = "") }
    }

    override fun onSaveTemplateNameChanged(name: String) {
        _state.update { it.copy(saveTemplateName = name) }
    }

    override fun onConfirmSaveTemplate() {
        val st = _state.value
        val name = st.saveTemplateName.trim()
        if (name.isBlank()) return

        val template = IntentTemplate(
            id             = UUID.randomUUID().toString(),
            name           = name,
            mode           = st.mode,
            deepLinkParams = if (st.mode == LaunchMode.DEEP_LINK) st.toDeepLinkParams() else null,
            intentParams   = if (st.mode == LaunchMode.INTENT) st.toIntentParams() else null,
        )
        _state.update { it.copy(
            templates = it.templates + template,
            isSaveTemplateDialogOpen = false,
            saveTemplateName = "",
        ) }
        persistAsync()
    }

    override fun onDismissSaveTemplateDialog() {
        _state.update { it.copy(isSaveTemplateDialogOpen = false, saveTemplateName = "") }
    }

    override fun onLaunchTemplate(template: IntentTemplate) {
        onRestoreFromTemplate(template)
        onLaunch()
    }

    override fun onRestoreFromTemplate(template: IntentTemplate) {
        _state.update { st ->
            val updated = when (template.mode) {
                LaunchMode.DEEP_LINK -> {
                    val p = template.deepLinkParams ?: return@update st
                    st.copy(
                        mode        = LaunchMode.DEEP_LINK,
                        dlUri       = p.uri,
                        dlAction    = p.action,
                        dlPackage   = p.packageName,
                        dlComponent = p.component,
                        dlCategory  = p.category,
                    )
                }
                LaunchMode.INTENT -> {
                    val p = template.intentParams ?: return@update st
                    st.copy(
                        mode         = LaunchMode.INTENT,
                        itAction     = p.action,
                        itDataUri    = p.dataUri,
                        itPackage    = p.packageName,
                        itComponent  = p.component,
                        itCategories = p.categories,
                        itFlags      = p.flags,
                        itExtras     = p.extras,
                    )
                }
            }
            updated.copy(commandPreview = computePreview(updated))
        }
    }

    override fun onDeleteTemplate(id: String) {
        _state.update { it.copy(templates = it.templates.filter { t -> t.id != id }) }
        persistAsync()
    }

    override fun prefillDeepLinkUri(uri: String) {
        updateField { copy(mode = LaunchMode.DEEP_LINK, dlUri = uri) }
    }

    private companion object {
        const val MAX_HISTORY = 50
    }
}
