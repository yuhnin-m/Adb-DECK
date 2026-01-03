package com.adbdeck.feature.deviceinfo

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана Device Info.
 */
interface DeviceInfoComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<DeviceInfoState>

    // ── Загрузка ─────────────────────────────────────────────────────────────

    /**
     * Выполнить ручное обновление всех секций.
     *
     * Обновление запускает независимые ADB-запросы по секциям.
     */
    fun onRefresh()

    // ── Экспорт ───────────────────────────────────────────────────────────────

    /**
     * Экспортировать текущие данные секций в JSON-файл.
     *
     * @param path Абсолютный путь к файлу на хосте.
     */
    fun onExportJson(path: String)

    // ── Feedback ─────────────────────────────────────────────────────────────

    /** Скрыть текущее feedback-сообщение. */
    fun onDismissFeedback()
}
