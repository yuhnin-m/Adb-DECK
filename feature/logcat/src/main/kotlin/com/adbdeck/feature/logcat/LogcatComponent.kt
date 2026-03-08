package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.logcat.LogcatLevel
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана Logcat.
 */
interface LogcatComponent {

    /** Текущее полное состояние экрана. */
    val state: StateFlow<LogcatState>

    // ── Stream control ─────────────────────────────────────────

    /** Запустить захват logcat для текущего активного устройства из DeviceManager. */
    fun onStart()

    /** Остановить захват и освободить процесс. */
    fun onStop()

    /** Очистить буфер вывода (данные удаляются, поток не останавливается). */
    fun onClear()

    // ── Import / export ─────────────────────────────────────────

    /**
     * Импортировать лог-файл и переключить экран в режим просмотра файла.
     *
     * В этом режиме live-стрим остановлен, новые строки из adb не добавляются.
     */
    fun onImportFromFile(path: String)

    /**
     * Закрыть режим просмотра файла и вернуться в обычный LIVE-режим экрана.
     */
    fun onCloseImportedFile()

    /**
     * Экспортировать текущий список логов в текстовый файл logcat.
     *
     * Источник экспорта — текущий `state.entries`.
     */
    fun onExportToFile(path: String)

    // ── Filters ────────────────────────────────────────────────

    /** Изменить текстовый поиск (ищет в tag + message). */
    fun onSearchChanged(query: String)

    /** Изменить фильтр по тегу (case-insensitive substring). */
    fun onTagFilterChanged(tag: String)

    /** Изменить фильтр по пакету / процессу (через PID сопоставление процессов). */
    fun onPackageFilterChanged(pkg: String)

    /** Установить минимальный уровень лога (`null` = все уровни). */
    fun onLevelFilterChanged(level: LogcatLevel?)

    // ── Display toggles ────────────────────────────────────────

    /** Переключить режим отображения (COMPACT ↔ FULL). */
    fun onDisplayModeChanged(mode: LogcatDisplayMode)

    /** Переключить отображение даты. */
    fun onToggleShowDate()

    /** Переключить отображение времени. */
    fun onToggleShowTime()

    /** Переключить отображение миллисекунд. */
    fun onToggleShowMillis()

    /** Переключить цветовую подсветку уровней. */
    fun onToggleColoredLevels()

    /**
     * Изменить режим автоскролла.
     * Вызывается из UI при детектировании прокрутки пользователем.
     */
    fun onAutoScrollChanged(value: Boolean)

    /**
     * Включить/выключить плавную по-кадровую публикацию новых строк.
     *
     * `true` — "живой" поток с небольшим интервалом между публикациями.
     * `false` — публикация без искусственной задержки.
     */
    fun onSmoothStreamAnimationChanged(value: Boolean)

    // ── Font settings ──────────────────────────────────────────

    /** Изменить шрифтовое семейство строк лога. */
    fun onFontFamilyChanged(family: LogcatFontFamily)

    /**
     * Изменить размер шрифта строк лога.
     *
     * @param size Размер в sp, допустимый диапазон 8–24.
     */
    fun onFontSizeChanged(size: Int)
}
