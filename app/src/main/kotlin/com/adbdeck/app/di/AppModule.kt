package com.adbdeck.app.di

import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.impl.DefaultSystemMonitorClient
import com.adbdeck.core.adb.impl.SystemAdbClient
import com.adbdeck.core.adb.impl.SystemContactsClient
import com.adbdeck.core.adb.impl.SystemIntentLaunchClient
import com.adbdeck.core.adb.impl.SystemNotificationsClient
import com.adbdeck.core.adb.impl.SystemDeviceControlClient
import com.adbdeck.core.adb.impl.SystemDeviceFileClient
import com.adbdeck.core.adb.impl.SystemDeviceInfoClient
import com.adbdeck.core.adb.impl.SystemDeviceManager
import com.adbdeck.core.adb.impl.SystemLogcatStreamer
import com.adbdeck.core.adb.impl.SystemPackageClient
import com.adbdeck.core.adb.impl.SystemScreenToolsClient
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.process.SystemProcessRunner
import com.adbdeck.core.settings.FileSettingsRepository
import com.adbdeck.core.settings.SettingsRepository
import org.koin.core.module.dsl.singleOf
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

    // ── ProcessRunner ────────────────────────────────────────────
    singleOf(::SystemProcessRunner) bind ProcessRunner::class

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

    // ── IntentLaunchClient ────────────────────────────────────────
    singleOf(::SystemIntentLaunchClient) bind IntentLaunchClient::class

    // ── NotificationsClient ───────────────────────────────────────
    singleOf(::SystemNotificationsClient) bind NotificationsClient::class
}
