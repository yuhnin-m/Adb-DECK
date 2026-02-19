package com.adbdeck.core.adb.api.adb

import com.adbdeck.core.adb.api.device.AdbDevice

/**
 * Контракт ADB-клиента.
 *
 * Описывает все операции взаимодействия с Android Debug Bridge.
 * UI и компоненты работают только с этим интерфейсом — реализация
 * подменяема и живет в модуле core:adb-impl.
 */
interface AdbClient {

    /**
     * Проверяет доступность adb по пути из настроек или системного PATH.
     * Если передан [adbPathOverride], использует его без изменения сохраненных настроек.
     *
     * @param adbPathOverride Временный путь к adb для текущей проверки.
     * @return [AdbCheckResult.Available] с версией, либо [AdbCheckResult.NotAvailable].
     */
    suspend fun checkAvailability(adbPathOverride: String? = null): AdbCheckResult

    /**
     * Возвращает статус ADB server (`adb server-status`, если поддерживается версией adb).
     *
     * @param adbPathOverride Временный путь к adb для текущей операции.
     * @return Типизированный статус и диагностическое сообщение.
     */
    suspend fun getServerStatus(adbPathOverride: String? = null): AdbServerStatusResult

    /**
     * Запускает ADB server (`adb start-server`).
     *
     * @param adbPathOverride Временный путь к adb для текущей операции.
     * @return [Result.success] с текстом вывода команды, либо [Result.failure] с причиной.
     */
    suspend fun startServer(adbPathOverride: String? = null): Result<String>

    /**
     * Останавливает ADB server (`adb kill-server`).
     *
     * @param adbPathOverride Временный путь к adb для текущей операции.
     * @return [Result.success] с текстом вывода команды, либо [Result.failure] с причиной.
     */
    suspend fun stopServer(adbPathOverride: String? = null): Result<String>

    /**
     * Перезапускает ADB server (последовательно `kill-server` + `start-server`).
     *
     * @param adbPathOverride Временный путь к adb для текущей операции.
     * @return [Result.success] с текстом вывода, либо [Result.failure] с причиной.
     */
    suspend fun restartServer(adbPathOverride: String? = null): Result<String>

    /**
     * Возвращает список подключенных устройств через `adb devices`.
     *
     * @return [Result.success] со списком [AdbDevice], либо [Result.failure] с причиной.
     */
    suspend fun getDevices(): Result<List<AdbDevice>>
}
