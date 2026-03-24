package com.adbdeck.feature.update.provider

/**
 * Контракт источника данных об обновлениях приложения.
 */
interface AppUpdateProvider {

    /**
     * Проверить, доступна ли более новая версия относительно [currentVersion].
     */
    suspend fun checkForUpdate(currentVersion: String): AppUpdateCheckResult
}

/**
 * Результат проверки обновления приложения.
 */
sealed interface AppUpdateCheckResult {
    /** Обновление не требуется. */
    data object UpToDate : AppUpdateCheckResult

    /**
     * Найдена более новая версия.
     *
     * @param version Целевая версия.
     * @param changelog Changelog (body релиза).
     * @param downloadUrl Ссылка на подходящий бинарный ассет (или страницу релиза).
     * @param expectedSha512 Ожидаемый SHA-512 (base64) для проверки целостности скачанного файла.
     */
    data class UpdateAvailable(
        val version: String,
        val changelog: String,
        val downloadUrl: String,
        val expectedSha512: String? = null,
    ) : AppUpdateCheckResult

    /**
     * Проверка не удалась.
     *
     * @param reason Техническое описание причины.
     */
    data class Failed(
        val reason: String,
    ) : AppUpdateCheckResult
}
