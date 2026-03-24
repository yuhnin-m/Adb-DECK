package com.adbdeck.feature.dashboard

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.AdbServerState
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.settings.resolvedAdbPath
import com.adbdeck.core.utils.TerminalLauncher
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [DashboardComponent].
 *
 * Lifecycle компонента привязан к [ComponentContext], поэтому
 * coroutineScope автоматически отменяется при уничтожении компонента.
 *
 * @param componentContext Контекст Decompose-компонента.
 * @param adbClient ADB-клиент для проверки доступности.
 * @param deviceManager Менеджер устройств — единый источник списка устройств в приложении.
 * @param settingsRepository Репозиторий настроек (для получения текущего пути к adb).
 * @param onNavigateToDevices Callback навигации на экран устройств.
 * @param onNavigateToDeviceInfo Callback навигации на экран информации об устройстве.
 * @param onNavigateToQuickToggles Callback навигации на экран быстрых тумблеров.
 * @param onNavigateToLogcat  Callback навигации на экран logcat.
 * @param onNavigateToPackages Callback навигации на экран пакетов.
 * @param onNavigateToApkInstall Callback навигации на экран установки APK.
 * @param onNavigateToDeepLinks Callback навигации на экран Deep Links / Intents.
 * @param onNavigateToNotifications Callback навигации на экран уведомлений.
 * @param onNavigateToScreenTools Callback навигации на экран Screen Tools.
 * @param onNavigateToScrcpy Callback навигации на экран Scrcpy (Mirror screen).
 * @param onNavigateToFileExplorer Callback навигации на экран File Explorer.
 * @param onNavigateToFileSystem Callback навигации на экран File System.
 * @param onNavigateToContacts Callback навигации на экран Contacts.
 * @param onNavigateToSystemMonitor Callback навигации на экран System Monitor.
 * @param onNavigateToSettings Callback навигации на экран настроек.
 * @param onOpenAppUpdate Callback открытия доступного обновления приложения.
 * @param availableAppUpdateFlow Flow с данными о доступном обновлении приложения для Dashboard-баннера.
 * @param openAdbShellInTerminal Launcher системного терминала для быстрых shell-действий.
 */
class DefaultDashboardComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val availableAppUpdateFlow: Flow<DashboardAppUpdateBanner?> = emptyFlow(),
    private val onNavigateToDevices: () -> Unit,
    private val onNavigateToDeviceInfo: () -> Unit,
    private val onNavigateToQuickToggles: () -> Unit,
    private val onNavigateToLogcat: () -> Unit,
    private val onNavigateToPackages: () -> Unit,
    private val onNavigateToApkInstall: () -> Unit,
    private val onNavigateToDeepLinks: () -> Unit,
    private val onNavigateToNotifications: () -> Unit,
    private val onNavigateToScreenTools: () -> Unit,
    private val onNavigateToScrcpy: () -> Unit,
    private val onNavigateToFileExplorer: () -> Unit,
    private val onNavigateToFileSystem: () -> Unit,
    private val onNavigateToContacts: () -> Unit,
    private val onNavigateToSystemMonitor: () -> Unit,
    private val onNavigateToSettings: () -> Unit,
    private val openAppUpdateAction: () -> Unit = {},
    private val openAdbShellInTerminal: (adbPath: String, deviceId: String?, root: Boolean) -> Unit = TerminalLauncher::openAdbShell,
) : DashboardComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private var refreshJob: Job? = null
    private var adbCheckJob: Job? = null
    private var adbServerJob: Job? = null
    private var dismissedAppUpdateVersion: String? = null

    private val _state = MutableStateFlow(
        DashboardState(
            deviceCount = deviceManager.devicesFlow.value.size,
            adbServer = DashboardAdbServerUiState(
                adbPath = resolvedAdbPath(),
            ),
        )
    )
    override val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeDevices()
        observeAdbPath()
        observeAvailableAppUpdates()
    }

    override fun onOpenDevices() = onNavigateToDevices()

    override fun onOpenDeviceInfo() = onNavigateToDeviceInfo()

    override fun onOpenQuickToggles() = onNavigateToQuickToggles()

    override fun onOpenLogcat() = onNavigateToLogcat()

    override fun onOpenPackages() = onNavigateToPackages()

    override fun onOpenApkInstall() = onNavigateToApkInstall()

    override fun onOpenDeepLinks() = onNavigateToDeepLinks()

    override fun onOpenNotifications() = onNavigateToNotifications()

    override fun onOpenScreenTools() = onNavigateToScreenTools()

    override fun onOpenScrcpy() = onNavigateToScrcpy()

    override fun onOpenFileExplorer() = onNavigateToFileExplorer()

    override fun onOpenFileSystem() = onNavigateToFileSystem()

    override fun onOpenContacts() = onNavigateToContacts()

    override fun onOpenSystemMonitor() = onNavigateToSystemMonitor()

    override fun onOpenSettings() = onNavigateToSettings()

    override fun onOpenAdbShell() {
        openAdbShell(root = false)
    }

    override fun onOpenRootAdbShell() {
        openAdbShell(root = true)
    }

    override fun onRefreshDevices() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            _state.update { it.copy(isRefreshingDevices = true, refreshError = null) }
            try {
                deviceManager.clearError()
                deviceManager.refresh()
                _state.update { current ->
                    current.copy(
                        isRefreshingDevices = false,
                        refreshError = deviceManager.errorFlow.value,
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isRefreshingDevices = false) }
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        isRefreshingDevices = false,
                        refreshError = e.message,
                    )
                }
            } finally {
                refreshJob = null
            }
        }
    }

    override fun onCheckAdb() {
        if (adbCheckJob?.isActive == true) return
        adbCheckJob = scope.launch {
            _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Checking) }
            val adbPath = resolvedAdbPath()
            try {
                val result = adbClient.checkAvailability(adbPath)
                val nextState = when (result) {
                    is AdbCheckResult.Available -> DashboardAdbCheckState.Available(result.version)
                    is AdbCheckResult.NotAvailable -> DashboardAdbCheckState.NotAvailable(result.reason)
                }
                _state.update { it.copy(adbCheckState = nextState) }
                when (result) {
                    is AdbCheckResult.Available -> refreshAdbServerStateFromAvailability(
                        adbPath = adbPath,
                        availability = result,
                        clearActionError = false,
                    )

                    is AdbCheckResult.NotAvailable -> applyAdbNotFound(
                        adbPath = adbPath,
                        reason = result.reason,
                        clearActionError = false,
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Idle) }
                throw e
            } catch (e: Exception) {
                val reason = e.message.orEmpty()
                _state.update {
                    it.copy(
                        adbCheckState = DashboardAdbCheckState.NotAvailable(reason),
                    )
                }
                // Не оставляем устаревший статус блока Adb Core при ошибке проверки.
                applyAdbNotFound(
                    adbPath = adbPath,
                    reason = reason,
                    clearActionError = false,
                )
            } finally {
                adbCheckJob = null
            }
        }
    }

    override fun onRefreshAdbServerStatus() {
        if (adbServerJob?.isActive == true) return
        adbServerJob = scope.launch {
            setActiveServerAction(DashboardAdbServerAction.REFRESH)
            try {
                refreshAdbServerState(
                    adbPath = resolvedAdbPath(),
                    clearActionError = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                applyServerError(message = e.message.orEmpty())
            } finally {
                clearActiveServerAction()
                adbServerJob = null
            }
        }
    }

    override fun onStartAdbServer() {
        runServerAction(
            action = DashboardAdbServerAction.START,
            command = { adbPath -> adbClient.startServer(adbPath) },
        )
    }

    override fun onStopAdbServer() {
        runServerAction(
            action = DashboardAdbServerAction.STOP,
            command = { adbPath -> adbClient.stopServer(adbPath) },
        )
    }

    override fun onRestartAdbServer() {
        runServerAction(
            action = DashboardAdbServerAction.RESTART,
            command = { adbPath -> adbClient.restartServer(adbPath) },
        )
    }

    override fun onDismissAdbCheck() {
        _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Idle) }
    }

    override fun onDismissRefreshError() {
        _state.update { it.copy(refreshError = null) }
    }

    override fun onDismissAdbServerError() {
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(actionError = null),
            )
        }
    }

    override fun onDismissTerminalLaunchError() {
        _state.update { it.copy(isTerminalLaunchFailed = false) }
    }

    override fun onDismissAppUpdateBanner() {
        dismissedAppUpdateVersion = _state.value.appUpdateBanner?.version
        _state.update { it.copy(appUpdateBanner = null) }
    }

    override fun onOpenAppUpdate() = openAppUpdateAction()

    private fun observeDevices() {
        scope.launch {
            deviceManager.devicesFlow.collect { devices ->
                _state.update { current ->
                    if (current.deviceCount == devices.size) {
                        current
                    } else {
                        current.copy(deviceCount = devices.size)
                    }
                }
            }
        }
    }

    private fun observeAdbPath() {
        scope.launch {
            settingsRepository.settingsFlow
                .map { it.resolvedAdbPath() }
                .distinctUntilChanged()
                .collect { adbPath ->
                    _state.update { current ->
                        if (current.adbServer.adbPath == adbPath) {
                            current
                        } else {
                            current.copy(
                                adbServer = current.adbServer.copy(adbPath = adbPath),
                            )
                        }
                    }
                }
        }
    }

    private fun observeAvailableAppUpdates() {
        scope.launch {
            availableAppUpdateFlow.collect { banner ->
                if (banner == null) {
                    dismissedAppUpdateVersion = null
                }

                _state.update { current ->
                    val shouldShowBanner = banner != null &&
                        banner.version.isNotBlank() &&
                        banner.version != dismissedAppUpdateVersion
                    current.copy(
                        appUpdateBanner = if (shouldShowBanner) banner else null,
                    )
                }
            }
        }
    }

    private fun runServerAction(
        action: DashboardAdbServerAction,
        command: suspend (adbPath: String) -> Result<String>,
    ) {
        if (adbServerJob?.isActive == true) return
        adbServerJob = scope.launch {
            setActiveServerAction(action)
            try {
                val adbPath = resolvedAdbPath()
                when (val availability = adbClient.checkAvailability(adbPath)) {
                    is AdbCheckResult.NotAvailable -> {
                        applyAdbNotFound(adbPath = adbPath, reason = availability.reason)
                        return@launch
                    }

                    is AdbCheckResult.Available -> {
                        val actionResult = command(adbPath)
                        if (actionResult.isFailure) {
                            applyServerError(
                                message = actionResult.exceptionOrNull()?.message.orEmpty(),
                                adbPath = adbPath,
                                adbVersion = availability.version,
                            )
                            return@launch
                        }

                        val fallbackState = when (action) {
                            DashboardAdbServerAction.START,
                            DashboardAdbServerAction.RESTART,
                            -> DashboardAdbServerState.RUNNING

                            DashboardAdbServerAction.STOP -> DashboardAdbServerState.STOPPED
                            DashboardAdbServerAction.REFRESH -> null
                        }
                        refreshAdbServerState(
                            adbPath = adbPath,
                            clearActionError = true,
                            fallbackState = fallbackState,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                applyServerError(message = e.message.orEmpty())
            } finally {
                clearActiveServerAction()
                adbServerJob = null
            }
        }
    }

    private suspend fun refreshAdbServerState(
        adbPath: String,
        clearActionError: Boolean,
        fallbackState: DashboardAdbServerState? = null,
    ) {
        when (val availability = adbClient.checkAvailability(adbPath)) {
            is AdbCheckResult.NotAvailable -> {
                applyAdbNotFound(adbPath = adbPath, reason = availability.reason, clearActionError = clearActionError)
            }

            is AdbCheckResult.Available -> {
                refreshAdbServerStateFromAvailability(
                    adbPath = adbPath,
                    availability = availability,
                    clearActionError = clearActionError,
                    fallbackState = fallbackState,
                )
            }
        }
    }

    private suspend fun refreshAdbServerStateFromAvailability(
        adbPath: String,
        availability: AdbCheckResult.Available,
        clearActionError: Boolean,
        fallbackState: DashboardAdbServerState? = null,
    ) {
        val serverStatus = adbClient.getServerStatus(adbPath)
        val inferredState = when {
            serverStatus.state == AdbServerState.UNKNOWN &&
                deviceManager.devicesFlow.value.isNotEmpty() ->
                DashboardAdbServerState.RUNNING

            else -> serverStatus.state.toDashboardState()
        }
        val nextServerState = if (
            fallbackState != null &&
            serverStatus.state == AdbServerState.UNKNOWN
        ) {
            fallbackState
        } else {
            inferredState
        }
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(
                    adbPath = adbPath,
                    isAdbFound = true,
                    adbVersion = availability.version,
                    serverState = nextServerState,
                    serverMessage = serverStatus.message.ifBlank { null },
                    actionError = if (clearActionError) null else current.adbServer.actionError,
                ),
            )
        }
    }

    private fun applyAdbNotFound(
        adbPath: String,
        reason: String,
        clearActionError: Boolean = true,
    ) {
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(
                    adbPath = adbPath,
                    isAdbFound = false,
                    adbVersion = null,
                    serverState = DashboardAdbServerState.UNKNOWN,
                    serverMessage = reason.ifBlank { null },
                    actionError = if (clearActionError) null else current.adbServer.actionError,
                ),
            )
        }
    }

    private fun applyServerError(
        message: String,
        adbPath: String? = null,
        adbVersion: String? = null,
    ) {
        val normalizedMessage = message.trim()
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(
                    adbPath = adbPath ?: current.adbServer.adbPath,
                    isAdbFound = true,
                    adbVersion = adbVersion ?: current.adbServer.adbVersion,
                    serverState = DashboardAdbServerState.ERROR,
                    serverMessage = normalizedMessage.ifBlank { null },
                    actionError = normalizedMessage,
                ),
            )
        }
    }

    private fun setActiveServerAction(action: DashboardAdbServerAction) {
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(
                    activeAction = action,
                    actionError = null,
                ),
            )
        }
    }

    private fun clearActiveServerAction() {
        _state.update { current ->
            current.copy(
                adbServer = current.adbServer.copy(
                    activeAction = null,
                ),
            )
        }
    }

    private fun openAdbShell(root: Boolean) {
        scope.launch {
            val adbPath = resolvedAdbPath()
            val deviceId = deviceManager.selectedDeviceFlow.value
                ?.deviceId
                ?.trim()
                ?.ifBlank { null }

            val launchResult = runCatching {
                openAdbShellInTerminal(adbPath, deviceId, root)
            }
            _state.update { current ->
                current.copy(isTerminalLaunchFailed = launchResult.isFailure)
            }
        }
    }

    private fun resolvedAdbPath(): String =
        settingsRepository.resolvedAdbPath()

    private fun AdbServerState.toDashboardState(): DashboardAdbServerState = when (this) {
        AdbServerState.RUNNING -> DashboardAdbServerState.RUNNING
        AdbServerState.STOPPED -> DashboardAdbServerState.STOPPED
        AdbServerState.UNKNOWN -> DashboardAdbServerState.UNKNOWN
        AdbServerState.ERROR -> DashboardAdbServerState.ERROR
    }
}
