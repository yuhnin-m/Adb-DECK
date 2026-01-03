package com.adbdeck.feature.deviceinfo

/**
 * Идентификаторы секций экрана Device Info.
 *
 * @param id Стабильный ID для сериализации/экспорта.
 * @param exportTitle Англоязычный заголовок для стабильного JSON-экспорта.
 */
enum class DeviceInfoSectionKind(
    val id: String,
    val exportTitle: String,
) {
    OVERVIEW(id = "overview", exportTitle = "Overview"),
    BUILD(id = "build", exportTitle = "Build"),
    DISPLAY(id = "display", exportTitle = "Display"),
    CPU_RAM(id = "cpu_ram", exportTitle = "CPU/RAM"),
    BATTERY(id = "battery", exportTitle = "Battery"),
    NETWORK(id = "network", exportTitle = "Network"),
    STORAGE(id = "storage", exportTitle = "Storage"),
    SECURITY(id = "security", exportTitle = "Security"),
    SYSTEM(id = "system", exportTitle = "System"),
}

/**
 * Состояние загрузки отдельной секции Device Info.
 */
sealed interface DeviceInfoSectionLoadState {
    /** Секция загружается. */
    data object Loading : DeviceInfoSectionLoadState

    /** Секция успешно загружена. */
    data class Success(val rows: List<DeviceInfoRow>) : DeviceInfoSectionLoadState

    /** Секция завершилась с ошибкой. */
    data class Error(val message: String) : DeviceInfoSectionLoadState
}

/**
 * Данные отдельной секции экрана Device Info.
 *
 * @param kind Идентификатор секции.
 * @param state Состояние секции (loading/success/error).
 */
data class DeviceInfoSection(
    val kind: DeviceInfoSectionKind,
    val state: DeviceInfoSectionLoadState,
)

/**
 * Строка мини-таблицы Key | Value.
 *
 * @param id Стабильный ID строки внутри экрана.
 * @param key Подпись ключа.
 * @param value Значение.
 */
data class DeviceInfoRow(
    val id: String,
    val key: String,
    val value: String,
)

/**
 * Краткосрочное уведомление для верхнего уровня экрана Device Info.
 *
 * @param message Текст сообщения.
 * @param isError Признак ошибки.
 */
data class DeviceInfoFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Полное состояние экрана Device Info.
 *
 * @param activeDeviceId ID активного устройства (`adb -s <id>`), либо null.
 * @param sections Список секций с независимыми состояниями.
 * @param isRefreshing `true`, пока выполняется ручное обновление секций.
 * @param isExportingJson `true`, пока выполняется экспорт JSON на диск.
 * @param lastUpdatedAtMillis Timestamp последнего успешного refresh (epoch millis).
 * @param feedback Краткосрочное сообщение обратной связи.
 */
data class DeviceInfoState(
    val activeDeviceId: String? = null,
    val sections: List<DeviceInfoSection> = defaultDeviceInfoSections(),
    val isRefreshing: Boolean = false,
    val isExportingJson: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
    val feedback: DeviceInfoFeedback? = null,
) {
    /** `true`, если есть активное устройство в состоянии DEVICE. */
    val isDeviceAvailable: Boolean
        get() = !activeDeviceId.isNullOrBlank()
}

/**
 * Создает секции в фиксированном порядке.
 */
fun defaultDeviceInfoSections(
    loadState: DeviceInfoSectionLoadState = DeviceInfoSectionLoadState.Loading,
): List<DeviceInfoSection> = DeviceInfoSectionKind.entries.map { kind ->
    DeviceInfoSection(kind = kind, state = loadState)
}
