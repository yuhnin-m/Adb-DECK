package com.adbdeck.core.adb.api

/**
 * Одна запись уведомления, извлечённая из `adb shell dumpsys notification`.
 *
 * Все поля, кроме [key] и [packageName], могут быть null —
 * формат dumpsys варьируется между версиями Android и вендорами.
 *
 * @param key             Составной ключ вида "0|pkg|id|tag|uid" — уникальный идентификатор.
 * @param packageName     Пакет приложения-владельца уведомления.
 * @param notificationId  ID уведомления (int), как передаётся в NotificationManager.notify().
 * @param tag             Тег уведомления, может быть null.
 * @param importance      Уровень важности 0–5 (IMPORTANCE_UNSPECIFIED…IMPORTANCE_HIGH).
 * @param channelId       Идентификатор канала уведомлений (Android 8+).
 * @param title           Заголовок уведомления.
 * @param text            Основной текст уведомления.
 * @param subText         Подтекст под основным текстом.
 * @param bigText         Полный текст расширенного уведомления (BigTextStyle).
 * @param summaryText     Текст-сводка (InboxStyle / MessagingStyle).
 * @param category        Категория уведомления (например, "msg", "alarm", "call").
 * @param flags           Битовая маска флагов (FLAG_ONGOING_EVENT, FLAG_NO_CLEAR и др.).
 * @param isOngoing       Флаг постоянного уведомления ((flags & FLAG_ONGOING_EVENT) != 0).
 * @param isClearable     Может ли пользователь смахнуть уведомление.
 * @param postedAt        Время публикации в миллисекундах (поле `when`), null если не распарсилось.
 * @param group           Ключ группы уведомлений.
 * @param sortKey         Ключ сортировки внутри группы.
 * @param actionsCount    Количество действий в уведомлении, если удалось извлечь.
 * @param actionTitles    Подписи action-кнопок (best-effort парсинг из dumpsys).
 * @param imageParameters Визуальные параметры уведомления (иконки/картинки/template/contentView).
 * @param rawBlock        Исходный текстовый блок из dumpsys — всегда доступен как fallback.
 */
data class NotificationRecord(
    val key: String,
    val packageName: String,
    val notificationId: Int,
    val tag: String?,
    val importance: Int,
    val channelId: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val summaryText: String?,
    val category: String?,
    val flags: Int,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val postedAt: Long?,
    val group: String?,
    val sortKey: String?,
    val actionsCount: Int? = null,
    val actionTitles: List<String> = emptyList(),
    val imageParameters: Map<String, String> = emptyMap(),
    val rawBlock: String,
)

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
