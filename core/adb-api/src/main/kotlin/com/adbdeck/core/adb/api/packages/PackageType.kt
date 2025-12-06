package com.adbdeck.core.adb.api.packages

/**
 * Тип установленного пакета.
 *
 * Определяется по пути к APK-файлу:
 * `/system`, `/product`, `/vendor` и их подкаталоги → [SYSTEM], остальное → [USER].
 */
enum class PackageType {
    /** Пользовательское приложение (установлено через Play Store или ADB). */
    USER,

    /** Системное приложение (предустановлено в ОС). */
    SYSTEM,
}
