package com.adbdeck.feature.deviceinfo.service

import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind

/**
 * Сервис сбора секций экрана Device Info.
 *
 * UI-слой работает только с этим интерфейсом и не знает о конкретных
 * ADB-командах/парсинге.
 */
interface DeviceInfoService {

    /**
     * Загрузить данные одной секции в формате Key | Value.
     *
     * @param section Секция для загрузки.
     * @param deviceId ID активного устройства.
     * @param adbPath Путь к `adb`.
     */
    suspend fun loadSection(
        section: DeviceInfoSectionKind,
        deviceId: String,
        adbPath: String,
    ): Result<List<DeviceInfoRow>>
}
