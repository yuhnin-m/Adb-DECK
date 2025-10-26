package com.adbdeck.feature.packages

import com.adbdeck.core.adb.api.AppPackage
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.adb.api.PackageClient
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [PackagesComponent].
 *
 * Архитектура взаимодействия с устройством:
 * 1. `init` — подписывается на [DeviceManager.selectedDeviceFlow]
 * 2. При смене устройства запускает загрузку списка пакетов
 * 3. [onSelectPackage] запускает отдельную корутину для `dumpsys package <pkg>`
 * 4. Деструктивные действия требуют двухшагового подтверждения ([onRequestXxx] → [onConfirmAction])
 * 5. Обратная связь ([ActionFeedback]) автоматически очищается через 3 секунды
 *
 * @param componentContext   Контекст Decompose (lifecycle, корутин-скоуп).
 * @param deviceManager      Менеджер устройств — источник активного устройства.
 * @param packageClient      ADB-клиент для работы с пакетами.
 * @param settingsRepository Репозиторий настроек (для получения пути к adb).
 */
class DefaultPackagesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val packageClient: PackageClient,
    private val settingsRepository: SettingsRepository,
) : PackagesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(PackagesState())
    override val state: StateFlow<PackagesState> = _state.asStateFlow()

    /** Job загрузки списка пакетов. Отменяется при повторном вызове [loadPackages]. */
    private var loadJob: Job? = null

    /** Job загрузки деталей пакета. Отменяется при смене выбранного пакета. */
    private var detailJob: Job? = null

    /** Job автоочистки [ActionFeedback]. Перезапускается при каждом новом feedback. */
    private var feedbackJob: Job? = null

    /** Последний известный selected device, с которым синхронизировано состояние экрана. */
    private var activeDeviceId: String? = null

    init {
        // Подписываемся на изменение активного устройства
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                when {
                    // Устройство недоступно или отключено
                    device == null || device.state != DeviceState.DEVICE -> {
                        loadJob?.cancel()
                        detailJob?.cancel()
                        activeDeviceId = null
                        _state.update {
                            it.copy(
                                listState = PackagesListState.NoDevice,
                                filteredPackages = emptyList(),
                                selectedPackage = null,
                                detailState = PackageDetailState.Idle,
                                pendingAction = null,
                            )
                        }
                    }
                    // Устройство изменилось или это первая загрузка
                    else -> {
                        val isDeviceChanged = activeDeviceId != device.deviceId
                        // Сбросить выделение при смене устройства
                        if (isDeviceChanged) {
                            detailJob?.cancel()
                            _state.update {
                                it.copy(
                                    selectedPackage = null,
                                    detailState = PackageDetailState.Idle,
                                    pendingAction = null,
                                )
                            }
                        }
                        activeDeviceId = device.deviceId
                        loadPackages()
                    }
                }
            }
        }
    }

    // ── Загрузка списка ────────────────────────────────────────────────────────

    override fun onRefresh() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) return
        loadPackages()
    }

    /** Загружает список пакетов с активного устройства. */
    private fun loadPackages() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val device = deviceManager.selectedDeviceFlow.value ?: return@launch
            if (device.state != DeviceState.DEVICE) return@launch

            val requestDeviceId = device.deviceId
            val adbPath = settingsRepository.getSettings().adbPath

            _state.update { it.copy(listState = PackagesListState.Loading) }

            packageClient.getPackages(deviceId = requestDeviceId, adbPath = adbPath)
                .onSuccess { packages ->
                    if (deviceManager.selectedDeviceFlow.value?.deviceId != requestDeviceId) return@onSuccess
                    activeDeviceId = requestDeviceId
                    _state.update { current ->
                        val selectedStillExists = current.selectedPackage
                            ?.let { selected -> packages.any { it.packageName == selected.packageName } }
                            ?: false
                        val pendingActionStillExists = when (val pending = current.pendingAction) {
                            is PendingPackageAction.ClearData ->
                                packages.any { it.packageName == pending.pkg.packageName }
                            is PendingPackageAction.Uninstall ->
                                packages.any { it.packageName == pending.pkg.packageName }
                            null -> true
                        }

                        current.copy(
                            listState = PackagesListState.Success(packages),
                            filteredPackages = applyFilters(packages, current),
                            selectedPackage = if (selectedStillExists) current.selectedPackage else null,
                            detailState = if (selectedStillExists) {
                                current.detailState
                            } else {
                                PackageDetailState.Idle
                            },
                            pendingAction = if (pendingActionStillExists) current.pendingAction else null,
                        )
                    }
                }
                .onFailure { error ->
                    if (deviceManager.selectedDeviceFlow.value?.deviceId != requestDeviceId) return@onFailure
                    _state.update {
                        it.copy(
                            listState = PackagesListState.Error(
                                error.message ?: "Неизвестная ошибка",
                            ),
                            filteredPackages = emptyList(),
                        )
                    }
                }
        }
    }

    // ── Фильтры и сортировка ───────────────────────────────────────────────────

    override fun onSearchChanged(query: String) {
        _state.update { current ->
            val packages = (current.listState as? PackagesListState.Success)?.packages ?: return
            current.copy(
                searchQuery = query,
                filteredPackages = applyFilters(packages, current.copy(searchQuery = query)),
            )
        }
    }

    override fun onTypeFilterChanged(filter: PackageTypeFilter) {
        _state.update { current ->
            val packages = (current.listState as? PackagesListState.Success)?.packages ?: return
            current.copy(
                typeFilter = filter,
                filteredPackages = applyFilters(packages, current.copy(typeFilter = filter)),
            )
        }
    }

    override fun onSortOrderChanged(order: PackageSortOrder) {
        _state.update { current ->
            val packages = (current.listState as? PackagesListState.Success)?.packages ?: return
            current.copy(
                sortOrder = order,
                filteredPackages = applyFilters(packages, current.copy(sortOrder = order)),
            )
        }
    }

    /**
     * Применяет текущие фильтры и сортировку к [allPackages] и возвращает отфильтрованный список.
     *
     * Фильтрация:
     * 1. По типу ([PackageTypeFilter.USER] / [PackageTypeFilter.SYSTEM])
     * 2. По поисковому запросу (substring, case-insensitive в [AppPackage.packageName] и [AppPackage.apkPath])
     *
     * Сортировка: по [AppPackage.packageName] (BY_NAME) или по метке (BY_LABEL — в данном случае тоже packageName).
     */
    private fun applyFilters(allPackages: List<AppPackage>, state: PackagesState): List<AppPackage> {
        var result = allPackages

        // Фильтр по типу
        result = when (state.typeFilter) {
            PackageTypeFilter.ALL -> result
            PackageTypeFilter.USER -> result.filter { it.type == com.adbdeck.core.adb.api.PackageType.USER }
            PackageTypeFilter.SYSTEM -> result.filter { it.type == com.adbdeck.core.adb.api.PackageType.SYSTEM }
        }

        // Текстовый поиск
        val query = state.searchQuery.trim()
        if (query.isNotEmpty()) {
            result = result.filter { pkg ->
                pkg.packageName.contains(query, ignoreCase = true) ||
                    pkg.apkPath.contains(query, ignoreCase = true)
            }
        }

        // Сортировка
        result = when (state.sortOrder) {
            // Источник уже отсортирован по имени пакета; фильтрация порядок не нарушает.
            PackageSortOrder.BY_NAME -> result
            // appLabel пока недоступен в AppPackage, поэтому сохраняем текущий порядок.
            PackageSortOrder.BY_LABEL -> result
        }

        return result
    }

    // ── Выбор пакета ──────────────────────────────────────────────────────────

    override fun onSelectPackage(pkg: AppPackage) {
        if (_state.value.selectedPackage?.packageName == pkg.packageName &&
            _state.value.detailState is PackageDetailState.Success
        ) {
            // Тот же пакет — не перезагружать
            return
        }

        detailJob?.cancel()
        _state.update { it.copy(selectedPackage = pkg, detailState = PackageDetailState.Loading) }

        detailJob = scope.launch {
            val device = deviceManager.selectedDeviceFlow.value ?: return@launch
            val adbPath = settingsRepository.getSettings().adbPath
            val requestDeviceId = device.deviceId
            val requestPackageName = pkg.packageName

            packageClient.getPackageDetails(
                deviceId = requestDeviceId,
                packageName = requestPackageName,
                adbPath = adbPath,
            )
                .onSuccess { details ->
                    if (deviceManager.selectedDeviceFlow.value?.deviceId != requestDeviceId) return@onSuccess
                    if (_state.value.selectedPackage?.packageName != requestPackageName) return@onSuccess
                    _state.update { it.copy(detailState = PackageDetailState.Success(details)) }
                }
                .onFailure { error ->
                    if (deviceManager.selectedDeviceFlow.value?.deviceId != requestDeviceId) return@onFailure
                    if (_state.value.selectedPackage?.packageName != requestPackageName) return@onFailure
                    _state.update {
                        it.copy(detailState = PackageDetailState.Error(error.message ?: "Ошибка загрузки"))
                    }
                }
        }
    }

    override fun onClearSelection() {
        detailJob?.cancel()
        _state.update { it.copy(selectedPackage = null, detailState = PackageDetailState.Idle) }
    }

    // ── Быстрые действия ──────────────────────────────────────────────────────

    override fun onLaunchApp(pkg: AppPackage) = runAction("Приложение запущено") {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.launchApp(device, pkg.packageName, adbPath)
    }

    override fun onForceStop(pkg: AppPackage) = runAction("Приложение остановлено") {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.forceStop(device, pkg.packageName, adbPath)
    }

    override fun onOpenAppInfo(pkg: AppPackage) = runAction("Открыта информация о приложении") {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.openAppInfo(device, pkg.packageName, adbPath)
    }

    /** Копирование имени пакета в буфер обмена обрабатывается непосредственно в UI-слое. */
    override fun onCopyPackageName(pkg: AppPackage) {
        // Фактическое копирование выполняется в composable через java.awt.Toolkit.
        // Компонент только инициирует feedback.
        showFeedback(ActionFeedback(message = "Скопировано: ${pkg.packageName}", isError = false))
    }

    // ── Деструктивные действия ────────────────────────────────────────────────

    override fun onRequestClearData(pkg: AppPackage) {
        _state.update { it.copy(pendingAction = PendingPackageAction.ClearData(pkg)) }
    }

    override fun onRequestUninstall(pkg: AppPackage) {
        _state.update { it.copy(pendingAction = PendingPackageAction.Uninstall(pkg)) }
    }

    override fun onCancelAction() {
        _state.update { it.copy(pendingAction = null) }
    }

    override fun onConfirmAction() {
        val action = _state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null) }

        when (action) {
            is PendingPackageAction.ClearData ->
                runAction("Данные очищены") {
                    val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
                    packageClient.clearData(device, action.pkg.packageName, adbPath)
                }

            is PendingPackageAction.Uninstall ->
                runAction("Пакет удалён") {
                    val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
                    val result = packageClient.uninstall(
                        device, action.pkg.packageName, action.keepData, adbPath,
                    )
                    // После удаления убрать пакет из списка без полной перезагрузки
                    if (result.isSuccess) {
                        _state.update { current ->
                            val newPackages = (current.listState as? PackagesListState.Success)
                                ?.packages?.filter { it.packageName != action.pkg.packageName }
                                ?: return@update current
                            current.copy(
                                listState = PackagesListState.Success(newPackages),
                                filteredPackages = applyFilters(newPackages, current),
                                selectedPackage = if (current.selectedPackage?.packageName == action.pkg.packageName) null else current.selectedPackage,
                                detailState = if (current.selectedPackage?.packageName == action.pkg.packageName) PackageDetailState.Idle else current.detailState,
                            )
                        }
                    }
                    result
                }
        }
    }

    // ── Разрешения ────────────────────────────────────────────────────────────

    override fun onGrantPermission(pkg: AppPackage, permission: String) {
        runAction("Разрешение выдано: $permission") {
            val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
            val result = packageClient.grantPermission(device, pkg.packageName, permission, adbPath)
            if (result.isSuccess) {
                reloadDetails(pkg)
            }
            result
        }
    }

    override fun onRevokePermission(pkg: AppPackage, permission: String) {
        runAction("Разрешение отозвано: $permission") {
            val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
            val result = packageClient.revokePermission(device, pkg.packageName, permission, adbPath)
            if (result.isSuccess) {
                reloadDetails(pkg)
            }
            result
        }
    }

    // ── Обратная связь ────────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = null) }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /**
     * Запускает [block] как фоновую операцию с обработкой isActionRunning и feedback.
     *
     * [block] должен вернуть [Result]<Unit> или `null` (если операция невозможна).
     * При успехе показывает [successMessage], при ошибке — текст исключения.
     *
     * @param successMessage Сообщение об успехе для [ActionFeedback].
     * @param block          Suspend-блок, выполняющий действие; возвращает `null` для отмены.
     */
    private fun runAction(
        successMessage: String,
        block: suspend () -> Result<Unit>?,
    ) {
        var shouldStart = false
        _state.update { current ->
            if (current.isActionRunning) {
                current
            } else {
                shouldStart = true
                current.copy(isActionRunning = true)
            }
        }
        if (!shouldStart) return

        scope.launch {
            try {
                val result = block() ?: return@launch
                result
                    .onSuccess { showFeedback(ActionFeedback(successMessage, isError = false)) }
                    .onFailure { e ->
                        showFeedback(ActionFeedback(e.message ?: "Операция завершилась с ошибкой", isError = true))
                    }
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    /** Возвращает пару (deviceId, adbPath) или `null` если устройство недоступно. */
    private fun requireDeviceAndPath(): Pair<String, String>? {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            showFeedback(ActionFeedback("Устройство не выбрано или недоступно", isError = true))
            return null
        }
        val adbPath = settingsRepository.getSettings().adbPath
        return device.deviceId to adbPath
    }

    /** Показывает [feedback] и запускает таймер на 3 секунды для автоочистки. */
    private fun showFeedback(feedback: ActionFeedback) {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = feedback) }
        feedbackJob = scope.launch {
            delay(3_000)
            _state.update { it.copy(actionFeedback = null) }
        }
    }

    /** Перезагружает детали выбранного пакета (после grant/revoke разрешений). */
    private suspend fun reloadDetails(pkg: AppPackage) {
        val (device, adbPath) = requireDeviceAndPath() ?: return
        val requestDeviceId = device
        val requestPackageName = pkg.packageName
        packageClient.getPackageDetails(requestDeviceId, requestPackageName, adbPath)
            .onSuccess { details ->
                if (deviceManager.selectedDeviceFlow.value?.deviceId != requestDeviceId) return@onSuccess
                _state.update { current ->
                    if (current.selectedPackage?.packageName != requestPackageName) {
                        current
                    } else {
                        current.copy(detailState = PackageDetailState.Success(details))
                    }
                }
            }
    }
}
