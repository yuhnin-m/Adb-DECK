package com.adbdeck.feature.systemmonitor.storage

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента вкладки «Storage» в System Monitor.
 *
 * Отображает информацию о разделах файловой системы Android-устройства.
 * Данные получаются однократно при смене устройства и обновляются по запросу пользователя.
 */
interface StorageComponent {

    /** Реактивное состояние вкладки. */
    val state: StateFlow<StorageState>

    /**
     * Обновить информацию о хранилище вручную.
     *
     * Перезапрашивает `df` через ADB. При отсутствии активного устройства — нет-оп.
     */
    fun onRefresh()
}
