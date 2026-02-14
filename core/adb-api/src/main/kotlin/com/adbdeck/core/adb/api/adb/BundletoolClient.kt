package com.adbdeck.core.adb.api.adb

/**
 * Контракт клиента для проверки bundletool.
 *
 * UI и feature-слой работают только с интерфейсом из `core:adb-api`,
 * а реализация живет в `core:adb-impl`.
 */
interface BundletoolClient {

    /**
     * Проверяет доступность bundletool по пути из настроек или системного PATH.
     * Если передан [bundletoolPathOverride], использует его без изменения сохраненных настроек.
     *
     * @param bundletoolPathOverride Временный путь к bundletool (исполняемому файлу или `.jar`).
     * @return [BundletoolCheckResult.Available] с версией, либо [BundletoolCheckResult.NotAvailable].
     */
    suspend fun checkAvailability(bundletoolPathOverride: String? = null): BundletoolCheckResult
}
