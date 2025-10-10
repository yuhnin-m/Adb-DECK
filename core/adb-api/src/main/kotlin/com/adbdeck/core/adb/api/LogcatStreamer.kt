package com.adbdeck.core.adb.api

import kotlinx.coroutines.flow.Flow

/**
 * Контракт потокового чтения `adb logcat`.
 *
 * Возвращает холодный [Flow], который:
 * - Запускает `adb logcat -v threadtime` в фоновом процессе.
 * - Эмитит строки по мере поступления.
 * - Завершается, когда процесс завершается или коллектор отменяет подписку.
 * - Гарантирует уничтожение процесса при отмене.
 */
interface LogcatStreamer {

    /**
     * Открыть поток logcat для устройства [deviceId].
     *
     * @param deviceId Идентификатор устройства (serial или IP:port).
     * @param adbPath  Путь к исполняемому файлу adb.
     * @return [Flow] строк logcat (сырой вывод).
     */
    fun stream(deviceId: String, adbPath: String = "adb"): Flow<String>
}
