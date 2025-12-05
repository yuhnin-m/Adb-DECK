package com.adbdeck.core.adb.api.intents

/**
 * Режим запуска activity через ADB.
 */
enum class LaunchMode {
    /** Запуск через deep link (ACTION_VIEW + URI). */
    DEEP_LINK,

    /** Явный Intent с набором параметров. */
    INTENT,
}

/**
 * Тип typed-extra для `am start`.
 *
 * @param flag  ADB-флаг для передачи значения (напр. `--es`, `--ei`).
 * @param label Отображаемое название типа.
 */
enum class ExtraType(val flag: String, val label: String) {
    STRING("--es", "String"),
    INT("--ei", "Int"),
    LONG("--el", "Long"),
    BOOLEAN("--ez", "Boolean"),
    FLOAT("--ef", "Float"),
}

/**
 * Один дополнительный параметр (extra) intent'а.
 *
 * @param key   Ключ extra.
 * @param type  Тип extra ([ExtraType]).
 * @param value Значение extra в виде строки.
 */
data class IntentExtra(
    val key: String = "",
    val type: ExtraType = ExtraType.STRING,
    val value: String = "",
)

/**
 * Параметры для запуска Deep Link (ACTION_VIEW + URI).
 *
 * @param uri         URI для запуска (обязательно).
 * @param packageName Пакет-приёмник (опционально).
 * @param component   Компонент activity (опционально, формат: `pkg/.Activity`).
 * @param action      Android-action (по умолчанию ACTION_VIEW).
 * @param category    Android-category (опционально).
 */
data class DeepLinkParams(
    val uri: String,
    val packageName: String = "",
    val component: String = "",
    val action: String = "android.intent.action.VIEW",
    val category: String = "",
)

/**
 * Параметры явного Intent для `adb shell am start`.
 *
 * @param action      Intent action (опционально).
 * @param dataUri     URI данных (опционально).
 * @param packageName Целевой пакет (опционально).
 * @param component   Целевой компонент `package/.Activity` (опционально).
 * @param categories  Список categories (опционально).
 * @param flags       Флаги в hex (напр. `0x10000000`).
 * @param extras      Список typed-extras.
 */
data class IntentParams(
    val action: String = "",
    val dataUri: String = "",
    val packageName: String = "",
    val component: String = "",
    val categories: List<String> = emptyList(),
    val flags: String = "",
    val extras: List<IntentExtra> = emptyList(),
)

/**
 * Результат выполнения `adb shell am start`.
 *
 * @param exitCode       Код завершения процесса.
 * @param stdout         Стандартный вывод команды.
 * @param stderr         Вывод ошибок.
 * @param commandPreview Полная строка команды (для отображения пользователю).
 */
data class LaunchResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val commandPreview: String,
) {
    /**
     * `true` если команда завершилась без ошибок.
     *
     * ADB может вернуть exitCode=0, но с "Error" в stdout
     * (например, если activity не найдена).
     */
    val isSuccess: Boolean
        get() = exitCode == 0
                && !stdout.contains("Error", ignoreCase = true)
                && !stderr.contains("Error", ignoreCase = true)
}

/**
 * Клиент для запуска deep link'ов и intent'ов через `adb shell am start`.
 */
interface IntentLaunchClient {

    /**
     * Строит строку команды для deep link (без выполнения, только для отображения).
     */
    fun buildDeepLinkCommand(
        deviceId: String,
        adbPath: String = "adb",
        params: DeepLinkParams,
    ): String

    /**
     * Строит строку команды для Intent (без выполнения, только для отображения).
     */
    fun buildIntentCommand(
        deviceId: String,
        adbPath: String = "adb",
        params: IntentParams,
    ): String

    /**
     * Запускает deep link на подключённом устройстве.
     *
     * @return [Result] с [LaunchResult] при успехе или исключением при сетевой/ADB ошибке.
     */
    suspend fun launchDeepLink(
        deviceId: String,
        adbPath: String = "adb",
        params: DeepLinkParams,
    ): Result<LaunchResult>

    /**
     * Запускает явный Intent на подключённом устройстве.
     *
     * @return [Result] с [LaunchResult] при успехе или исключением при сетевой/ADB ошибке.
     */
    suspend fun launchIntent(
        deviceId: String,
        adbPath: String = "adb",
        params: IntentParams,
    ): Result<LaunchResult>
}
