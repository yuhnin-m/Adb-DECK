package com.adbdeck.feature.settings

import com.adbdeck.core.settings.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана настроек.
 */
interface SettingsComponent {

    /** Текущее состояние UI экрана настроек. */
    val state: StateFlow<SettingsUiState>

    /**
     * Пользователь изменил поле пути к adb.
     *
     * @param path Новое значение пути.
     */
    fun onAdbPathChanged(path: String)

    /** Сохранить текущие настройки. */
    fun onSave()

    /** Проверить доступность adb по текущему пути. */
    fun onCheckAdb()

    /**
     * Пользователь выбрал другую тему.
     *
     * @param theme Новая тема.
     */
    fun onThemeChanged(theme: AppTheme)

    // ── Logcat settings (сохраняются немедленно) ────────────────

    /** Переключить режим отображения logcat (compact / full). */
    fun onLogcatCompactModeChanged(value: Boolean)

    /** Переключить отображение даты в строке logcat. */
    fun onLogcatShowDateChanged(value: Boolean)

    /** Переключить отображение времени в строке logcat. */
    fun onLogcatShowTimeChanged(value: Boolean)

    /** Переключить отображение миллисекунд в строке logcat. */
    fun onLogcatShowMillisChanged(value: Boolean)

    /** Переключить цветовую подсветку уровней logcat. */
    fun onLogcatColoredLevelsChanged(value: Boolean)

    /**
     * Изменить максимальный размер буфера logcat.
     *
     * @param lines Количество строк (минимум 100).
     */
    fun onLogcatMaxBufferedLinesChanged(lines: Int)

    /** Переключить автоскролл по умолчанию для logcat. */
    fun onLogcatAutoScrollChanged(value: Boolean)
}
