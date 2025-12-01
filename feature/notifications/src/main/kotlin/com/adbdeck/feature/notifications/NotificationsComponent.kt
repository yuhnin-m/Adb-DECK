package com.adbdeck.feature.notifications

import com.adbdeck.core.adb.api.NotificationRecord
import kotlinx.coroutines.flow.StateFlow

/**
 * Публичный интерфейс компонента Notifications.
 *
 * Управляет получением уведомлений с устройства, фильтрацией, деталями
 * и сохранением записей в локальную коллекцию.
 * Реализован в [DefaultNotificationsComponent].
 */
interface NotificationsComponent {

    /** Наблюдаемое состояние экрана. */
    val state: StateFlow<NotificationsState>

    // ── Загрузка ──────────────────────────────────────────────────────────────

    /**
     * Выполнить `adb shell dumpsys notification` и обновить список.
     * Вызывается при открытии экрана, смене устройства или нажатии кнопки Refresh.
     */
    fun onRefresh()

    // ── Фильтрация и поиск ───────────────────────────────────────────────────

    /**
     * Обновить строку полнотекстового поиска (ищет в packageName, title, text).
     */
    fun onSearchChanged(query: String)

    /**
     * Обновить фильтр по имени пакета (частичное совпадение, регистронезависимо).
     */
    fun onPackageFilterChanged(pkg: String)

    /**
     * Переключить фильтр источника записей (ALL / CURRENT / HISTORICAL / SAVED).
     */
    fun onFilterChanged(filter: NotificationsFilter)

    /**
     * Переключить порядок сортировки.
     */
    fun onSortOrderChanged(order: NotificationsSortOrder)

    // ── Выбор и детали ───────────────────────────────────────────────────────

    /**
     * Выбрать уведомление для просмотра деталей.
     */
    fun onSelectNotification(record: NotificationRecord)

    /**
     * Закрыть панель деталей.
     */
    fun onCloseDetail()

    /**
     * Переключить вкладку в панели деталей.
     */
    fun onSelectTab(tab: NotificationsTab)

    // ── Действия ─────────────────────────────────────────────────────────────

    /**
     * Скопировать имя пакета в системный буфер обмена.
     */
    fun onCopyPackageName(record: NotificationRecord)

    /**
     * Скопировать заголовок уведомления в буфер обмена.
     */
    fun onCopyTitle(record: NotificationRecord)

    /**
     * Скопировать текст уведомления в буфер обмена.
     */
    fun onCopyText(record: NotificationRecord)

    /**
     * Скопировать исходный блок дампа в буфер обмена.
     */
    fun onCopyRawDump(record: NotificationRecord)

    /**
     * Сохранить уведомление в локальную коллекцию (персистентно).
     * Если уведомление с таким key уже сохранено — игнорируется.
     */
    fun onSaveNotification(record: NotificationRecord)

    /**
     * Удалить сохранённое уведомление по его UUID [id].
     */
    fun onDeleteSaved(id: String)

    /**
     * Экспортировать уведомление в JSON-файл по указанному пути.
     *
     * @param record Запись уведомления для экспорта.
     * @param path   Путь к файлу на хосте (выбирается через JFileChooser в UI).
     */
    fun onExportToJson(record: NotificationRecord, path: String)

    // ── Интеграция ───────────────────────────────────────────────────────────

    /**
     * Перейти на экран Packages и выделить пакет с именем [packageName].
     */
    fun onOpenInPackages(packageName: String)

    /**
     * Скопировать [uri] в буфер обмена и перейти на экран Deep Links.
     */
    fun onOpenInDeepLinks(uri: String)

    // ── Обратная связь ───────────────────────────────────────────────────────

    /**
     * Скрыть временный баннер обратной связи.
     */
    fun onDismissFeedback()
}
