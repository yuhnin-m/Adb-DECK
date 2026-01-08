package com.adbdeck.feature.quicktoggles

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт экрана Quick Toggles.
 */
interface QuickTogglesComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<QuickTogglesState>

    // ── Загрузка ─────────────────────────────────────────────────────────────

    /** Ручное обновление статусов всех toggle-элементов. */
    fun onRefresh()

    /**
     * Ручное обновление статуса одного toggle-элемента.
     *
     * Нужен для локального refresh в отдельных блоках (например Animations).
     */
    fun onRefreshToggle(toggleId: QuickToggleId)

    // ── Переключение ─────────────────────────────────────────────────────────

    /**
     * Запросить изменение состояния toggle.
     *
     * Для потенциально опасных toggle-ов может потребоваться подтверждение.
     *
     * @param toggleId Идентификатор toggle.
     * @param targetState Целевое состояние (`ON/OFF`).
     */
    fun onRequestToggle(toggleId: QuickToggleId, targetState: QuickToggleState)

    /**
     * Обновить draft-значение одного параметра Animations.
     */
    fun onAnimationDraftChanged(key: String, value: Float)

    /**
     * Применить (settings put) значение одного параметра Animations и выполнить read-back.
     */
    fun onSetAnimationScale(key: String)

    /** Подтвердить ожидающее действие [QuickTogglesState.pendingAction]. */
    fun onConfirmToggle()

    /** Отменить ожидающее действие [QuickTogglesState.pendingAction]. */
    fun onCancelToggle()

    // ── Fallback ─────────────────────────────────────────────────────────────

    /**
     * Открыть системные настройки устройства для конкретного toggle.
     *
     * Используется как fallback, если shell-toggle не сработал.
     */
    fun onOpenSettings(toggleId: QuickToggleId)

    // ── Feedback ─────────────────────────────────────────────────────────────

    /** Скрыть текущее feedback-сообщение. */
    fun onDismissFeedback()
}
