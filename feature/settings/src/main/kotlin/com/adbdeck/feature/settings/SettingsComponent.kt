package com.adbdeck.feature.settings

import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.AppLanguage
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

    /**
     * Пользователь изменил поле пути к bundletool.
     *
     * @param path Новое значение пути.
     */
    fun onBundletoolPathChanged(path: String)

    /** Сохранить текущие настройки. */
    fun onSave()

    /** Проверить доступность adb по текущему пути. */
    fun onCheckAdb()

    /** Автоматически найти ADB в PATH и стандартных SDK-путях. */
    fun onAutoDetectAdb()

    /** Применить путь ADB из списка кандидатов, найденных автодетектом. */
    fun onSelectAutoDetectedAdbPath(path: String)

    /** Закрыть список кандидатов автодетекта ADB. */
    fun onDismissAutoDetectedAdbCandidates()

    /** Проверить доступность bundletool по текущему пути и вывести версию. */
    fun onCheckBundletool()

    /**
     * Пользователь изменил поле пути к scrcpy.
     *
     * @param path Новое значение пути.
     */
    fun onScrcpyPathChanged(path: String)

    /** Проверить доступность scrcpy по текущему пути (запускает `scrcpy --version`). */
    fun onCheckScrcpy()

    /** Скрыть баннер обратной связи. */
    fun onDismissFeedback()

    /** Проверить наличие обновлений приложения. */
    fun onCheckForUpdates()

    /**
     * Пользователь выбрал другую тему.
     *
     * @param theme Новая тема.
     */
    fun onThemeChanged(theme: AppTheme)

    /**
     * Пользователь выбрал другой язык интерфейса.
     *
     * @param language Новый язык.
     */
    fun onLanguageChanged(language: AppLanguage)

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

    /**
     * Изменить шрифтовое семейство строк logcat.
     *
     * @param family Имя [com.adbdeck.feature.logcat.LogcatFontFamily] (строковое представление).
     */
    fun onLogcatFontFamilyChanged(family: String)

    /**
     * Изменить размер шрифта строк logcat.
     *
     * @param size Размер в sp, допустимый диапазон 8–24.
     */
    fun onLogcatFontSizeChanged(size: Int)
}
