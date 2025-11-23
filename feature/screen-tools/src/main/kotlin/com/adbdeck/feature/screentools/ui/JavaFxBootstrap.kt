package com.adbdeck.feature.screentools.ui

import javafx.application.Platform
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Ленивая инициализация JavaFX runtime для встроенного video preview.
 *
 * Инициализация выносится из момента открытия вкладки, чтобы убрать фриз
 * при первом показе плеера.
 */
internal object JavaFxBootstrap {

    private val initialized = AtomicBoolean(false)

    /**
     * Выполняет однократный прогрев JavaFX runtime.
     */
    fun prewarm() {
        if (!initialized.compareAndSet(false, true)) return

        // Снижаем шум логов JavaFX о classpath-конфигурации.
        runCatching {
            Logger.getLogger("com.sun.javafx.application.PlatformImpl").level = Level.SEVERE
        }

        runCatching {
            Platform.startup { /* no-op */ }
        }.recoverCatching {
            // Если runtime уже поднят, JavaFX бросает IllegalStateException — это штатно.
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater { /* no-op */ }
            }
        }
    }
}
