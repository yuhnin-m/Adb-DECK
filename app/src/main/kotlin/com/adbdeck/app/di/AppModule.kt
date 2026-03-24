package com.adbdeck.app.di

import com.adbdeck.app.APP_VERSION
import com.adbdeck.app.AppComponent
import com.adbdeck.app.DefaultAppComponent
import com.adbdeck.app.devicemanager.DefaultDeviceSelectorComponent
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.DefaultRootChildFactory
import com.adbdeck.app.navigation.DefaultRootComponent
import com.adbdeck.app.navigation.RootChildFactory
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.feature.update.AppUpdateComponent
import com.adbdeck.feature.update.DefaultAppUpdateComponent
import com.adbdeck.feature.update.provider.AppUpdateProvider
import com.adbdeck.feature.update.provider.GithubReleasesUpdateProvider
import com.arkivanov.decompose.ComponentContext
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.impl.monitoring.DefaultSystemMonitorClient
import com.adbdeck.core.adb.impl.adb.SystemAdbClient
import com.adbdeck.core.adb.impl.adb.SystemBundletoolClient
import com.adbdeck.core.adb.impl.scrcpy.SystemScrcpyClient
import com.adbdeck.core.adb.impl.contacts.SystemContactsClient
import com.adbdeck.core.adb.impl.intents.SystemIntentLaunchClient
import com.adbdeck.core.adb.impl.notifications.SystemNotificationsClient
import com.adbdeck.core.adb.impl.apkinstall.SystemApkInstallClient
import com.adbdeck.core.adb.impl.device.SystemDeviceControlClient
import com.adbdeck.core.adb.impl.files.SystemDeviceFileClient
import com.adbdeck.core.adb.impl.device.SystemDeviceInfoClient
import com.adbdeck.core.adb.impl.device.SystemDeviceManager
import com.adbdeck.core.adb.impl.logcat.SystemLogcatStreamer
import com.adbdeck.core.adb.impl.packages.SystemPackageClient
import com.adbdeck.core.adb.impl.screen.SystemScreenToolsClient
import com.adbdeck.core.process.InMemoryProcessHistoryStore
import com.adbdeck.core.process.LoggingProcessRunner
import com.adbdeck.core.process.ProcessHistoryStore
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.process.SystemProcessRunner
import com.adbdeck.core.settings.FileSettingsRepository
import com.adbdeck.core.settings.SettingsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/**
 * Корневой Koin-модуль приложения.
 *
 * Собирает все зависимости в одном месте:
 * - Инфраструктурный слой ([ProcessRunner])
 * - Настройки ([SettingsRepository])
 * - ADB-клиент ([AdbClient])
 * - Менеджер устройств ([DeviceManager]) — singleton, живет весь lifecycle приложения
 * - Streamer логкэта ([LogcatStreamer]) — singleton, stateless factory потоков
 *
 * Все зависимости — singletons, создаются один раз при старте приложения.
 */
val appModule = module {

    // ── ProcessRunner + in-memory history ───────────────────────
    single<ProcessHistoryStore> { InMemoryProcessHistoryStore(capacity = 500) }
    singleOf(::SystemProcessRunner)
    single<ProcessRunner> {
        LoggingProcessRunner(
            delegate = get<SystemProcessRunner>(),
            historyStore = get<ProcessHistoryStore>(),
        )
    }

    // ── SettingsRepository ───────────────────────────────────────
    single<SettingsRepository> {
        FileSettingsRepository(
            settingsFile = File(
                System.getProperty("user.home"),
                ".adbdeck/settings.json",
            )
        )
    }

    // ── AdbClient ────────────────────────────────────────────────
    single<AdbClient> {
        SystemAdbClient(
            processRunner = get(),
            settingsRepository = get(),
        )
    }

    // ── BundletoolClient ──────────────────────────────────────────
    single<BundletoolClient> {
        SystemBundletoolClient(
            processRunner = get(),
            settingsRepository = get(),
        )
    }

    // ── DeviceManager ────────────────────────────────────────────
    single<DeviceManager> {
        SystemDeviceManager(
            adbClient = get(),
            processRunner = get(),
            settingsRepository = get(),
        )
    }

    // ── LogcatStreamer ────────────────────────────────────────────
    singleOf(::SystemLogcatStreamer) bind LogcatStreamer::class

    // ── PackageClient ─────────────────────────────────────────────
    singleOf(::SystemPackageClient) bind PackageClient::class

    // ── SystemMonitorClient ───────────────────────────────────────
    singleOf(::DefaultSystemMonitorClient) bind SystemMonitorClient::class

    // ── DeviceInfoClient ─────────────────────────────────────────
    singleOf(::SystemDeviceInfoClient) bind DeviceInfoClient::class

    // ── DeviceControlClient ───────────────────────────────────────
    singleOf(::SystemDeviceControlClient) bind DeviceControlClient::class

    // ── DeviceFileClient ──────────────────────────────────────────
    singleOf(::SystemDeviceFileClient) bind DeviceFileClient::class

    // ── ContactsClient ────────────────────────────────────────────
    singleOf(::SystemContactsClient) bind ContactsClient::class

    // ── ScreenToolsClient ─────────────────────────────────────────
    singleOf(::SystemScreenToolsClient) bind ScreenToolsClient::class

    // ── ScrcpyClient ──────────────────────────────────────────────
    single<ScrcpyClient> {
        SystemScrcpyClient(
            processRunner = get(),
            settingsRepository = get(),
            historyStore = get(),
        )
    }

    // ── ApkInstallClient ──────────────────────────────────────────
    singleOf(::SystemApkInstallClient) bind ApkInstallClient::class

    // ── IntentLaunchClient ────────────────────────────────────────
    singleOf(::SystemIntentLaunchClient) bind IntentLaunchClient::class

    // ── NotificationsClient ───────────────────────────────────────
    singleOf(::SystemNotificationsClient) bind NotificationsClient::class

    // ── App-level component graph ─────────────────────────────────
    singleOf(::DefaultRootChildFactory) bind RootChildFactory::class

    factory<RootComponent> { (componentContext: ComponentContext, appUpdateComponent: AppUpdateComponent) ->
        DefaultRootComponent(
            componentContext = componentContext,
            rootChildFactory = get(),
            appUpdateComponent = appUpdateComponent,
        )
    }

    factory<DeviceSelectorComponent> { (componentContext: ComponentContext) ->
        DefaultDeviceSelectorComponent(
            componentContext = componentContext,
            deviceManager = get(),
        )
    }

    single<AppUpdateProvider> {
        GithubReleasesUpdateProvider()
    }

    factory<AppUpdateComponent> { (componentContext: ComponentContext) ->
        DefaultAppUpdateComponent(
            componentContext = componentContext,
            appUpdateProvider = get(),
            currentVersion = APP_VERSION,
        )
    }

    factory<AppComponent> { (componentContext: ComponentContext) ->
        val appUpdateComponent = get<AppUpdateComponent> { parametersOf(componentContext) }
        DefaultAppComponent(
            rootComponent = get<RootComponent> { parametersOf(componentContext, appUpdateComponent) },
            deviceSelectorComponent = get<DeviceSelectorComponent> { parametersOf(componentContext) },
            appUpdateComponent = appUpdateComponent,
        )
    }
}
