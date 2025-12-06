package com.adbdeck.core.adb.api.screen

/**
 * Прогресс установки APK.
 *
 * @param progress Значение `0f..1f`, либо `null`, если доступен только текстовый статус.
 * @param message Текущий статус/строка из вывода adb.
 */
data class ApkInstallProgress(
    val progress: Float?,
    val message: String,
)
