package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.feature.contacts.models.ContactsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Публичный интерфейс компонента "Контакты".
 *
 * Представляет список контактов Android-устройства, полученный через ADB.
 * Поддерживает создание, удаление, поиск, а также импорт/экспорт в форматах
 * JSON и vCard (VCF).
 *
 * Все файловые пути передаются из UI-слоя (через JFileChooser), компонент
 * не открывает диалоги самостоятельно.
 */
interface ContactsComponent {

    /** Поток состояния экрана. */
    val state: StateFlow<ContactsState>

    // ── Список ──────────────────────────────────────────────────────────────

    /** Перезагрузить список контактов с устройства. */
    fun onRefresh()

    /** Обновить поисковый запрос и отфильтровать список. */
    fun onSearchChanged(query: String)

    /** Выбрать аккаунт по умолчанию для создания/импорта контактов. */
    fun onSelectTargetAccount(account: ContactAccount)

    // ── Детали ──────────────────────────────────────────────────────────────

    /** Выбрать контакт и загрузить его детальную информацию. */
    fun onSelectContact(contact: Contact)

    /** Закрыть панель деталей (снять выбор). */
    fun onCloseDetail()

    /** Обновить детальную информацию текущего выбранного контакта. */
    fun onRefreshDetail()

    // ── Форма контакта (создание/редактирование) ────────────────────────────

    /** Открыть диалог добавления нового контакта. */
    fun onShowAddForm()

    /** Открыть диалог редактирования выбранного контакта (из панели деталей). */
    fun onShowEditForm()

    /** Изменить поле "Имя" в форме добавления. */
    fun onAddFormFirstNameChanged(value: String)

    /** Изменить поле "Фамилия" в форме добавления. */
    fun onAddFormLastNameChanged(value: String)

    /** Изменить аккаунт в форме добавления. */
    fun onAddFormAccountChanged(account: ContactAccount)

    /** Изменить первый телефон в форме добавления. */
    fun onAddFormPhone1Changed(value: String)

    /** Изменить тип первого телефона в форме добавления. */
    fun onAddFormPhone1TypeChanged(type: PhoneType)

    /** Изменить второй телефон в форме добавления. */
    fun onAddFormPhone2Changed(value: String)

    /** Изменить тип второго телефона в форме добавления. */
    fun onAddFormPhone2TypeChanged(type: PhoneType)

    /** Изменить email в форме добавления. */
    fun onAddFormEmailChanged(value: String)

    /** Изменить тип email в форме добавления. */
    fun onAddFormEmailTypeChanged(type: EmailType)

    /** Изменить организацию в форме добавления. */
    fun onAddFormOrganizationChanged(value: String)

    /** Изменить заметки в форме добавления. */
    fun onAddFormNotesChanged(value: String)

    /** Отправить форму — создать или обновить контакт на устройстве. */
    fun onSubmitAddForm()

    /** Закрыть диалог добавления без сохранения. */
    fun onDismissAddForm()

    // ── Удаление ────────────────────────────────────────────────────────────

    /** Запросить удаление контакта (открывает диалог подтверждения). */
    fun onRequestDelete(contact: Contact)

    /** Подтвердить удаление текущего `pendingDeleteContact`. */
    fun onConfirmDelete()

    /** Отменить удаление (закрыть диалог подтверждения). */
    fun onCancelDelete()

    // ── Экспорт ──────────────────────────────────────────────────────────────

    /**
     * Экспортировать все контакты в JSON-файл.
     * @param path Абсолютный путь к файлу назначения (выбирается через JFileChooser в UI).
     */
    fun onExportAllToJson(path: String)

    /**
     * Экспортировать все контакты в VCF-файл.
     * @param path Абсолютный путь к файлу назначения.
     */
    fun onExportAllToVcf(path: String)

    /**
     * Экспортировать выбранный контакт в JSON-файл.
     * @param contact Контакт для экспорта.
     * @param path    Абсолютный путь к файлу назначения.
     */
    fun onExportContactToJson(contact: Contact, path: String)

    /**
     * Экспортировать выбранный контакт в VCF-файл.
     * @param contact Контакт для экспорта.
     * @param path    Абсолютный путь к файлу назначения.
     */
    fun onExportContactToVcf(contact: Contact, path: String)

    // ── Импорт ───────────────────────────────────────────────────────────────

    /**
     * Импортировать контакты из JSON-файла.
     * @param path Абсолютный путь к JSON-файлу.
     */
    fun onImportFromJson(path: String)

    /**
     * Импортировать контакты из VCF-файла.
     * @param path Абсолютный путь к VCF-файлу.
     */
    fun onImportFromVcf(path: String)

    // ── Длительные операции ─────────────────────────────────────────────────

    /** Отменить текущую длительную операцию импорта/экспорта. */
    fun onCancelOperation()

    // ── Feedback ─────────────────────────────────────────────────────────────

    /** Скрыть предупреждающий баннер про ограничения ADB. */
    fun onDismissSafetyBanner()

    /** Скрыть текущее feedback-уведомление. */
    fun onDismissFeedback()
}
