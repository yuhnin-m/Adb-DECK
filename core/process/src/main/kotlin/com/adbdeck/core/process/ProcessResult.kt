package com.adbdeck.core.process

/**
 * Результат выполнения внешнего системного процесса.
 *
 * @param exitCode Код завершения процесса (0 = успех).
 * @param stdout Стандартный вывод процесса.
 * @param stderr Вывод ошибок процесса.
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    /** Возвращает `true`, если процесс завершился успешно (exitCode == 0). */
    val isSuccess: Boolean get() = exitCode == 0

    /** Возвращает первую непустую строку из stdout, либо пустую строку. */
    val firstOutputLine: String get() = stdout.lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
}
