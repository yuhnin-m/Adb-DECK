package com.adbdeck.core.process

import kotlinx.coroutines.flow.StateFlow

/**
 * Потокобезопасное in-memory хранилище истории команд.
 *
 * История живет только в RAM процесса и не сохраняется между запусками приложения.
 */
interface ProcessHistoryStore {

    /** Текущая история команд в порядке добавления (сначала старые, в конце новые). */
    val entries: StateFlow<List<ProcessHistoryEntry>>

    /** Добавить запись в историю. */
    fun append(entry: ProcessHistoryEntry)

    /** Очистить историю. */
    fun clear()
}
