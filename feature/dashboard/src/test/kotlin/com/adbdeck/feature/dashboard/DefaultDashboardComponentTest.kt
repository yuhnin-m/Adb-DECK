package com.adbdeck.feature.dashboard

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
            onNavigateToDevices = {},
            onNavigateToLogcat = {},
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
    ) : AdbClient {

        var checkCalls: Int = 0
            private set

        override suspend fun checkAvailability(adbPathOverride: String?): AdbCheckResult {
            checkCalls++
            if (delayMs > 0) delay(delayMs)
            checkThrowable?.let { throw it }
            return checkResult
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
}
