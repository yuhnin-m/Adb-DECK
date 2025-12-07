package com.adbdeck.core.adb.impl.device

import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.device.RebootMode
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [com.adbdeck.core.adb.api.device.DeviceControlClient] через системный adb-процесс.
 *
 * ## Выполняемые команды:
 * - [com.adbdeck.core.adb.api.device.RebootMode.NORMAL]     → `adb -s <id> reboot`
 * - [com.adbdeck.core.adb.api.device.RebootMode.RECOVERY]   → `adb -s <id> reboot recovery`
 * - [com.adbdeck.core.adb.api.device.RebootMode.BOOTLOADER] → `adb -s <id> reboot bootloader`
 *
 * ## Важно о поведении adb reboot:
 * Команда не возвращает вывода — устройство немедленно начинает перезагрузку,
 * соединение разрывается, а exitCode обычно = 0. При ошибках (не root / нет прав)
 * adb может вывести сообщение в stderr.
 *
 * @param processRunner Исполнитель внешних процессов.
 */
class SystemDeviceControlClient(
    private val processRunner: ProcessRunner,
) : DeviceControlClient {

    override suspend fun reboot(
        deviceId: String,
        mode: RebootMode,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        // Строим аргументы команды: adb -s <id> reboot [mode]
        val args = buildList {
            add(adbPath)
            addAll(listOf("-s", deviceId))
            add("reboot")
            when (mode) {
                RebootMode.NORMAL -> Unit               // без аргумента
                RebootMode.RECOVERY -> add("recovery")
                RebootMode.BOOTLOADER -> add("bootloader")
            }
        }

        val result = processRunner.run(args)

        // adb reboot не гарантирует вывод — проверяем только наличие ошибки в stderr
        val stderr = result.stderr.trim()
        if (stderr.isNotEmpty() && !result.isSuccess) {
            error("Ошибка перезагрузки [$deviceId] mode=$mode: $stderr")
        }
    }
}