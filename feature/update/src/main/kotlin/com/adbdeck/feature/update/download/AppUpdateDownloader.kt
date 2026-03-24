package com.adbdeck.feature.update.download

import java.nio.file.Path

/**
 * Загружает пакет обновления приложения во временный файл.
 */
interface AppUpdateDownloader {

    /**
     * @param url URL бинарного ассета обновления.
     * @param targetVersion версия, для которой загружается пакет.
     * @param onProgress callback прогресса (`0f..1f`) или `null`, если размер неизвестен.
     */
    suspend fun download(
        url: String,
        targetVersion: String,
        onProgress: (Float?) -> Unit = {},
    ): DownloadedAppUpdatePackage
}

/**
 * Результат успешной загрузки пакета обновления.
 */
data class DownloadedAppUpdatePackage(
    val file: Path,
    val sourceUrl: String,
    val targetVersion: String,
)
