package com.adbdeck.core.settings

/** Дефолтное имя исполняемого файла adb (поиск через системный PATH). */
const val DEFAULT_ADB_EXECUTABLE: String = "adb"

/**
 * Нормализует пользовательский путь к adb:
 * - trim пробелы;
 * - если строка пустая, возвращает [DEFAULT_ADB_EXECUTABLE].
 */
fun String?.resolvedAdbPathOrDefault(): String =
    this?.trim().orEmpty().ifBlank { DEFAULT_ADB_EXECUTABLE }

/** Возвращает нормализованный путь к adb из настроек приложения. */
fun AppSettings.resolvedAdbPath(): String = adbPath.resolvedAdbPathOrDefault()
