package com.adbdeck.core.adb.api

/**
 * Тип транспортного соединения ADB-устройства.
 *
 * Определяется эвристически по формату [AdbDevice.deviceId]:
 * - `emulator-XXXX` → [EMULATOR]
 * - `IP:PORT` (содержит двоеточие) → [WIFI]
 * - Всё остальное → [USB]
 */
enum class DeviceTransportType {
    /** USB-кабель. */
    USB,

    /** Беспроводное подключение по Wi-Fi (adb over TCP/IP). */
    WIFI,

    /** Android-эмулятор (emulator-5554 и т.д.). */
    EMULATOR,

    /** Не удалось определить тип. */
    UNKNOWN,
}

/**
 * Расширенная информация об Android-устройстве, полученная через ADB shell.
 *
 * Основной источник — `adb -s <id> shell getprop` (батарея — `dumpsys battery`,
 * разрешение — `wm size`).
 *
 * Все поля имеют безопасные значения по умолчанию: пустая строка означает «недоступно».
 *
 * @param deviceId          Идентификатор устройства (серийник или IP:port).
 * @param model             Модель устройства (`ro.product.model`).
 * @param manufacturer      Производитель (`ro.product.manufacturer`).
 * @param brand             Бренд (`ro.product.brand`).
 * @param productName       Кодовое имя продукта (`ro.product.name`).
 * @param androidVersion    Версия Android (`ro.build.version.release`).
 * @param sdkVersion        SDK API level (`ro.build.version.sdk`). 0 = неизвестно.
 * @param buildFingerprint  Полный build fingerprint (`ro.build.fingerprint`).
 * @param securityPatch     Дата патча безопасности (`ro.build.version.security_patch`).
 * @param cpuAbiList        Список поддерживаемых ABI (`ro.product.cpu.abilist`).
 * @param screenResolution  Разрешение экрана в формате «WxH» (из `wm size`).
 * @param screenDensity     Плотность экрана в dpi (`ro.sf.lcd_density`).
 * @param batteryLevel      Уровень заряда батареи (0–100). -1 если не удалось получить.
 * @param batteryCharging   `true` если устройство заряжается (status == 2).
 * @param transportType     Тип транспорта — USB / Wi-Fi / Emulator.
 */
data class DeviceInfo(
    val deviceId: String,
    val model: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val productName: String = "",
    val androidVersion: String = "",
    val sdkVersion: Int = 0,
    val buildFingerprint: String = "",
    val securityPatch: String = "",
    val cpuAbiList: String = "",
    val screenResolution: String = "",
    val screenDensity: Int = 0,
    val batteryLevel: Int = -1,
    val batteryCharging: Boolean = false,
    val transportType: DeviceTransportType = DeviceTransportType.UNKNOWN,
) {
    /**
     * Читаемое название устройства.
     *
     * Приоритет: «Manufacturer Model» → Model → productName → deviceId.
     */
    val displayName: String
        get() = when {
            manufacturer.isNotEmpty() && model.isNotEmpty() ->
                "$manufacturer $model"
            model.isNotEmpty() -> model
            productName.isNotEmpty() -> productName
            else -> deviceId
        }

    /** `true` если устройство является эмулятором. */
    val isEmulator: Boolean
        get() = transportType == DeviceTransportType.EMULATOR
}

// ── Состояние загрузки DeviceInfo ─────────────────────────────────────────────

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
