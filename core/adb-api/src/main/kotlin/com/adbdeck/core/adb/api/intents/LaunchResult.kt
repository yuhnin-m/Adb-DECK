package com.adbdeck.core.adb.api.intents

/**
 * Результат выполнения `adb shell am start`.
 *
 * @param exitCode       Код завершения процесса.
 * @param stdout         Стандартный вывод команды.
 * @param stderr         Вывод ошибок.
 * @param commandPreview Полная строка команды (для отображения пользователю).
 */
data class LaunchResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val commandPreview: String,
) {
    /**
     * `true` если команда завершилась без ошибок.
     *
     * ADB может вернуть exitCode=0, но с "Error" в stdout
     * (например, если activity не найдена).
     */
    val isSuccess: Boolean
        get() = exitCode == 0
            && !stdout.contains("Error", ignoreCase = true)
            && !stderr.contains("Error", ignoreCase = true)
}
