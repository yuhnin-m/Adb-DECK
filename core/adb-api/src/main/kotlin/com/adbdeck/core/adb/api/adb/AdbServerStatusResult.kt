package com.adbdeck.core.adb.api.adb

/**
 * Тип состояния ADB server.
 */
enum class AdbServerState {
    RUNNING,
    STOPPED,
    UNKNOWN,
    ERROR,
}

/**
 * Результат запроса состояния ADB server.
 *
 * @param state   Определенное состояние сервера.
 * @param message Дополнительный диагностический текст (stdout/stderr, причина ошибки и т.п.).
 */
data class AdbServerStatusResult(
    val state: AdbServerState,
    val message: String = "",
)
