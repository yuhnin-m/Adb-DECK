package com.adbdeck.feature.packages

import adbdeck.feature.packages.generated.resources.Res
import adbdeck.feature.packages.generated.resources.packages_error_export_path_not_selected
import adbdeck.feature.packages.generated.resources.packages_error_operation_failed_default
import adbdeck.feature.packages.generated.resources.packages_feedback_app_info_opened
import adbdeck.feature.packages.generated.resources.packages_feedback_app_launched
import adbdeck.feature.packages.generated.resources.packages_feedback_app_stopped
import adbdeck.feature.packages.generated.resources.packages_feedback_data_cleared
import adbdeck.feature.packages.generated.resources.packages_feedback_device_unavailable
import adbdeck.feature.packages.generated.resources.packages_feedback_open_package_device_unavailable
import adbdeck.feature.packages.generated.resources.packages_feedback_operation_failed
import adbdeck.feature.packages.generated.resources.packages_feedback_package_copied
import adbdeck.feature.packages.generated.resources.packages_feedback_package_deleted
import adbdeck.feature.packages.generated.resources.packages_feedback_package_exported
import adbdeck.feature.packages.generated.resources.packages_feedback_package_not_found
import adbdeck.feature.packages.generated.resources.packages_feedback_permission_granted
import adbdeck.feature.packages.generated.resources.packages_feedback_permission_revoked
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.packages.PackageClient
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

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
 * @param openPackageInLogcat Внешний callback для перехода в Logcat с package-фильтром.
 */
class DefaultPackagesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val packageClient: PackageClient,
    private val settingsRepository: SettingsRepository,
    private val openPackageInLogcat: (String) -> Unit = {},
    initialPackageToReveal: String? = null,
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

    /** Пакет, который нужно раскрыть при внешней навигации (например из System Monitor). */
    private var pendingRevealPackageName: String? = initialPackageToReveal?.trim()?.ifBlank { null }

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
                        pendingRevealPackageName = null
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
                        val needsReload = _state.value.listState !is PackagesListState.Success
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
                        if (isDeviceChanged || needsReload) {
                            loadPackages()
                        }
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
            val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

            _state.update { it.copy(listState = PackagesListState.Loading) }

            packageClient.getPackages(deviceId = requestDeviceId, adbPath = adbPath)
                .onSuccess { packages ->
                    if (!isRequestStillValid(requestDeviceId)) return@onSuccess
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
                    resolvePendingReveal(packages)
                }
                .onFailure { error ->
                    if (!isRequestStillValid(requestDeviceId)) return@onFailure
                    _state.update {
                        it.copy(
                            listState = PackagesListState.Error(
                                error.message ?: "",
                            ),
                            filteredPackages = emptyList(),
                        )
                    }
                }
        }
    }

    override fun onRevealPackage(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return

        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            showFeedbackResource(
                messageRes = Res.string.packages_feedback_open_package_device_unavailable,
                isError = true,
            )
            return
        }

        pendingRevealPackageName = normalized
        val loaded = (_state.value.listState as? PackagesListState.Success)?.packages
        if (loaded != null) {
            resolvePendingReveal(loaded)
        } else {
            loadPackages()
        }
    }

    private fun resolvePendingReveal(packages: List<AppPackage>) {
        val target = pendingRevealPackageName ?: return
        val match = packages.firstOrNull { it.packageName == target }
        pendingRevealPackageName = null

        _state.update { current ->
            val nextState = current.copy(
                searchQuery = target,
                typeFilter = PackageTypeFilter.ALL,
                showDisabledOnly = false,
                showDebuggableOnly = false,
            )
            current.copy(
                searchQuery = target,
                typeFilter = PackageTypeFilter.ALL,
                showDisabledOnly = false,
                showDebuggableOnly = false,
                filteredPackages = applyFilters(packages, nextState),
            )
        }

        if (match != null) {
            onSelectPackage(match)
        } else {
            showFeedbackResource(
                messageRes = Res.string.packages_feedback_package_not_found,
                isError = true,
                target,
            )
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

    override fun onDisabledFilterChanged(enabled: Boolean) {
        _state.update { current ->
            val packages = (current.listState as? PackagesListState.Success)?.packages ?: return
            current.copy(
                showDisabledOnly = enabled,
                filteredPackages = applyFilters(packages, current.copy(showDisabledOnly = enabled)),
            )
        }
    }

    override fun onDebuggableFilterChanged(enabled: Boolean) {
        _state.update { current ->
            val packages = (current.listState as? PackagesListState.Success)?.packages ?: return
            current.copy(
                showDebuggableOnly = enabled,
                filteredPackages = applyFilters(packages, current.copy(showDebuggableOnly = enabled)),
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
     * 2. По состоянию disabled/debuggable
     * 3. По поисковому запросу (substring, case-insensitive в [AppPackage.packageName] и [AppPackage.apkPath])
     *
     * Сортировка: по [AppPackage.packageName] (BY_NAME) или по метке (BY_LABEL — в данном случае тоже packageName).
     */
    private fun applyFilters(allPackages: List<AppPackage>, state: PackagesState): List<AppPackage> {
        var result = allPackages

        // Фильтр по типу
        result = when (state.typeFilter) {
            PackageTypeFilter.ALL -> result
            PackageTypeFilter.USER -> result.filter { it.type == com.adbdeck.core.adb.api.packages.PackageType.USER }
            PackageTypeFilter.SYSTEM -> result.filter { it.type == com.adbdeck.core.adb.api.packages.PackageType.SYSTEM }
        }

        // Дополнительные фильтры
        if (state.showDisabledOnly) {
            result = result.filter { !it.isEnabled }
        }
        if (state.showDebuggableOnly) {
            result = result.filter { it.isDebuggable }
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
            if (device.state != DeviceState.DEVICE) return@launch
            val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
            val requestDeviceId = device.deviceId
            val requestPackageName = pkg.packageName

            packageClient.getPackageDetails(
                deviceId = requestDeviceId,
                packageName = requestPackageName,
                adbPath = adbPath,
            )
                .onSuccess { details ->
                    if (!isRequestStillValid(requestDeviceId)) return@onSuccess
                    if (_state.value.selectedPackage?.packageName != requestPackageName) return@onSuccess
                    _state.update { it.copy(detailState = PackageDetailState.Success(details)) }
                }
                .onFailure { error ->
                    if (!isRequestStillValid(requestDeviceId)) return@onFailure
                    if (_state.value.selectedPackage?.packageName != requestPackageName) return@onFailure
                    _state.update {
                        it.copy(
                            detailState = PackageDetailState.Error(
                                error.message ?: "",
                            )
                        )
                    }
                }
        }
    }

    override fun onClearSelection() {
        detailJob?.cancel()
        _state.update { it.copy(selectedPackage = null, detailState = PackageDetailState.Idle) }
    }

    // ── Быстрые действия ──────────────────────────────────────────────────────

    override fun onLaunchApp(pkg: AppPackage) = runAction(
        successMessage = { getString(Res.string.packages_feedback_app_launched) }
    ) {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.launchApp(device, pkg.packageName, adbPath)
    }

    override fun onForceStop(pkg: AppPackage) = runAction(
        successMessage = { getString(Res.string.packages_feedback_app_stopped) }
    ) {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.forceStop(device, pkg.packageName, adbPath)
    }

    override fun onOpenAppInfo(pkg: AppPackage) = runAction(
        successMessage = { getString(Res.string.packages_feedback_app_info_opened) }
    ) {
        val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
        packageClient.openAppInfo(device, pkg.packageName, adbPath)
    }

    override fun onTrackInLogcat(pkg: AppPackage) {
        val packageName = pkg.packageName.trim()
        if (packageName.isBlank()) return
        openPackageInLogcat(packageName)
    }

    override fun onExportApk(pkg: AppPackage, localPath: String) {
        val destination = localPath.trim()
        runAction(
            successMessage = { getString(Res.string.packages_feedback_package_exported) },
        ) {
            if (destination.isBlank()) {
                error(getString(Res.string.packages_error_export_path_not_selected))
            }

            val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
            val apkPaths = packageClient.getPackageApkPaths(
                deviceId = device,
                packageName = pkg.packageName,
                adbPath = adbPath,
            ).getOrElse { throw it }

            if (apkPaths.size > 1) {
                val targetPath = ensureExtension(destination, "apks")
                packageClient.exportApkSet(
                    deviceId = device,
                    packageName = pkg.packageName,
                    localArchivePath = targetPath,
                    adbPath = adbPath,
                )
            } else {
                val targetPath = ensureExtension(destination, "apk")
                packageClient.exportBaseApk(
                    deviceId = device,
                    packageName = pkg.packageName,
                    localPath = targetPath,
                    adbPath = adbPath,
                )
            }
        }
    }

    /** Копирование имени пакета в буфер обмена обрабатывается непосредственно в UI-слое. */
    override fun onCopyPackageName(pkg: AppPackage) {
        // Фактическое копирование выполняется в composable через java.awt.Toolkit.
        // Компонент только инициирует feedback.
        showFeedbackResource(
            messageRes = Res.string.packages_feedback_package_copied,
            isError = false,
            pkg.packageName,
        )
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
                runAction(successMessage = { getString(Res.string.packages_feedback_data_cleared) }) {
                    val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
                    packageClient.clearData(device, action.pkg.packageName, adbPath)
                }

            is PendingPackageAction.Uninstall ->
                runAction(successMessage = { getString(Res.string.packages_feedback_package_deleted) }) {
                    val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
                    val result = packageClient.uninstall(
                        device, action.pkg.packageName, action.keepData, adbPath,
                    )
                    // После удаления убрать пакет из списка без полной перезагрузки
                    if (result.isSuccess && isRequestStillValid(device)) {
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
        runAction(
            successMessage = {
                getString(Res.string.packages_feedback_permission_granted, permission)
            }
        ) {
            val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
            val result = packageClient.grantPermission(device, pkg.packageName, permission, adbPath)
            if (result.isSuccess && isRequestStillValid(device)) {
                reloadDetails(pkg)
            }
            result
        }
    }

    override fun onRevokePermission(pkg: AppPackage, permission: String) {
        runAction(
            successMessage = {
                getString(Res.string.packages_feedback_permission_revoked, permission)
            }
        ) {
            val (device, adbPath) = requireDeviceAndPath() ?: return@runAction null
            val result = packageClient.revokePermission(device, pkg.packageName, permission, adbPath)
            if (result.isSuccess && isRequestStillValid(device)) {
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
     * @param successMessage Suspend-поставщик локализованного сообщения об успехе.
     * @param block          Suspend-блок, выполняющий действие; возвращает `null` для отмены.
     */
    private fun runAction(
        successMessage: suspend () -> String,
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
                    .onSuccess {
                        showFeedback(ActionFeedback(successMessage(), isError = false))
                    }
                    .onFailure { e ->
                        val details = e.message
                            ?.takeIf { it.isNotBlank() }
                            ?: getString(Res.string.packages_error_operation_failed_default)
                        val message = getString(Res.string.packages_feedback_operation_failed, details)
                        showFeedback(ActionFeedback(message = message, isError = true))
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
            showFeedbackResource(
                messageRes = Res.string.packages_feedback_device_unavailable,
                isError = true,
            )
            return null
        }
        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
        activeDeviceId = device.deviceId
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

    /**
     * Локализованный helper для показа feedback по string-resource.
     */
    private fun showFeedbackResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        scope.launch {
            val message = getString(messageRes, *args)
            showFeedback(ActionFeedback(message = message, isError = isError))
        }
    }

    /** Добавляет расширение [extension], если путь его не содержит. */
    private fun ensureExtension(path: String, extension: String): String {
        val normalized = path.trim()
        val normalizedExtension = extension.trim().removePrefix(".")
        if (normalized.isBlank() || normalizedExtension.isBlank()) return normalized
        return if (normalized.endsWith(".$normalizedExtension", ignoreCase = true)) {
            normalized
        } else {
            "$normalized.$normalizedExtension"
        }
    }

    /** Перезагружает детали выбранного пакета (после grant/revoke разрешений). */
    private suspend fun reloadDetails(pkg: AppPackage) {
        val (device, adbPath) = requireDeviceAndPath() ?: return
        val requestDeviceId = device
        val requestPackageName = pkg.packageName
        packageClient.getPackageDetails(requestDeviceId, requestPackageName, adbPath)
            .onSuccess { details ->
                if (!isRequestStillValid(requestDeviceId)) return@onSuccess
                _state.update { current ->
                    if (current.selectedPackage?.packageName != requestPackageName) {
                        current
                    } else {
                        current.copy(detailState = PackageDetailState.Success(details))
                    }
                }
            }
    }

    private fun isRequestStillValid(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.state == DeviceState.DEVICE &&
            selected.deviceId == deviceId &&
            activeDeviceId == deviceId
    }
}
