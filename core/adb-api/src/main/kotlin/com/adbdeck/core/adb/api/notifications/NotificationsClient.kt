package com.adbdeck.core.adb.api.notifications

/**
 * Клиент для получения уведомлений Android-устройства через ADB.
 *
 * Источник данных: `adb shell dumpsys notification`.
 * Реализация: [com.adbdeck.core.adb.impl.SystemNotificationsClient].
 */
interface NotificationsClient {

    /**
     * Получить список текущих уведомлений с устройства.
     *
     * @param deviceId Идентификатор ADB-устройства (серийный номер или IP:port).
     * @param adbPath  Путь к исполняемому файлу adb (по умолчанию "adb").
     * @return [Result] со списком записей или с ошибкой выполнения команды.
     */
    suspend fun getNotifications(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<List<NotificationRecord>>
}
