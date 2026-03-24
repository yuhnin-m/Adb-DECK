package com.adbdeck.app

import adbdeck.app.generated.resources.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.adbdeck.app.di.appModule
import com.adbdeck.app.ui.AppContent
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.process.ProcessHistoryStore
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import java.awt.Dimension
import java.util.Locale

private const val APP_TITLE = "ADB Deck"

/**
 * Точка входа приложения ADB Deck.
 *
 * Порядок инициализации:
 * 1. Запуск Koin DI
 * 2. Создание Decompose LifecycleRegistry + AppComponent
 * 3. Открытие главного окна Compose Desktop
 */
fun main() = application {
    // ── 1. Инициализация DI ──────────────────────────────────────
    startKoin {
        modules(appModule)
    }

    // ── 2. Lifecycle и AppComponent ──────────────────────────────
    val lifecycle = LifecycleRegistry()
    val koin = GlobalContext.get()
    val settingsRepository: SettingsRepository = koin.get()
    val processHistoryStore: ProcessHistoryStore = koin.get()

    val componentContext = DefaultComponentContext(lifecycle = lifecycle)
    val appComponent: AppComponent = koin.get { parametersOf(componentContext) }

    lifecycle.resume()

    // ── 3. Главное окно ──────────────────────────────────────────
    Window(
        onCloseRequest = {
            lifecycle.stop()
            lifecycle.destroy()
            exitApplication()
        },
        title = APP_TITLE,
        icon = painterResource(Res.drawable.adbdeck_window),
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(1000, 800)
            onDispose {}
        }

        // Подписка на настройки — управляет темой на уровне всего приложения
        val settings by settingsRepository.settingsFlow.collectAsState()
        val appUpdateState by appComponent.appUpdateComponent.state.collectAsState()
        val scope = rememberCoroutineScope()
        val startupLocale = remember { Locale.getDefault() }
        var languageReloadKey by remember { mutableIntStateOf(0) }

        val isDark = when (settings.theme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }

        LaunchedEffect(settings.language, startupLocale) {
            val localeChanged = applyJvmLocale(settings.language.resolveLocale(startupLocale))
            if (localeChanged) {
                languageReloadKey++
            }
        }

        LaunchedEffect(appComponent.appUpdateComponent) {
            appComponent.appUpdateComponent.onStart()
        }

        key(languageReloadKey) {
            AdbDeckTheme(isDarkTheme = isDark) {
                AppContent(
                    rootComponent = appComponent.rootComponent,
                    isDarkTheme = isDark,
                    onToggleTheme = {
                        val newTheme = if (isDark) AppTheme.LIGHT else AppTheme.DARK
                        scope.launch {
                            settingsRepository.saveSettings(settings.copy(theme = newTheme))
                        }
                    },
                    deviceSelectorComponent = appComponent.deviceSelectorComponent,
                    processHistoryStore = processHistoryStore,
                    appUpdateState = appUpdateState,
                    onInstallUpdateNow = appComponent.appUpdateComponent::onInstallUpdateNow,
                    onCancelUpdate = appComponent.appUpdateComponent::onCancelUpdate,
                    onDismissUpdate = appComponent.appUpdateComponent::onDismissUpdate,
                )
            }
        }
    }
}

private fun AppLanguage.resolveLocale(startupLocale: Locale): Locale = when (this) {
    AppLanguage.SYSTEM -> startupLocale
    AppLanguage.ENGLISH -> Locale.US
    AppLanguage.RUSSIAN -> Locale.forLanguageTag("ru-RU")
}

private fun applyJvmLocale(locale: Locale): Boolean {
    if (Locale.getDefault() == locale) return false
    Locale.setDefault(locale)
    System.setProperty("user.language", locale.language)
    if (locale.country.isNotBlank()) {
        System.setProperty("user.country", locale.country)
    }
    return true
}
