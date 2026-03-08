package com.adbdeck.feature.logcat

/**
 * Типизированные ошибки экрана Logcat.
 *
 * Вместо хранения готовых строк в state, UI сам маппит эти типы
 * в локализованные ресурсы.
 */
sealed interface LogcatError {

    /**
     * Активное устройство не выбрано.
     */
    data object NoActiveDevice : LogcatError

    /**
     * Выбранное устройство недоступно для работы с adb.
     *
     * @param deviceId Идентификатор устройства.
     * @param deviceStateRaw Сырое состояние устройства из adb.
     */
    data class DeviceUnavailable(
        val deviceId: String,
        val deviceStateRaw: String,
    ) : LogcatError

    /**
     * Ошибка во время чтения потока logcat.
     *
     * @param details Дополнительный текст ошибки (если есть).
     */
    data class StreamFailure(
        val details: String?,
    ) : LogcatError

    /**
     * Ошибка импорта logcat-файла.
     *
     * @param details Дополнительный текст ошибки (если есть).
     */
    data class ImportFailure(
        val details: String?,
    ) : LogcatError

    /**
     * Ошибка экспорта logcat-файла.
     *
     * @param details Дополнительный текст ошибки (если есть).
     */
    data class ExportFailure(
        val details: String?,
    ) : LogcatError
}
