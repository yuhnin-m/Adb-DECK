package com.adbdeck.core.adb.api.monitoring

/**
 * Результат выполнения `adb shell sh -c <command>`.
 *
 * @param exitCode Код завершения shell-команды.
 * @param stdout Стандартный вывод команды.
 * @param stderr Вывод ошибок команды.
 */
data class ShellCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    /** `true`, если команда завершилась успешно (exitCode == 0). */
    val isSuccess: Boolean get() = exitCode == 0
}
