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
}
