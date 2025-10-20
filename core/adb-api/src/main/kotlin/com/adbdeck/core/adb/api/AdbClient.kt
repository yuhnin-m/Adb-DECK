package com.adbdeck.core.adb.api

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
     * Возвращает список подключенных устройств через `adb devices`.
     *
     * @return [Result.success] со списком [AdbDevice], либо [Result.failure] с причиной.
     */
    suspend fun getDevices(): Result<List<AdbDevice>>
}
