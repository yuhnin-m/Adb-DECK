package com.adbdeck.core.adb.api.screen

/**
 * Сессия активной записи экрана Android-устройства.
 *
 * @param sessionId Внутренний идентификатор сессии внутри реализации клиента.
 * @param deviceId  Идентификатор устройства, на котором запущена запись.
 * @param remotePath Временный путь видео-файла на устройстве.
 * @param remotePid PID процесса `screenrecord` на устройстве.
 * @param startedAtEpochMillis Момент запуска записи (epoch millis).
 */
data class ScreenrecordSession(
    val sessionId: String,
    val deviceId: String,
    val remotePath: String,
    val remotePid: String,
    val startedAtEpochMillis: Long,
)
