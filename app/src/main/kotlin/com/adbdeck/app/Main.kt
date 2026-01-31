package com.adbdeck.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.adbdeck.app.devicemanager.DefaultDeviceSelectorComponent
import com.adbdeck.app.di.appModule
import com.adbdeck.app.navigation.DefaultRootComponent
import com.adbdeck.app.ui.AppContent
import com.adbdeck.core.adb.api.adb.AdbClient
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
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.process.ProcessHistoryStore
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

/**
 * Точка входа приложения ADB Deck.
 *
 * Порядок инициализации:
 * 1. Запуск Koin DI
 * 2. Создание Decompose LifecycleRegistry + RootComponent
 * 3. Создание DeviceSelectorComponent (вне Decompose, привязан к Window-scope)
 * 4. Открытие главного окна Compose Desktop
 */
fun main() = application {
    // ── 1. Инициализация DI ──────────────────────────────────────
    startKoin {
        modules(appModule)
    }

    // ── 2. Lifecycle и RootComponent ─────────────────────────────
    val lifecycle = LifecycleRegistry()
    val settingsRepository: SettingsRepository = get(SettingsRepository::class.java)
    val deviceManager: DeviceManager = get(DeviceManager::class.java)
    val logcatStreamer: LogcatStreamer = get(LogcatStreamer::class.java)
    val processHistoryStore: ProcessHistoryStore = get(ProcessHistoryStore::class.java)

    val rootComponent = DefaultRootComponent(
        componentContext    = DefaultComponentContext(lifecycle = lifecycle),
        adbClient           = get(AdbClient::class.java),
        settingsRepository  = settingsRepository,
        deviceManager       = deviceManager,
        logcatStreamer      = logcatStreamer,
        packageClient       = get(PackageClient::class.java),
        systemMonitorClient = get(SystemMonitorClient::class.java),
        deviceInfoClient    = get(DeviceInfoClient::class.java),
        deviceControlClient = get(DeviceControlClient::class.java),
        deviceFileClient    = get(DeviceFileClient::class.java),
        contactsClient      = get(ContactsClient::class.java),
        screenToolsClient   = get(ScreenToolsClient::class.java),
        apkInstallClient    = get(ApkInstallClient::class.java),
        processRunner       = get(ProcessRunner::class.java),
        intentLaunchClient  = get(IntentLaunchClient::class.java),
        notificationsClient = get(NotificationsClient::class.java),
    )

    lifecycle.resume()

    // ── 3. Главное окно ──────────────────────────────────────────
    Window(
        onCloseRequest = {
            lifecycle.stop()
            exitApplication()
        },
        title = "ADB Deck",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        // Подписка на настройки — управляет темой на уровне всего приложения
        val settings by settingsRepository.settingsFlow.collectAsState()
        val scope = rememberCoroutineScope()

        // DeviceSelectorComponent создается один раз и живет пока открыто окно.
        // scope из rememberCoroutineScope привязан к Window-composition.
        val deviceSelectorComponent = remember(scope) {
            DefaultDeviceSelectorComponent(
                deviceManager = deviceManager,
                scope = scope,
            )
        }

        val isDark = when (settings.theme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }

        AdbDeckTheme(isDarkTheme = isDark) {
            AppContent(
                rootComponent = rootComponent,
                isDarkTheme = isDark,
                onToggleTheme = {
                    val newTheme = if (isDark) AppTheme.LIGHT else AppTheme.DARK
                    scope.launch {
                        settingsRepository.saveSettings(settings.copy(theme = newTheme))
                    }
                },
                deviceSelectorComponent = deviceSelectorComponent,
                processHistoryStore = processHistoryStore,
            )
        }
    }
}
