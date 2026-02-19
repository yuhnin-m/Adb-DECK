package com.adbdeck.feature.dashboard

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.AdbServerState
import com.adbdeck.core.adb.api.adb.AdbServerStatusResult
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.settings.AppSettings
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDashboardComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `device count follows device manager flow`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            deviceManager = FakeDeviceManager(
                initialDevices = listOf(device("emulator-5554")),
            ),
        )
        try {
            assertEquals(1, fixture.component.state.value.deviceCount)

            fixture.deviceManager.devicesFlow.value = listOf(
                device("emulator-5554"),
                device("emulator-5556"),
            )

            awaitCondition("Ожидалось обновление количества устройств") {
                fixture.component.state.value.deviceCount == 2
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `refresh uses device manager and reports error flow`() = runTest(context = testDispatcher) {
        val deviceManager = FakeDeviceManager(
            initialDevices = listOf(device("emulator-5554")),
            refreshDelayMs = 5_000,
            refreshErrorAfterCall = "adb devices failed",
        )
        val fixture = createFixture(deviceManager = deviceManager)
        try {
            fixture.component.onRefreshDevices()
            testDispatcher.scheduler.runCurrent()

            assertTrue(fixture.component.state.value.isRefreshingDevices)
            assertEquals(null, fixture.component.state.value.refreshError)

            testDispatcher.scheduler.advanceTimeBy(5_000)
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидалось завершение обновления устройств") {
                !fixture.component.state.value.isRefreshingDevices
            }
            assertEquals(1, fixture.deviceManager.refreshCalls)
            assertEquals("adb devices failed", fixture.component.state.value.refreshError)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `refresh error is shown when refresh throws`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            deviceManager = FakeDeviceManager(
                refreshThrowable = IllegalStateException("refresh exploded"),
            ),
        )
        try {
            fixture.component.onRefreshDevices()
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидалось сообщение об ошибке refresh") {
                fixture.component.state.value.refreshError == "refresh exploded" &&
                    !fixture.component.state.value.isRefreshingDevices
            }

            assertEquals("refresh exploded", fixture.component.state.value.refreshError)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `refresh ignores duplicated start while job is active`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            deviceManager = FakeDeviceManager(refreshDelayMs = 5_000),
        )
        try {
            fixture.component.onRefreshDevices()
            fixture.component.onRefreshDevices()
            testDispatcher.scheduler.runCurrent()

            assertTrue(fixture.component.state.value.isRefreshingDevices)
            assertEquals(1, fixture.deviceManager.refreshCalls)

            testDispatcher.scheduler.advanceTimeBy(5_000)
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидалось завершение refresh") {
                !fixture.component.state.value.isRefreshingDevices
            }
            assertEquals(1, fixture.deviceManager.refreshCalls)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `adb check ignores duplicated start and reports available state`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                delayMs = 5_000,
                checkResult = AdbCheckResult.Available("Android Debug Bridge version 1.0.41"),
            ),
        )
        try {
            fixture.component.onCheckAdb()
            fixture.component.onCheckAdb()
            testDispatcher.scheduler.runCurrent()

            assertTrue(fixture.component.state.value.adbCheckState is DashboardAdbCheckState.Checking)
            assertEquals(1, fixture.adbClient.checkCalls)

            testDispatcher.scheduler.advanceTimeBy(5_000)
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидался статус Available после проверки adb") {
                fixture.component.state.value.adbCheckState is DashboardAdbCheckState.Available
            }

            val adbState = fixture.component.state.value.adbCheckState as DashboardAdbCheckState.Available
            assertEquals("Android Debug Bridge version 1.0.41", adbState.version)
            assertEquals(1, fixture.adbClient.checkCalls)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `adb check also refreshes server status`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.Available("Android Debug Bridge version 1.0.41"),
                serverStatusResult = AdbServerStatusResult(
                    state = AdbServerState.STOPPED,
                    message = "daemon is not running",
                ),
            ),
        )
        try {
            fixture.component.onCheckAdb()
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидался синхронизированный статус ADB server после check adb") {
                fixture.component.state.value.adbServer.serverState == DashboardAdbServerState.STOPPED
            }

            assertEquals(1, fixture.adbClient.checkCalls)
            assertEquals(1, fixture.adbClient.serverStatusCalls)
            assertEquals(
                DashboardAdbServerState.STOPPED,
                fixture.component.state.value.adbServer.serverState,
            )
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `refresh adb server status populates server block`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.Available("Android Debug Bridge version 1.0.41"),
                serverStatusResult = AdbServerStatusResult(
                    state = AdbServerState.STOPPED,
                    message = "daemon is not running",
                ),
            ),
        )
        try {
            fixture.component.onRefreshAdbServerStatus()
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидалось завершение refresh статуса ADB server") {
                fixture.component.state.value.adbServer.activeAction == null &&
                    fixture.component.state.value.adbServer.serverState == DashboardAdbServerState.STOPPED
            }

            assertTrue(fixture.component.state.value.adbServer.isAdbFound)
            assertEquals(
                DashboardAdbServerState.STOPPED,
                fixture.component.state.value.adbServer.serverState,
            )
            assertEquals(1, fixture.adbClient.serverStatusCalls)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `unknown server status is inferred as running when device is visible`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.Available("Android Debug Bridge version 1.0.41"),
                serverStatusResult = AdbServerStatusResult(
                    state = AdbServerState.UNKNOWN,
                    message = "unsupported format",
                ),
            ),
            deviceManager = FakeDeviceManager(
                initialDevices = listOf(device("emulator-5554")),
            ),
        )
        try {
            fixture.component.onRefreshAdbServerStatus()
            testDispatcher.scheduler.runCurrent()

            awaitCondition("Ожидалось определение RUNNING при UNKNOWN и видимом устройстве") {
                fixture.component.state.value.adbServer.serverState == DashboardAdbServerState.RUNNING
            }

            assertEquals(
                DashboardAdbServerState.RUNNING,
                fixture.component.state.value.adbServer.serverState,
            )
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `start server does not execute command when adb is unavailable`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.NotAvailable("adb not found"),
            ),
        )
        try {
            fixture.component.onStartAdbServer()

            awaitCondition("Ожидалось завершение start server при недоступном adb") {
                fixture.component.state.value.adbServer.activeAction == null
            }

            assertEquals(0, fixture.adbClient.startServerCalls)
            assertTrue(!fixture.component.state.value.adbServer.isAdbFound)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `dismiss handlers clear feedback states`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.NotAvailable("adb not found"),
            ),
            deviceManager = FakeDeviceManager(
                refreshThrowable = IllegalArgumentException("refresh failed"),
            ),
        )
        try {
            fixture.component.onCheckAdb()
            awaitCondition("Ожидался NotAvailable после check adb") {
                fixture.component.state.value.adbCheckState is DashboardAdbCheckState.NotAvailable
            }

            fixture.component.onRefreshDevices()
            awaitCondition("Ожидалась ошибка refresh") {
                fixture.component.state.value.refreshError == "refresh failed"
            }

            fixture.component.onDismissAdbCheck()
            fixture.component.onDismissRefreshError()

            assertTrue(fixture.component.state.value.adbCheckState is DashboardAdbCheckState.Idle)
            assertEquals(null, fixture.component.state.value.refreshError)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `dismiss adb server error clears server action feedback`() = runTest(context = testDispatcher) {
        val fixture = createFixture(
            adbClient = FakeAdbClient(
                checkResult = AdbCheckResult.Available("Android Debug Bridge version 1.0.41"),
                startServerResult = Result.failure(IllegalStateException("failed to start server")),
            ),
        )
        try {
            fixture.component.onStartAdbServer()

            awaitCondition("Ожидалась ошибка действия ADB server") {
                fixture.component.state.value.adbServer.actionError == "failed to start server"
            }

            fixture.component.onDismissAdbServerError()
            assertEquals(null, fixture.component.state.value.adbServer.actionError)
        } finally {
            fixture.close()
        }
    }

    private suspend fun awaitCondition(
        errorMessage: String,
        timeoutMs: Long = 5_000,
        condition: () -> Boolean,
    ) {
        val scheduler = testDispatcher.scheduler
        val deadline = scheduler.currentTime + timeoutMs
        while (!condition()) {
            scheduler.advanceTimeBy(50)
            scheduler.runCurrent()
            if (scheduler.currentTime >= deadline) {
                fail(errorMessage)
            }
        }
    }

    private fun createFixture(
        adbClient: FakeAdbClient = FakeAdbClient(),
        deviceManager: FakeDeviceManager = FakeDeviceManager(),
    ): DashboardFixture {
        val lifecycle = LifecycleRegistry()
        lifecycle.resume()

        val component = DefaultDashboardComponent(
            componentContext = DefaultComponentContext(lifecycle),
            adbClient = adbClient,
            deviceManager = deviceManager,
            settingsRepository = FakeSettingsRepository(),
            onNavigateToDevices = {},
            onNavigateToDeviceInfo = {},
            onNavigateToQuickToggles = {},
            onNavigateToLogcat = {},
            onNavigateToPackages = {},
            onNavigateToApkInstall = {},
            onNavigateToDeepLinks = {},
            onNavigateToNotifications = {},
            onNavigateToScreenTools = {},
            onNavigateToFileExplorer = {},
            onNavigateToContacts = {},
            onNavigateToSystemMonitor = {},
            onNavigateToSettings = {},
        )
        testDispatcher.scheduler.runCurrent()

        return DashboardFixture(
            component = component,
            adbClient = adbClient,
            deviceManager = deviceManager,
            lifecycle = lifecycle,
        )
    }

    private data class DashboardFixture(
        val component: DefaultDashboardComponent,
        val adbClient: FakeAdbClient,
        val deviceManager: FakeDeviceManager,
        val lifecycle: LifecycleRegistry,
    ) {
        fun close() {
            lifecycle.destroy()
        }
    }

    private class FakeAdbClient(
        private val delayMs: Long = 0L,
        private val checkResult: AdbCheckResult = AdbCheckResult.Available("ok"),
        private val checkThrowable: Throwable? = null,
        private val serverStatusResult: AdbServerStatusResult = AdbServerStatusResult(AdbServerState.RUNNING),
        private val startServerResult: Result<String> = Result.success("started"),
        private val stopServerResult: Result<String> = Result.success("stopped"),
        private val restartServerResult: Result<String> = Result.success("restarted"),
    ) : AdbClient {

        var checkCalls: Int = 0
            private set
        var serverStatusCalls: Int = 0
            private set
        var startServerCalls: Int = 0
            private set
        var stopServerCalls: Int = 0
            private set
        var restartServerCalls: Int = 0
            private set

        override suspend fun checkAvailability(adbPathOverride: String?): AdbCheckResult {
            checkCalls++
            if (delayMs > 0) delay(delayMs)
            checkThrowable?.let { throw it }
            return checkResult
        }

        override suspend fun getServerStatus(adbPathOverride: String?): AdbServerStatusResult {
            serverStatusCalls++
            return serverStatusResult
        }

        override suspend fun startServer(adbPathOverride: String?): Result<String> {
            startServerCalls++
            return startServerResult
        }

        override suspend fun stopServer(adbPathOverride: String?): Result<String> {
            stopServerCalls++
            return stopServerResult
        }

        override suspend fun restartServer(adbPathOverride: String?): Result<String> {
            restartServerCalls++
            return restartServerResult
        }

        override suspend fun getDevices(): Result<List<AdbDevice>> = Result.success(emptyList())
    }

    private class FakeDeviceManager(
        initialDevices: List<AdbDevice> = emptyList(),
        private val refreshDelayMs: Long = 0L,
        private val refreshErrorAfterCall: String? = null,
        private val refreshThrowable: Throwable? = null,
    ) : DeviceManager {

        override val devicesFlow: MutableStateFlow<List<AdbDevice>> = MutableStateFlow(initialDevices)
        override val selectedDeviceFlow: MutableStateFlow<AdbDevice?> = MutableStateFlow(initialDevices.firstOrNull())
        override val isConnecting: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val errorFlow: MutableStateFlow<String?> = MutableStateFlow(null)
        override val savedEndpointsFlow: MutableStateFlow<List<DeviceEndpoint>> = MutableStateFlow(emptyList())

        var refreshCalls: Int = 0
            private set

        override suspend fun refresh() {
            refreshCalls++
            if (refreshDelayMs > 0) delay(refreshDelayMs)
            refreshThrowable?.let { throw it }
            errorFlow.value = refreshErrorAfterCall
        }

        override suspend fun connect(host: String, port: Int): Result<String> = Result.success("ok")

        override suspend fun disconnect(deviceId: String): Result<Unit> = Result.success(Unit)

        override suspend fun switchToTcpIp(serialId: String, port: Int): Result<Unit> = Result.success(Unit)

        override fun selectDevice(device: AdbDevice) {
            selectedDeviceFlow.value = device
        }

        override suspend fun saveEndpoint(endpoint: DeviceEndpoint) = Unit

        override suspend fun removeEndpoint(endpoint: DeviceEndpoint) = Unit

        override fun clearError() {
            errorFlow.value = null
        }
    }

    private fun device(id: String): AdbDevice = AdbDevice(
        deviceId = id,
        state = DeviceState.DEVICE,
    )

    private class FakeSettingsRepository : SettingsRepository {
        override val settingsFlow: StateFlow<AppSettings> = MutableStateFlow(AppSettings(adbPath = "adb"))

        override fun getSettings(): AppSettings = settingsFlow.value

        override suspend fun saveSettings(settings: AppSettings) = Unit
    }
}
