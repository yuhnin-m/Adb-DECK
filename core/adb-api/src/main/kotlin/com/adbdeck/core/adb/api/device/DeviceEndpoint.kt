package com.adbdeck.core.adb.api.device

/**
 * Сохраненный TCP/IP endpoint ADB-устройства.
 *
 * Используется для:
 * - Wi-Fi подключений через `adb connect host:port`
 * - Списка избранных/известных устройств
 * - Быстрого переподключения ранее использованных устройств
 *
 * @param host  Хост или IP-адрес устройства (например "192.168.1.100").
 * @param port  ADB-порт (по умолчанию 5555).
 * @param label Необязательный пользовательский псевдоним для удобства.
 */
data class DeviceEndpoint(
    val host: String,
    val port: Int = 5555,
    val label: String = "",
) {
    /** Полный адрес в формате "host:port", используемый в adb connect/disconnect. */
    val address: String get() = "$host:$port"

    companion object {
        /**
         * Парсит строку вида "host:port" в [DeviceEndpoint].
         *
         * Корректно обрабатывает IPv6-адреса (последнее двоеточие — разделитель порта).
         *
         * @param address Строка адреса.
         * @return Распознанный [DeviceEndpoint] или `null` если строка некорректна.
         */
        fun fromAddress(address: String): DeviceEndpoint? {
            if (address.isBlank()) return null
            val lastColon = address.lastIndexOf(':')
            return if (lastColon > 0) {
                val host = address.substring(0, lastColon)
                val port = address.substring(lastColon + 1).toIntOrNull() ?: return null
                if (host.isBlank() || port !in 1..65535) return null
                DeviceEndpoint(host = host, port = port)
            } else {
                // Нет порта — принять как хост с портом по умолчанию
                DeviceEndpoint(host = address)
            }
        }
    }
}
