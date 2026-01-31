package com.adbdeck.core.process

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory реализация [ProcessHistoryStore] с ограничением размера истории.
 *
 * Потокобезопасность обеспечивается синхронизацией по приватному lock-объекту.
 */
class InMemoryProcessHistoryStore(
    private val capacity: Int = DEFAULT_CAPACITY,
) : ProcessHistoryStore {

    private val lock = Any()
    private val _entries = MutableStateFlow<List<ProcessHistoryEntry>>(emptyList())

    override val entries: StateFlow<List<ProcessHistoryEntry>> = _entries.asStateFlow()

    override fun append(entry: ProcessHistoryEntry) {
        synchronized(lock) {
            val current = _entries.value
            val updated = when {
                capacity <= 0 -> emptyList()
                current.size < capacity -> current + entry
                else -> current.drop(1) + entry
            }
            _entries.value = updated
        }
    }

    override fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
        }
    }

    private companion object {
        const val DEFAULT_CAPACITY: Int = 500
    }
}
