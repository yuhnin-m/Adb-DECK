package com.adbdeck.feature.notifications.storage

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.feature.notifications.SavedNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// Сериализуемые модели хранилища
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Сериализуемая модель одного сохранённого уведомления.
 * Поле rawBlock не сохраняется — оно слишком объёмно и содержит контекстную информацию,
 * которая устаревает. При восстановлении rawBlock заполняется пустой строкой.
 */
@Serializable
internal data class StorageSavedNotification(
    val id: String,
    val savedAt: Long,
    val note: String = "",
    // Поля NotificationRecord
    val key: String,
    val packageName: String,
    val notificationId: Int,
    val tag: String? = null,
    val importance: Int = 0,
    val channelId: String? = null,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
    val summaryText: String? = null,
    val category: String? = null,
    val flags: Int = 0,
    val isOngoing: Boolean = false,
    val isClearable: Boolean = true,
    val postedAt: Long? = null,
    val group: String? = null,
    val sortKey: String? = null,
    val actionsCount: Int? = null,
    val actionTitles: List<String> = emptyList(),
    val imageParameters: Map<String, String> = emptyMap(),
)

@Serializable
internal data class NotificationsStorageData(
    val saved: List<StorageSavedNotification> = emptyList(),
)

// ──────────────────────────────────────────────────────────────────────────────
// Сервис хранилища
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Персистентное хранилище сохранённых пользователем уведомлений.
 *
 * Данные записываются в `~/.adbdeck/notifications.json` в формате JSON.
 * Все операции выполняются в [Dispatchers.IO].
 */
class NotificationsStorage {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val storageFile = File(System.getProperty("user.home"), ".adbdeck/notifications.json")

    /**
     * Загрузить список сохранённых уведомлений из файла.
     * При отсутствии файла возвращает пустой список.
     */
    suspend fun load(): Result<List<SavedNotification>> = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) return@withContext Result.success(emptyList())
        runCatchingPreserveCancellation {
            val data = json.decodeFromString<NotificationsStorageData>(storageFile.readText())
            data.saved.map { it.toDomain() }
        }
    }

    /**
     * Сохранить список уведомлений в файл.
     *
     * @param saved Актуальный список сохранённых уведомлений.
     */
    suspend fun save(saved: List<SavedNotification>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingPreserveCancellation {
            storageFile.parentFile?.mkdirs()
            val data = NotificationsStorageData(saved = saved.map { it.toStorage() })
            storageFile.writeText(json.encodeToString(data))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Конверторы: domain ↔ storage
// ──────────────────────────────────────────────────────────────────────────────

private fun SavedNotification.toStorage() = StorageSavedNotification(
    id             = id,
    savedAt        = savedAt,
    note           = note,
    key            = record.key,
    packageName    = record.packageName,
    notificationId = record.notificationId,
    tag            = record.tag,
    importance     = record.importance,
    channelId      = record.channelId,
    title          = record.title,
    text           = record.text,
    subText        = record.subText,
    bigText        = record.bigText,
    summaryText    = record.summaryText,
    category       = record.category,
    flags          = record.flags,
    isOngoing      = record.isOngoing,
    isClearable    = record.isClearable,
    postedAt       = record.postedAt,
    group          = record.group,
    sortKey        = record.sortKey,
    actionsCount   = record.actionsCount,
    actionTitles   = record.actionTitles,
    imageParameters = record.imageParameters,
)

private fun StorageSavedNotification.toDomain() = SavedNotification(
    id      = id,
    savedAt = savedAt,
    note    = note,
    record  = NotificationRecord(
        key            = key,
        packageName    = packageName,
        notificationId = notificationId,
        tag            = tag,
        importance     = importance,
        channelId      = channelId,
        title          = title,
        text           = text,
        subText        = subText,
        bigText        = bigText,
        summaryText    = summaryText,
        category       = category,
        flags          = flags,
        isOngoing      = isOngoing,
        isClearable    = isClearable,
        postedAt       = postedAt,
        group          = group,
        sortKey        = sortKey,
        actionsCount   = actionsCount,
        actionTitles   = actionTitles,
        imageParameters = imageParameters,
        rawBlock       = "", // rawBlock не персистируется
    ),
)
