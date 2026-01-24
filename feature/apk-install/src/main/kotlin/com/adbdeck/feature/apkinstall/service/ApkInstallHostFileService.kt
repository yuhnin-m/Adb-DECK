package com.adbdeck.feature.apkinstall.service

/**
 * Причина невалидного APK-пути на хосте.
 */
enum class ApkFileValidationError {
    EMPTY_PATH,
    FILE_NOT_FOUND,
    INVALID_EXTENSION,
    IO_ACCESS_ERROR,
}

/**
 * Результат проверки APK-файла на хосте.
 */
sealed interface ApkFileValidationResult {
    /** Путь валиден и указывает на существующий `.apk` файл. */
    data class Valid(
        val absolutePath: String,
        val fileName: String,
    ) : ApkFileValidationResult

    /** Путь невалиден, содержит конкретную типизированную причину. */
    data class Invalid(
        val reason: ApkFileValidationError,
        val originalPath: String = "",
    ) : ApkFileValidationResult
}

/**
 * Host-side операции для feature APK install.
 */
interface ApkInstallHostFileService {

    /**
     * Открыть системный диалог выбора APK-файла.
     *
     * @return Абсолютный путь к выбранному файлу или `null`, если пользователь отменил выбор.
     */
    suspend fun selectApkFile(initialPath: String): Result<String?>

    /**
     * Проверить, что путь указывает на существующий `.apk` файл.
     */
    suspend fun validateApkPath(path: String): ApkFileValidationResult
}
