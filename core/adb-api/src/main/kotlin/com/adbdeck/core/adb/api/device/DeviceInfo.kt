package com.adbdeck.core.adb.api.device

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
