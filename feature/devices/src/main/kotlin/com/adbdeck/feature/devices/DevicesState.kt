package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.AdbDevice

/**
 * Состояние экрана списка устройств.
 *
 * Отражает все возможные фазы жизненного цикла данных на экране.
 */
sealed class DevicesState {

    /** Данные загружаются. */
    data object Loading : DevicesState()

    /** Данные загружены, список пуст. */
    data object Empty : DevicesState()

    /**
     * Данные загружены, список устройств непуст.
     *
     * @param devices Список подключенных устройств.
     */
    data class Success(val devices: List<AdbDevice>) : DevicesState()

    /**
     * Произошла ошибка при загрузке.
     *
     * @param message Описание ошибки для отображения пользователю.
     */
    data class Error(val message: String) : DevicesState()
}
