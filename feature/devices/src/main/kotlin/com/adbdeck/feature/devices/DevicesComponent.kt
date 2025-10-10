package com.adbdeck.feature.devices

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана устройств.
 */
interface DevicesComponent {

    /** Текущее состояние экрана. */
    val state: StateFlow<DevicesState>

    /** Обновить список устройств через `adb devices`. */
    fun onRefresh()
}
