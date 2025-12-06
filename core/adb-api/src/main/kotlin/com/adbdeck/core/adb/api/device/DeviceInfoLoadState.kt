package com.adbdeck.core.adb.api.device

/**
 * Состояние загрузки [DeviceInfo] для одного устройства.
 *
 * Используется в [com.adbdeck.feature.devices.DevicesState.deviceInfos]
 * как значения Map<deviceId, DeviceInfoLoadState>.
 */
sealed class DeviceInfoLoadState {
    /** Информация загружается. */
    data object Loading : DeviceInfoLoadState()

    /** Информация успешно загружена. */
    data class Loaded(val info: DeviceInfo) : DeviceInfoLoadState()

    /** Не удалось загрузить информацию. */
    data class Failed(val message: String) : DeviceInfoLoadState()
}
