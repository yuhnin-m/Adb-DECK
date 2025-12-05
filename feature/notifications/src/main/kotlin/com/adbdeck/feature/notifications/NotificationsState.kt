package com.adbdeck.feature.notifications

import com.adbdeck.core.adb.api.notifications.NotificationRecord

// ──────────────────────────────────────────────────────────────────────────────
// Перечисления
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Вкладка правой панели деталей уведомления.
 */
enum class NotificationsTab {
    /** Распарсенные поля уведомления с действиями. */
    DETAILS,

    /** Исходный блок из `dumpsys notification`. */
    RAW_DUMP,

    /** Сохранённые пользователем уведомления. */
    SAVED,
}

/**
 * Фильтр источника записей в основном списке.
 */
enum class NotificationsFilter {
    /** Все записи: текущие + исторические (без дублей). */
    ALL,

    /** Только уведомления, которые сейчас активны на устройстве. */
    CURRENT,

    /** Уведомления из runtime-истории, которые уже исчезли с устройства. */
    HISTORICAL,

    /** Только записи, сохранённые пользователем в локальную коллекцию. */
    SAVED,
}

/**
 * Порядок сортировки списка уведомлений.
 */
enum class NotificationsSortOrder {
    /** Сначала самые новые (по времени публикации). */
    NEWEST_FIRST,

    /** Сначала самые старые. */
    OLDEST_FIRST,

    /** Алфавитно по имени пакета. */
    BY_PACKAGE,
}

// ──────────────────────────────────────────────────────────────────────────────
// Состояние загрузки списка
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Состояние загрузки списка уведомлений.
 */
sealed interface NotificationsListState {
    /** Активное устройство не выбрано. */
    data object NoDevice : NotificationsListState

    /** Идёт загрузка. */
    data object Loading : NotificationsListState

    /** Уведомления успешно получены. */
    data class Success(val items: List<NotificationRecord>) : NotificationsListState

    /** Ошибка получения данных. */
    data class Error(val message: String) : NotificationsListState
}

// ──────────────────────────────────────────────────────────────────────────────
// Доменные модели
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Уведомление, сохранённое пользователем в локальную коллекцию.
 *
 * @param id      UUID-идентификатор записи.
 * @param savedAt Время сохранения в миллисекундах.
 * @param record  Оригинальная запись уведомления.
 * @param note    Опциональная заметка пользователя.
 */
data class SavedNotification(
    val id: String,
    val savedAt: Long,
    val record: NotificationRecord,
    val note: String = "",
)

/**
 * Временная обратная связь для пользователя (баннер внизу экрана).
 *
 * @param message  Текст сообщения.
 * @param isError  `true` — отобразить в цвете ошибки; `false` — успех.
 */
data class NotificationFeedback(
    val message: String,
    val isError: Boolean = false,
)

// ──────────────────────────────────────────────────────────────────────────────
// Корневое состояние экрана
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Полное состояние экрана Notifications.
 *
 * @param activeDeviceId            Серийный номер активного устройства или null.
 * @param deviceMessage             Строка-подсказка о статусе устройства.
 * @param listState                 Состояние загрузки основного списка.
 * @param currentNotifications      Уведомления, полученные при последнем refresh.
 * @param snapshotHistory           Runtime-история: ранее виденные уведомления (max 500).
 *                                  Не персистируется — сбрасывается при перезапуске приложения.
 * @param savedNotifications        Уведомления, сохранённые пользователем (max 200, персистентны).
 * @param filter                    Текущий фильтр источника записей.
 * @param sortOrder                 Текущий порядок сортировки.
 * @param searchQuery               Строка полнотекстового поиска.
 * @param packageFilter             Фильтр по имени пакета (частичное совпадение).
 * @param displayedNotifications    Итоговый список для отображения (вычисляется в компоненте).
 * @param selectedKey               Ключ выбранного уведомления или null.
 * @param selectedRecord            Выбранная запись уведомления или null.
 * @param selectedTab               Активная вкладка панели деталей.
 * @param isRefreshing              `true` во время выполнения refresh.
 * @param feedback                  Временное сообщение обратной связи или null.
 */
data class NotificationsState(
    val activeDeviceId: String? = null,
    val deviceMessage: String = "Активное устройство не выбрано",

    val listState: NotificationsListState = NotificationsListState.NoDevice,
    val currentNotifications: List<NotificationRecord> = emptyList(),
    val snapshotHistory: List<NotificationRecord> = emptyList(),
    val savedNotifications: List<SavedNotification> = emptyList(),

    val filter: NotificationsFilter = NotificationsFilter.ALL,
    val sortOrder: NotificationsSortOrder = NotificationsSortOrder.NEWEST_FIRST,
    val searchQuery: String = "",
    val packageFilter: String = "",

    val displayedNotifications: List<NotificationRecord> = emptyList(),

    val selectedKey: String? = null,
    val selectedRecord: NotificationRecord? = null,
    val selectedTab: NotificationsTab = NotificationsTab.DETAILS,

    val isRefreshing: Boolean = false,
    val feedback: NotificationFeedback? = null,
)
