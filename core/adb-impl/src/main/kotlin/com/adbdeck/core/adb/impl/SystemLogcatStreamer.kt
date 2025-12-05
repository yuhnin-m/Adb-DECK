package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Реализация [LogcatStreamer] через системный `adb logcat`.
 *
 * Запускает `adb -s <deviceId> logcat -v threadtime` как отдельный OS-процесс.
 * Строки читаются на [Dispatchers.IO] и передаются в поток через Channel.
 *
 * Безопасность ресурсов:
 * - `awaitClose` гарантирует уничтожение процесса при отмене коллектора.
 * - `destroyForcibly()` вызывается как в нормальном завершении, так и при отмене.
 * - `trySend` используется вместо `send` для предотвращения блокировки IO-потока.
 */
class SystemLogcatStreamer : LogcatStreamer {

    override fun stream(deviceId: String, adbPath: String): Flow<String> = callbackFlow {
        val command = listOf(adbPath, "-s", deviceId, "logcat", "-v", "threadtime")
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        // Читаем stdout на IO — блокирующий I/O, поэтому отдельная корутина
        val readJob = launch(Dispatchers.IO) {
            try {
                process.inputStream.bufferedReader().use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break   // процесс завершился
                        trySend(line)                            // non-blocking, drop если переполнено
                    }
                }
            } catch (_: Exception) {
                // Поток закрыт — либо процесс завершился, либо получили interrupt
            } finally {
                process.destroyForcibly()
            }
            close() // сигнализируем callbackFlow о завершении
        }

        // stderr также нужно читать, иначе adb-процесс может блокироваться при переполнении буфера.
        val stderrJob = launch(Dispatchers.IO) {
            try {
                process.errorStream.bufferedReader().use { reader ->
                    while (isActive) {
                        if (reader.readLine() == null) break
                    }
                }
            } catch (_: Exception) {
                // Игнорируем: поток ошибок закрывается при завершении процесса/отмене.
            }
        }

        // Вызывается при отмене коллектора
        awaitClose {
            readJob.cancel()
            stderrJob.cancel()
            process.destroyForcibly()
        }
    }
        .flowOn(Dispatchers.IO)
}
