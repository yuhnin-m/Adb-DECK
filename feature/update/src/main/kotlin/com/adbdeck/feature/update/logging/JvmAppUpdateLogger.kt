package com.adbdeck.feature.update.logging

import java.util.logging.Level
import java.util.logging.Logger

/**
 * JVM-реализация [AppUpdateLogger] на базе [Logger].
 */
class JvmAppUpdateLogger(
    private val logger: Logger = Logger.getLogger(LOGGER_NAME),
) : AppUpdateLogger {

    override fun info(message: String) {
        logger.log(Level.INFO, message)
    }

    override fun warn(message: String, throwable: Throwable?) {
        logger.log(Level.WARNING, message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        logger.log(Level.SEVERE, message, throwable)
    }

    private companion object {
        private const val LOGGER_NAME = "com.adbdeck.feature.update"
    }
}
