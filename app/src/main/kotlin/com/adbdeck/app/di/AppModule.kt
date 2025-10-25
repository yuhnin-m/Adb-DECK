package com.adbdeck.app.di

import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.LogcatStreamer
import com.adbdeck.core.adb.api.PackageClient
import com.adbdeck.core.adb.impl.SystemAdbClient
import com.adbdeck.core.adb.impl.SystemDeviceManager
import com.adbdeck.core.adb.impl.SystemLogcatStreamer
import com.adbdeck.core.adb.impl.SystemPackageClient
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
}
