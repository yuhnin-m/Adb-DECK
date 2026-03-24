package com.adbdeck.feature.update.download

import com.adbdeck.feature.update.logging.AppUpdateLogger
import com.adbdeck.feature.update.logging.NoOpAppUpdateLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import kotlin.coroutines.coroutineContext

/**
 * HTTP-реализация [AppUpdateDownloader] для загрузки update-ассета с прогрессом.
 */
class HttpAppUpdateDownloader(
    private val appUpdateLogger: AppUpdateLogger = NoOpAppUpdateLogger,
) : AppUpdateDownloader {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val downloadRoot: Path = Path.of(
        System.getProperty("java.io.tmpdir").orEmpty(),
        "adbdeck",
        "updates",
        "downloads",
    )

    override suspend fun download(
        url: String,
        targetVersion: String,
        onProgress: (Float?) -> Unit,
    ): DownloadedAppUpdatePackage = withContext(Dispatchers.IO) {
        val uri = URI.create(url)
        Files.createDirectories(downloadRoot)

        val extension = extractExtension(uri.path)
        val safeVersion = targetVersion
            .ifBlank { "unknown" }
            .replace(VERSION_SANITIZE_REGEX, "_")
        val baseName = "adbdeck-update-$safeVersion-${System.currentTimeMillis()}"

        val targetFile = downloadRoot.resolve(baseName + extension)
        val partFile = downloadRoot.resolve(baseName + extension + ".part")

        appUpdateLogger.info("Downloading app update package: $url")
        onProgress(0f)

        try {
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "ADBDeck-Updater")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299) {
                error("Update download failed: HTTP ${response.statusCode()}")
            }

            val contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L)
                .takeIf { it > 0L }

            response.body().use { input ->
                Files.newOutputStream(
                    partFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                ).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L

                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val progress = if (contentLength != null) {
                            (downloadedBytes.toDouble() / contentLength.toDouble()).toFloat().coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        onProgress(progress)
                    }
                }
            }

            val size = Files.size(partFile)
            if (size <= 0L) {
                error("Downloaded update package is empty: $partFile")
            }

            Files.move(partFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
            onProgress(1f)

            appUpdateLogger.info("App update package downloaded: $targetFile ($size bytes)")
            DownloadedAppUpdatePackage(
                file = targetFile,
                sourceUrl = url,
                targetVersion = targetVersion,
            )
        } catch (cancellation: CancellationException) {
            appUpdateLogger.info("App update package download was cancelled.")
            runCatching { Files.deleteIfExists(partFile) }
            runCatching { Files.deleteIfExists(targetFile) }
            throw cancellation
        } catch (error: Throwable) {
            appUpdateLogger.error("Failed to download app update package.", error)
            runCatching { Files.deleteIfExists(partFile) }
            runCatching { Files.deleteIfExists(targetFile) }
            throw error
        }
    }

    private fun extractExtension(path: String): String {
        val filename = path.substringAfterLast('/', missingDelimiterValue = "")
        if (!filename.contains('.')) return ".bin"
        val ext = filename.substringAfterLast('.', missingDelimiterValue = "")
        return if (ext.isBlank() || ext.length > 10) ".bin" else ".${ext.lowercase()}"
    }

    private companion object {
        val VERSION_SANITIZE_REGEX = Regex("[^A-Za-z0-9._-]")
    }
}
