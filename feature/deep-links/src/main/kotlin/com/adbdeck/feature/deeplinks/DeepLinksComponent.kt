package com.adbdeck.feature.deeplinks

import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.LaunchMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Публичный интерфейс компонента Deep Links / Intents.
 *
 * Управляет формой запуска, историей и шаблонами.
 * Реализован в [DefaultDeepLinksComponent].
 */
interface DeepLinksComponent {

    /** Наблюдаемое состояние экрана. */
    val state: StateFlow<DeepLinksState>

    // ── Режим ────────────────────────────────────────────────────────────────

    /** Переключить режим [LaunchMode.DEEP_LINK] / [LaunchMode.INTENT]. */
    fun onModeChanged(mode: LaunchMode)

    // ── Форма Deep Link ───────────────────────────────────────────────────────

    fun onDlUriChanged(value: String)
    fun onDlActionChanged(value: String)
    fun onDlPackageChanged(value: String)
    fun onDlComponentChanged(value: String)
    fun onDlCategoryChanged(value: String)

    // ── Форма Intent ──────────────────────────────────────────────────────────

    fun onItActionChanged(value: String)
    fun onItDataUriChanged(value: String)
    fun onItPackageChanged(value: String)
    fun onItComponentChanged(value: String)
    fun onItCategoryAdd(category: String)
    fun onItCategoryRemove(index: Int)
    fun onItFlagsChanged(value: String)
    fun onItExtraAdd()
    fun onItExtraRemove(index: Int)
    fun onItExtraKeyChanged(index: Int, key: String)
    fun onItExtraTypeChanged(index: Int, type: ExtraType)
    fun onItExtraValueChanged(index: Int, value: String)

    // ── Запуск ───────────────────────────────────────────────────────────────

    /** Выполнить запуск (deep link или intent) на активном устройстве. */
    fun onLaunch()

    // ── Правая панель ────────────────────────────────────────────────────────

    /** Переключить вкладку правой панели. */
    fun onRightTabChanged(tab: DeepLinksTab)

    // ── История ───────────────────────────────────────────────────────────────

    /** Восстановить параметры формы из записи истории. */
    fun onRestoreFromHistory(entry: LaunchHistoryEntry)

    /** Удалить запись из истории по ID. */
    fun onDeleteHistoryEntry(id: String)

    /** Очистить всю историю запусков. */
    fun onClearHistory()

    // ── Шаблоны ───────────────────────────────────────────────────────────────

    /** Открыть диалог сохранения текущей конфигурации как шаблона. */
    fun onShowSaveTemplateDialog()

    /** Обновить имя нового шаблона в диалоге. */
    fun onSaveTemplateNameChanged(name: String)

    /** Подтвердить сохранение шаблона. */
    fun onConfirmSaveTemplate()

    /** Закрыть диалог сохранения шаблона без сохранения. */
    fun onDismissSaveTemplateDialog()

    /** Запустить шаблон (восстановить параметры + выполнить). */
    fun onLaunchTemplate(template: IntentTemplate)

    /** Восстановить параметры формы из шаблона (без запуска). */
    fun onRestoreFromTemplate(template: IntentTemplate)

    /** Удалить шаблон по ID. */
    fun onDeleteTemplate(id: String)

    /**
     * Предзаполнить поле URI в форме Deep Link.
     * Используется при переходе из экрана Notifications через кнопку "Open in Deep Links".
     *
     * @param uri URI для подстановки в поле Deep Link URI.
     */
    fun prefillDeepLinkUri(uri: String)
}
