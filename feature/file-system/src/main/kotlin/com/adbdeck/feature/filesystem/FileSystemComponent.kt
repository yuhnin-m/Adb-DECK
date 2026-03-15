package com.adbdeck.feature.filesystem

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана «File System».
 *
 * Отображает информацию о разделах файловой системы Android-устройства.
 * Данные получаются однократно при смене устройства и обновляются по запросу пользователя.
 */
interface FileSystemComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<FileSystemState>

    /**
     * Обновить информацию о хранилище вручную.
     *
     * Перезапрашивает `df` через ADB. При отсутствии активного устройства — нет-оп.
     */
    fun onRefresh()

    // ── Cleanup ────────────────────────────────────────────────────────────────

    /** Открыть child-окно очистки временных файлов. */
    fun onOpenCleanup()

    /** Закрыть child-окно очистки временных файлов. */
    fun onDismissCleanup()

    /** Переключить выбор опции очистки. */
    fun onToggleCleanupOption(option: CleanupOption)

    /**
     * Запросить запуск очистки.
     *
     * Если есть выбранные опции и устройство доступно — откроется confirm dialog.
     */
    fun onStartCleanup()

    /** Подтвердить запуск cleanup и начать последовательное выполнение команд. */
    fun onConfirmCleanup()

    /** Закрыть confirm dialog без запуска cleanup. */
    fun onDismissCleanupConfirm()

    /** Отменить выполняющуюся cleanup-задачу (cancel coroutine/job). */
    fun onCancelCleanup()

    /** Скопировать текущий cleanup-лог в системный буфер обмена. */
    fun onCopyCleanupLog()

    // ── Навигация ──────────────────────────────────────────────────────────────

    /** Открыть раздел в File Explorer по [path]. */
    fun onOpenPartition(path: String)
}
