package com.adbdeck.feature.scrcpy

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.device.SavedWifiDevice
import com.adbdeck.core.adb.api.scrcpy.ScrcpyCheckResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyExitResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyLaunchRequest
import com.adbdeck.core.adb.api.scrcpy.ScrcpySession
import com.adbdeck.core.settings.AppSettings
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultScrcpyComponentTest {

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
    fun `start is rejected when no active device`() = runTest(context = testDispatcher) {
        val fixture = createFixture(selectedDevice = null)
        try {
            fixture.component.startScrcpy()
            testDispatcher.scheduler.runCurrent()

            assertEquals(0, fixture.scrcpyClient.startCalls)
            assertEquals(ScrcpyProcessState.IDLE, fixture.component.state.value.processState)
            assertTrue(fixture.component.state.value.feedback?.isError == true)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `start maps config into launch request and switches to running`() = runTest(context = testDispatcher) {
        val session = FakeScrcpySession(sessionId = "session-start")
        val fixture = createFixture(
            selectedDevice = device("emulator-5554"),
            scrcpyClient = FakeScrcpyClient(startResult = Result.success(session)),
        )
        try {
            fixture.component.onMaxResolutionChanged(ScrcpyMaxResolution.P720)
            fixture.component.onFpsChanged(ScrcpyFps.FPS_30)
            fixture.component.onBitrateChanged("12")
            fixture.component.onAllowInputChanged(false)
            fixture.component.onFullscreenChanged(true)
            fixture.component.onWindowWidthChanged("1280")
            fixture.component.onWindowHeightChanged("720")
            fixture.component.onVideoCodecChanged(ScrcpyVideoCodec.H265)
            fixture.component.onKeyboardModeChanged(ScrcpyInputMode.UHID)
            fixture.component.onMouseModeChanged(ScrcpyInputMode.AOA)

            fixture.component.startScrcpy()
            awaitCondition("Ожидался запуск scrcpy") {
                fixture.component.state.value.processState == ScrcpyProcessState.RUNNING
            }

            assertEquals(1, fixture.scrcpyClient.startCalls)
            val request = fixture.scrcpyClient.lastRequest
            assertNotNull(request)
            assertEquals("emulator-5554", request.deviceId)
            assertEquals(720, request.maxSize)
            assertEquals(30, request.maxFps)
            assertEquals(12, request.bitrateMbps)
            assertEquals(false, request.allowInput)
            assertEquals(true, request.fullscreen)
            assertEquals(1280, request.windowWidth)
            assertEquals(720, request.windowHeight)
            assertEquals("h265", request.videoCodec)
            assertEquals("uhid", request.keyboardMode)
            assertEquals("aoa", request.mouseMode)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `stop transitions to idle and calls session stop`() = runTest(context = testDispatcher) {
        val session = FakeScrcpySession(sessionId = "session-stop")
        val fixture = createFixture(
            selectedDevice = device("emulator-5554"),
            scrcpyClient = FakeScrcpyClient(startResult = Result.success(session)),
        )
        try {
            fixture.component.startScrcpy()
            awaitCondition("Ожидался запуск перед stop") {
                fixture.component.state.value.processState == ScrcpyProcessState.RUNNING
            }

            fixture.component.stopScrcpy()
            awaitCondition("Ожидалось завершение stop") {
                fixture.component.state.value.processState == ScrcpyProcessState.IDLE
            }

            assertEquals(1, session.stopCalls)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `unexpected session exit reports error feedback`() = runTest(context = testDispatcher) {
        val session = FakeScrcpySession(sessionId = "session-crash")
        val fixture = createFixture(
            selectedDevice = device("emulator-5554"),
            scrcpyClient = FakeScrcpyClient(startResult = Result.success(session)),
        )
        try {
            fixture.component.startScrcpy()
            awaitCondition("Ожидался запуск перед симуляцией crash") {
                fixture.component.state.value.processState == ScrcpyProcessState.RUNNING
            }

            session.completeExit(exitCode = 1, output = "boom")
            awaitCondition("Ожидалось состояние IDLE после crash") {
                fixture.component.state.value.processState == ScrcpyProcessState.IDLE &&
                    fixture.component.state.value.feedback?.isError == true
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `stop failure keeps error state`() = runTest(context = testDispatcher) {
        val session = FakeScrcpySession(
            sessionId = "session-stop-failed",
            stopFailure = IllegalStateException("cannot stop"),
        )
        val fixture = createFixture(
            selectedDevice = device("emulator-5554"),
            scrcpyClient = FakeScrcpyClient(startResult = Result.success(session)),
        )
        try {
            fixture.component.startScrcpy()
            awaitCondition("Ожидался запуск перед stop failure") {
                fixture.component.state.value.processState == ScrcpyProcessState.RUNNING
            }

            fixture.component.stopScrcpy()
            awaitCondition("Ожидалось состояние ERROR при неуспешной остановке") {
                fixture.component.state.value.processState == ScrcpyProcessState.ERROR &&
                    fixture.component.state.value.feedback?.isError == true
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `bitrate accepts only known presets`() = runTest(context = testDispatcher) {
        val fixture = createFixture(selectedDevice = device("emulator-5554"))
        try {
            fixture.component.onBitrateChanged("8")
            assertEquals("8", fixture.component.state.value.config.bitrate)

            fixture.component.onBitrateChanged("7")
            assertEquals("8", fixture.component.state.value.config.bitrate)

            fixture.component.onBitrateChanged("abc")
            assertEquals("8", fixture.component.state.value.config.bitrate)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `start is blocked when window dimensions are out of range`() = runTest(context = testDispatcher) {
        val fixture = createFixture(selectedDevice = device("emulator-5554"))
        try {
            fixture.component.onWindowWidthChanged("1")
            fixture.component.startScrcpy()
            testDispatcher.scheduler.runCurrent()

            assertEquals(0, fixture.scrcpyClient.startCalls)
            assertEquals(ScrcpyProcessState.ERROR, fixture.component.state.value.processState)
            assertTrue(fixture.component.state.value.feedback?.isError == true)
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
        selectedDevice: AdbDevice? = device("emulator-5554"),
        scrcpyClient: FakeScrcpyClient = FakeScrcpyClient(),
    ): ScrcpyFixture {
        val lifecycle = LifecycleRegistry()
        lifecycle.resume()

        val deviceManager = FakeDeviceManager(selectedDevice)
        val settingsRepository = FakeSettingsRepository()

        val component = DefaultScrcpyComponent(
            componentContext = DefaultComponentContext(lifecycle),
            deviceManager = deviceManager,
            settingsRepository = settingsRepository,
            scrcpyClient = scrcpyClient,
            onOpenSettings = {},
            messageProvider = FakeScrcpyMessageProvider(),
        )
        testDispatcher.scheduler.runCurrent()

        return ScrcpyFixture(
            component = component,
            scrcpyClient = scrcpyClient,
            lifecycle = lifecycle,
        )
    }

    private data class ScrcpyFixture(
        val component: DefaultScrcpyComponent,
        val scrcpyClient: FakeScrcpyClient,
        val lifecycle: LifecycleRegistry,
    ) {
        fun close() {
            lifecycle.destroy()
        }
    }

    private class FakeScrcpyClient(
        var startResult: Result<ScrcpySession> = Result.failure(IllegalStateException("not configured")),
    ) : ScrcpyClient {
        var startCalls: Int = 0
            private set
        var lastRequest: ScrcpyLaunchRequest? = null
            private set
        var lastPathOverride: String? = null
            private set

        override suspend fun checkAvailability(scrcpyPathOverride: String?): ScrcpyCheckResult {
            return ScrcpyCheckResult.Available("scrcpy 2.5")
        }

        override suspend fun startSession(
            request: ScrcpyLaunchRequest,
            scrcpyPathOverride: String?,
        ): Result<ScrcpySession> {
            startCalls += 1
            lastRequest = request
            lastPathOverride = scrcpyPathOverride
            return startResult
        }
    }

    private class FakeScrcpySession(
        override val sessionId: String,
        private val stopFailure: Throwable? = null,
    ) : ScrcpySession {
        override val deviceId: String = "emulator-5554"

        private val exitDeferred = CompletableDeferred<ScrcpyExitResult>()
        private var alive: Boolean = true

        var stopCalls: Int = 0
            private set

        override fun isAlive(): Boolean = alive

        override suspend fun awaitExit(): ScrcpyExitResult {
            val result = exitDeferred.await()
            alive = false
            return result
        }

        override suspend fun stop(gracefulTimeoutMs: Long): Result<Unit> {
            stopCalls += 1
            val failure = stopFailure
            if (failure != null) {
                return Result.failure(failure)
            }

            completeExit(exitCode = 0, output = "")
            return Result.success(Unit)
        }

        fun completeExit(exitCode: Int, output: String) {
            alive = false
            exitDeferred.complete(ScrcpyExitResult(exitCode = exitCode, output = output))
        }
    }

    private class FakeDeviceManager(
        selectedDevice: AdbDevice?,
    ) : DeviceManager {
        override val devicesFlow: MutableStateFlow<List<AdbDevice>> = MutableStateFlow(
            selectedDevice?.let { listOf(it) } ?: emptyList(),
        )
        override val selectedDeviceFlow: MutableStateFlow<AdbDevice?> = MutableStateFlow(selectedDevice)
        override val isConnecting: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val errorFlow: MutableStateFlow<String?> = MutableStateFlow(null)
        override val savedEndpointsFlow: MutableStateFlow<List<DeviceEndpoint>> = MutableStateFlow(emptyList())
        override val wifiHistoryFlow: MutableStateFlow<List<SavedWifiDevice>> = MutableStateFlow(emptyList())

        override suspend fun refresh() = Unit

        override suspend fun connect(host: String, port: Int): Result<String> = Result.success("ok")

        override suspend fun disconnect(deviceId: String): Result<Unit> = Result.success(Unit)

        override suspend fun switchToTcpIp(serialId: String, port: Int): Result<Unit> = Result.success(Unit)

        override fun selectDevice(device: AdbDevice) {
            selectedDeviceFlow.value = device
        }

        override suspend fun saveEndpoint(endpoint: DeviceEndpoint) = Unit

        override suspend fun removeEndpoint(endpoint: DeviceEndpoint) = Unit

        override suspend fun upsertWifiHistory(entry: SavedWifiDevice) = Unit

        override suspend fun removeWifiHistory(address: String) = Unit

        override fun clearError() {
            errorFlow.value = null
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settingsFlow: StateFlow<AppSettings> = MutableStateFlow(AppSettings(scrcpyPath = "scrcpy"))

        override fun getSettings(): AppSettings = settingsFlow.value

        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeScrcpyMessageProvider : ScrcpyMessageProvider {
        override suspend fun noDevice(): String = "No device"
        override suspend fun notConfigured(): String = "Not configured"
        override suspend fun started(): String = "Started"
        override suspend fun stopped(): String = "Stopped"
        override suspend fun startFailed(reason: String): String = "Start failed: $reason"
        override suspend fun stopFailed(reason: String): String = "Stop failed: $reason"
        override suspend fun crashed(reason: String): String = "Crashed: $reason"
        override suspend fun windowWidthRange(min: Int, max: Int): String = "Width must be in $min..$max"
        override suspend fun windowHeightRange(min: Int, max: Int): String = "Height must be in $min..$max"
    }

    private companion object {
        fun device(id: String): AdbDevice = AdbDevice(
            deviceId = id,
            state = DeviceState.DEVICE,
        )
    }
}
