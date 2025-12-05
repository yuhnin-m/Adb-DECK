package com.adbdeck.core.adb.api.device

/**
 * Состояние подключенного ADB-устройства.
 *
 * Значения соответствуют строкам, которые возвращает команда `adb devices`.
 *
 * @param rawValue Строковое значение из вывода adb.
 */
enum class DeviceState(val rawValue: String) {
    /** Устройство готово к работе. */
    DEVICE("device"),

    /** Устройство недоступно или отключено. */
    OFFLINE("offline"),

    /** Требуется подтверждение отладки по USB на устройстве. */
    UNAUTHORIZED("unauthorized"),

    /** Неизвестное или нераспознанное состояние. */
    UNKNOWN("unknown");

    companion object {
        /**
         * Возвращает [DeviceState] по строковому значению из вывода adb.
         * Если строка не распознана — возвращает [UNKNOWN].
         *
         * @param value Строка из вывода `adb devices`.
         */
        fun fromRawValue(value: String): DeviceState =
            entries.find { it.rawValue == value.trim() } ?: UNKNOWN
    }
}
