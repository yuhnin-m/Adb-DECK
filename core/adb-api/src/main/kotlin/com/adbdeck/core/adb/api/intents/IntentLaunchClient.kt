package com.adbdeck.core.adb.api.intents

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
