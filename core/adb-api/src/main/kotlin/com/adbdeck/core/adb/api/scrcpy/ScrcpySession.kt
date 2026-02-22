package com.adbdeck.core.adb.api.scrcpy

/**
 * Результат завершения scrcpy-процесса.
 *
 * @param exitCode Код завершения процесса.
 * @param output   Объединённый вывод процесса (stdout/stderr), если доступен.
 */
data class ScrcpyExitResult(
    val exitCode: Int,
    val output: String = "",
)

/**
 * Активная scrcpy-сессия.
 */
interface ScrcpySession {

    /** Уникальный идентификатор сессии в рамках текущего процесса приложения. */
    val sessionId: String

    /** Идентификатор устройства, для которого запущен scrcpy. */
    val deviceId: String

    /** `true`, если процесс всё ещё жив. */
    fun isAlive(): Boolean

    /**
     * Ожидать завершения процесса и получить итоговый [ScrcpyExitResult].
     *
     * Повторные вызовы возвращают тот же результат.
     */
    suspend fun awaitExit(): ScrcpyExitResult

    /**
     * Остановить процесс (graceful stop, затем force kill при таймауте).
     *
     * @param gracefulTimeoutMs Время ожидания мягкой остановки до `destroyForcibly`.
     */
    suspend fun stop(gracefulTimeoutMs: Long = 1_500L): Result<Unit>
}
