package com.adbdeck.core.adb.api

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

/**
 * Базовая информация об установленном пакете.
 *
 * Получается из вывода команды `pm list packages -f`.
 *
 * @param packageName Имя пакета (reverse-DNS, например `com.example.app`).
 * @param apkPath     Абсолютный путь к основному APK-файлу на устройстве.
 * @param type        Тип пакета: пользовательский или системный.
 * @param isEnabled   Включён ли пакет (`pm disable` может отключить его).
 */
data class AppPackage(
    val packageName: String,
    val apkPath: String = "",
    val type: PackageType = PackageType.USER,
    val isEnabled: Boolean = true,
) {
    /**
     * Возвращает имя APK-файла без полного пути — удобно для краткого отображения.
     *
     * Например, `/data/app/com.example.app/base.apk` → `base.apk`.
     */
    val apkFileName: String
        get() = apkPath.substringAfterLast('/')
}
