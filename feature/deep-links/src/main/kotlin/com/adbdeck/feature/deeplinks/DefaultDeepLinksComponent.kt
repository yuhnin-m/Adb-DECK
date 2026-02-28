package com.adbdeck.feature.deeplinks

import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.IntentExtra
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.settings.resolvedAdbPath
import com.adbdeck.feature.deeplinks.handlers.DeepLinksHistoryHandler
import com.adbdeck.feature.deeplinks.handlers.DeepLinksLaunchHandler
import com.adbdeck.feature.deeplinks.handlers.DeepLinksTemplatesHandler
import com.adbdeck.feature.deeplinks.models.DeepLinksFeedback
import com.adbdeck.feature.deeplinks.models.DeepLinksState
import com.adbdeck.feature.deeplinks.models.DeepLinksTab
import com.adbdeck.feature.deeplinks.models.IntentTemplate
import com.adbdeck.feature.deeplinks.models.LaunchHistoryEntry
import com.adbdeck.feature.deeplinks.models.computeIntentFlagsMask
import com.adbdeck.feature.deeplinks.models.deriveSelectedIntentFlags
import com.adbdeck.feature.deeplinks.models.formatIntentFlagsMask
import com.adbdeck.feature.deeplinks.models.parseIntentFlagsMask
import com.adbdeck.feature.deeplinks.storage.DeepLinksStorage
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * @param packageClient     Клиент списка пакетов для autocomplete.
 * @param intentLaunchClient Клиент для запуска deep link / intent.
 * @param settingsRepository Репозиторий настроек (adbPath).
 */
class DefaultDeepLinksComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val packageClient: PackageClient,
    private val intentLaunchClient: IntentLaunchClient,
    private val settingsRepository: SettingsRepository,
    /** URI для предзаполнения формы Deep Link при открытии из экрана Notifications. */
    initialDeepLinkUri: String? = null,
) : DeepLinksComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private val storage = DeepLinksStorage()
    private val launchHandler = DeepLinksLaunchHandler(intentLaunchClient, MAX_HISTORY)
    private val historyHandler = DeepLinksHistoryHandler()
    private val templatesHandler = DeepLinksTemplatesHandler()
    private val persistMutex = Mutex()
    private var launchJob: Job? = null
    private var feedbackJob: Job? = null
    private var packageSuggestionsJob: Job? = null
    @Volatile
    private var currentAdbPath: String =
        settingsRepository.resolvedAdbPath()

    private val _state = MutableStateFlow(DeepLinksState())
    override val state: StateFlow<DeepLinksState> = _state.asStateFlow()

    init {
        // Загрузить историю и шаблоны из файла
        scope.launch {
            storage.load()
                .onSuccess { (history, templates) ->
                    _state.update { it.copy(history = history, templates = templates) }
                }
                .onFailure { error ->
                    showFeedback(
                        message = "Не удалось загрузить историю/шаблоны: ${error.message ?: "неизвестная ошибка"}",
                        isError = true,
                    )
                }
        }

        // Предзаполнить URI из внешнего источника (например, Notifications)
        if (!initialDeepLinkUri.isNullOrBlank()) {
            updateField { copy(mode = LaunchMode.DEEP_LINK, dlUri = initialDeepLinkUri) }
        }

        // Подписка на изменение adbPath, чтобы preview совпадал с фактическим запуском.
        scope.launch {
            settingsRepository.settingsFlow
                .map { it.resolvedAdbPath() }
                .distinctUntilChanged()
                .collect { adbPath ->
                    currentAdbPath = adbPath
                    _state.update { st ->
                        st.copy(commandPreview = computePreview(st))
                    }
                    loadPackageSuggestions(_state.value.activeDeviceId)
                }
        }

        // Подписка на изменение активного устройства
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                var shouldReloadPackageSuggestions = false
                _state.update { st ->
                    val activeDeviceId = device
                        ?.takeIf { it.state == DeviceState.DEVICE }
                        ?.deviceId
                    shouldReloadPackageSuggestions = st.activeDeviceId != activeDeviceId
                    val updated = st.copy(activeDeviceId = activeDeviceId)
                    updated.copy(commandPreview = computePreview(updated))
                }
                if (shouldReloadPackageSuggestions) {
                    loadPackageSuggestions(
                        deviceId = device
                            ?.takeIf { it.state == DeviceState.DEVICE }
                            ?.deviceId,
                    )
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
        return launchHandler.computePreview(
            state = st,
            deviceId = deviceId,
            adbPath = currentAdbPath,
        )
    }

    private fun syncIntentFlagsState(state: DeepLinksState): DeepLinksState {
        val parsedMask = parseIntentFlagsMask(state.itFlags)
        return parsedMask.fold(
            onSuccess = { mask ->
                state.copy(
                    itSelectedFlags = deriveSelectedIntentFlags(mask),
                    itFlagsValidationMessage = null,
                )
            },
            onFailure = { error ->
                state.copy(
                    itFlagsValidationMessage = error.message ?: "Некорректный формат flags",
                )
            },
        )
    }

    private fun loadPackageSuggestions(deviceId: String?) {
        packageSuggestionsJob?.cancel()

        if (deviceId == null) {
            _state.update {
                it.copy(
                    packageSuggestions = emptyList(),
                    isPackageSuggestionsLoading = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                packageSuggestions = emptyList(),
                isPackageSuggestionsLoading = true,
            )
        }

        packageSuggestionsJob = scope.launch {
            packageClient.getPackages(deviceId = deviceId, adbPath = currentAdbPath)
                .onSuccess { packages ->
                    val packageNames = packages.asSequence()
                        .map { appPackage -> appPackage.packageName }
                        .filter { packageName -> packageName.isNotBlank() }
                        .distinct()
                        .toList()

                    _state.update { current ->
                        val selectedDeviceId = deviceManager.selectedDeviceFlow.value
                            ?.takeIf { it.state == DeviceState.DEVICE }
                            ?.deviceId
                        if (selectedDeviceId != deviceId) {
                            current
                        } else {
                            current.copy(
                                packageSuggestions = packageNames,
                                isPackageSuggestionsLoading = false,
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { current ->
                        val selectedDeviceId = deviceManager.selectedDeviceFlow.value
                            ?.takeIf { it.state == DeviceState.DEVICE }
                            ?.deviceId
                        if (selectedDeviceId != deviceId) {
                            current
                        } else {
                            current.copy(
                                packageSuggestions = emptyList(),
                                isPackageSuggestionsLoading = false,
                            )
                        }
                    }
                }
        }
    }

    private fun persistAsync() {
        scope.launch {
            persistMutex.withLock {
                val st = _state.value
                storage.save(st.history, st.templates)
                    .onFailure { error ->
                        showFeedback(
                            message = "Не удалось сохранить изменения: ${error.message ?: "неизвестная ошибка"}",
                            isError = true,
                        )
                    }
            }
        }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = DeepLinksFeedback(message = message, isError = isError)) }
        feedbackJob = scope.launch {
            delay(FEEDBACK_AUTO_DISMISS_MS)
            _state.update { it.copy(feedback = null) }
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
    override fun onItFlagsChanged(value: String)     = updateField { syncIntentFlagsState(copy(itFlags = value)) }

    override fun onShowIntentFlagsDialog() {
        _state.update { state ->
            syncIntentFlagsState(state).copy(isIntentFlagsDialogOpen = true)
        }
    }

    override fun onDismissIntentFlagsDialog() {
        _state.update { it.copy(isIntentFlagsDialogOpen = false) }
    }

    override fun onApplyIntentFlagsSelection(selectedFlagKeys: Set<String>) {
        val normalizedFlags = selectedFlagKeys.toSet()
        val mask = computeIntentFlagsMask(normalizedFlags)
        updateField {
            copy(
                itFlags = formatIntentFlagsMask(mask),
                itSelectedFlags = normalizedFlags,
                itFlagsValidationMessage = null,
                isIntentFlagsDialogOpen = false,
            )
        }
    }

    override fun onItCategoryAdd(category: String) {
        if (category.isBlank()) return
        updateField { copy(itCategories = itCategories + category) }
    }

    override fun onItCategoryRemove(index: Int) = updateField {
        if (index !in itCategories.indices) return@updateField this
        copy(itCategories = itCategories.toMutableList().also { it.removeAt(index) })
    }

    override fun onItExtraAdd() = updateField {
        copy(itExtras = itExtras + IntentExtra())
    }

    override fun onItExtraRemove(index: Int) = updateField {
        if (index !in itExtras.indices) return@updateField this
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
        val selectedDevice = deviceManager.selectedDeviceFlow.value
            ?.takeIf { it.state == DeviceState.DEVICE }
            ?: return

        val launchState = _state.value
        val deviceId = selectedDevice.deviceId
        val adbPath = currentAdbPath

        var canStart = false
        _state.update { current ->
            if (current.isLaunching) {
                current
            } else {
                canStart = true
                current.copy(isLaunching = true, lastResult = null)
            }
        }
        if (!canStart) return

        launchJob = scope.launch {
            try {
                val result = launchHandler.launch(
                    state = launchState,
                    deviceId = deviceId,
                    adbPath = adbPath,
                )

                result.fold(
                    onSuccess = { launchResult ->
                        val historyEntry = launchHandler.createHistoryEntry(
                            state = launchState,
                            launchResult = launchResult,
                        )
                        _state.update { current ->
                            current.copy(
                                lastResult = launchResult,
                                history = launchHandler.appendHistory(
                                    currentHistory = current.history,
                                    newEntry = historyEntry,
                                ),
                                rightTab = DeepLinksTab.COMMAND_RESULT,
                            )
                        }
                        persistAsync()
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                lastResult = launchHandler.createFailureResult(
                                    state = launchState,
                                    error = error,
                                ),
                                rightTab = DeepLinksTab.COMMAND_RESULT,
                            )
                        }
                        showFeedback(
                            message = "Не удалось выполнить запуск: ${error.message ?: "неизвестная ошибка"}",
                            isError = true,
                        )
                    },
                )
            } finally {
                _state.update { it.copy(isLaunching = false) }
                launchJob = null
            }
        }
    }

    // ── Правая панель ────────────────────────────────────────────────────────

    override fun onRightTabChanged(tab: DeepLinksTab) {
        _state.update { it.copy(rightTab = tab) }
    }

    // ── История ───────────────────────────────────────────────────────────────

    override fun onRestoreFromHistory(entry: LaunchHistoryEntry) {
        _state.update { st ->
            val updated = syncIntentFlagsState(historyHandler.restoreFromHistory(st, entry))
            updated.copy(commandPreview = computePreview(updated))
        }
    }

    override fun onDeleteHistoryEntry(id: String) {
        _state.update { it.copy(history = historyHandler.delete(it.history, id)) }
        persistAsync()
    }

    override fun onClearHistory() {
        _state.update { it.copy(history = historyHandler.clear()) }
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

        val template = templatesHandler.createTemplate(st, name)
        _state.update { it.copy(
            templates = templatesHandler.append(it.templates, template),
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
            val updated = syncIntentFlagsState(templatesHandler.restoreFromTemplate(st, template))
            updated.copy(commandPreview = computePreview(updated))
        }
    }

    override fun onDeleteTemplate(id: String) {
        _state.update { it.copy(templates = templatesHandler.delete(it.templates, id)) }
        persistAsync()
    }

    override fun prefillDeepLinkUri(uri: String) {
        updateField { copy(mode = LaunchMode.DEEP_LINK, dlUri = uri) }
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = null) }
    }

    private companion object {
        const val MAX_HISTORY = 50
        const val FEEDBACK_AUTO_DISMISS_MS = 3_000L
    }
}
