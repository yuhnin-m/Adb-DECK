package com.adbdeck.feature.update.logging

/**
 * Контракт логирования событий update-фичи.
 */
interface AppUpdateLogger {

    /** Низкорисковые диагностические события. */
    fun info(message: String)

    /** Предупреждения, не блокирующие основную работу приложения. */
    fun warn(message: String, throwable: Throwable? = null)

    /** Ошибки проверки/обработки обновления. */
    fun error(message: String, throwable: Throwable? = null)
}

/**
 * Пустая реализация логгера (для preview и fallback-сценариев).
 */
object NoOpAppUpdateLogger : AppUpdateLogger {
    override fun info(message: String) = Unit
    override fun warn(message: String, throwable: Throwable?) = Unit
    override fun error(message: String, throwable: Throwable?) = Unit
}
