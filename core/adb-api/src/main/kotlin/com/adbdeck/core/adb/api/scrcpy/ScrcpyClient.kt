package com.adbdeck.core.adb.api.scrcpy

/**
 * Контракт клиента для операций scrcpy.
 */
interface ScrcpyClient {

    /**
     * Проверить доступность scrcpy через `scrcpy --version`.
     *
     * @param scrcpyPathOverride Временный путь к исполняемому файлу.
     */
    suspend fun checkAvailability(scrcpyPathOverride: String? = null): ScrcpyCheckResult

    /**
     * Запустить новую scrcpy-сессию.
     *
     * @param request Конфигурация запуска.
     * @param scrcpyPathOverride Временный путь к исполняемому файлу.
     */
    suspend fun startSession(
        request: ScrcpyLaunchRequest,
        scrcpyPathOverride: String? = null,
    ): Result<ScrcpySession>
}
