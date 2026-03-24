package com.adbdeck.feature.update.provider

import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Проверка обновлений через GitHub Releases API (`/releases/latest`).
 */
class GithubReleasesUpdateProvider(
    private val owner: String = DEFAULT_OWNER,
    private val repository: String = DEFAULT_REPOSITORY,
) : AppUpdateProvider {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override suspend fun checkForUpdate(currentVersion: String): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        runCatchingPreserveCancellation {
            val latestRelease = fetchLatestRelease()

            if (!isNewerVersion(
                    currentVersion = currentVersion,
                    candidateVersion = latestRelease.version,
                )
            ) {
                AppUpdateCheckResult.UpToDate
            } else {
                val currentPlatform = detectPlatform()
                val selectedAsset = selectBestAsset(
                    assets = latestRelease.assets,
                    platform = currentPlatform,
                )
                val downloadUrl = selectedAsset?.downloadUrl ?: latestRelease.releasePageUrl
                val expectedSha512 = selectedAsset?.let { asset ->
                    findExpectedSha512ForAsset(
                        selectedAsset = asset,
                        assets = latestRelease.assets,
                        platform = currentPlatform,
                    )
                }

                AppUpdateCheckResult.UpdateAvailable(
                    version = latestRelease.version,
                    changelog = latestRelease.changelog,
                    downloadUrl = downloadUrl,
                    expectedSha512 = expectedSha512,
                )
            }
        }.getOrElse { error ->
            AppUpdateCheckResult.Failed(
                reason = error.message.orEmpty(),
            )
        }
    }

    private fun fetchLatestRelease(): GithubReleaseDto {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$owner/$repository/releases/latest"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ADBDeck-Updater")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("GitHub releases API failed: HTTP ${response.statusCode()}")
        }

        val root = json.parseToJsonElement(response.body()).jsonObject
        return root.toGithubReleaseDto()
    }

    private fun JsonObject.toGithubReleaseDto(): GithubReleaseDto {
        val tag = this["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val normalizedVersion = tag.removePrefix("v").removePrefix("V")
        if (normalizedVersion.isBlank()) {
            error("Release tag_name is empty")
        }

        val releasePageUrl = this["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (releasePageUrl.isBlank()) {
            error("Release html_url is empty")
        }

        val changelog = this["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val assetsJson = this["assets"]?.jsonArray ?: JsonArray(emptyList())
        val assets = assetsJson.toAssets()

        return GithubReleaseDto(
            version = normalizedVersion,
            changelog = changelog,
            releasePageUrl = releasePageUrl,
            assets = assets,
        )
    }

    private fun JsonArray.toAssets(): List<GithubAssetDto> {
        return mapNotNull { item ->
            val objectValue = item.jsonObject
            val name = objectValue["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val url = objectValue["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (name.isBlank() || url.isBlank()) return@mapNotNull null
            GithubAssetDto(name = name, downloadUrl = url)
        }
    }

    private fun selectBestAsset(
        assets: List<GithubAssetDto>,
        platform: DetectedPlatform,
    ): GithubAssetDto? {
        if (assets.isEmpty()) return null

        val osFiltered = assets.filter { asset ->
            val lower = asset.name.lowercase()
            platform.osTokens.any(lower::contains)
        }.ifEmpty {
            assets
        }

        val archFiltered = if (platform.archTokens.isEmpty()) {
            osFiltered
        } else {
            osFiltered.filter { asset ->
                val lower = asset.name.lowercase()
                platform.archTokens.any(lower::contains)
            }.ifEmpty {
                osFiltered
            }
        }

        return archFiltered.maxByOrNull { asset ->
            scoreAsset(asset.name.lowercase(), platform)
        }
    }

    private fun findExpectedSha512ForAsset(
        selectedAsset: GithubAssetDto,
        assets: List<GithubAssetDto>,
        platform: DetectedPlatform,
    ): String? {
        val metadataAsset = findUpdateMetadataAsset(assets = assets, platform = platform) ?: return null

        val metadataContent = fetchTextAsset(metadataAsset.downloadUrl)
        val checksumsByFilename = parseSha512ByFileName(metadataContent)
        return checksumsByFilename[selectedAsset.name]
    }

    private fun findUpdateMetadataAsset(
        assets: List<GithubAssetDto>,
        platform: DetectedPlatform,
    ): GithubAssetDto? {
        val candidates = when (platform.kind) {
            PlatformKind.MAC_OS -> listOf("latest-mac.yml")
            PlatformKind.WINDOWS -> listOf("latest.yml")
            PlatformKind.LINUX -> listOf("latest-linux.yml")
            PlatformKind.UNKNOWN -> emptyList()
        }
        if (candidates.isEmpty()) return null

        return candidates
            .asSequence()
            .mapNotNull { candidate ->
                assets.firstOrNull { asset ->
                    asset.name.equals(candidate, ignoreCase = true)
                }
            }
            .firstOrNull()
    }

    private fun fetchTextAsset(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "ADBDeck-Updater")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Update metadata download failed: HTTP ${response.statusCode()}")
        }
        return response.body()
    }

    private fun parseSha512ByFileName(content: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        var pendingFileName: String? = null

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val pending = pendingFileName

            if (trimmed.startsWith("- url:")) {
                pendingFileName = trimmed.substringAfter(":").trim().trim('"', '\'')
                return@forEach
            }

            if (pending != null && trimmed.startsWith("sha512:")) {
                val hash = trimmed.substringAfter(":").trim().trim('"', '\'')
                if (hash.isNotBlank()) {
                    map[pending] = hash
                }
                pendingFileName = null
            }
        }

        return map
    }

    private fun scoreAsset(assetName: String, platform: DetectedPlatform): Int {
        var score = 0
        if (platform.osTokens.any(assetName::contains)) score += 20
        if (platform.archTokens.any(assetName::contains)) score += 15

        score += when (platform.kind) {
            PlatformKind.MAC_OS -> when {
                assetName.endsWith(".zip") -> 45
                assetName.endsWith(".dmg") -> 35
                else -> 0
            }
            PlatformKind.WINDOWS -> if (assetName.endsWith(".msi")) 40 else 0
            PlatformKind.LINUX -> when {
                assetName.endsWith(".deb") -> 35
                assetName.endsWith(".rpm") -> 30
                else -> 0
            }
            PlatformKind.UNKNOWN -> 0
        }

        return score
    }

    private fun isNewerVersion(
        currentVersion: String,
        candidateVersion: String,
    ): Boolean {
        val current = parseVersion(currentVersion)
        val candidate = parseVersion(candidateVersion)
        if (current == null || candidate == null) {
            return candidateVersion != currentVersion
        }
        return candidate > current
    }

    private fun parseVersion(raw: String): SemanticVersion? {
        val normalized = raw.trim().removePrefix("v").removePrefix("V")
        val match = VERSION_REGEX.find(normalized) ?: return null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        return SemanticVersion(major, minor, patch)
    }

    private fun detectPlatform(): DetectedPlatform {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val osArch = System.getProperty("os.arch").orEmpty().lowercase()

        val osKind = when {
            osName.contains("mac") || osName.contains("darwin") -> PlatformKind.MAC_OS
            osName.contains("win") -> PlatformKind.WINDOWS
            osName.contains("nux") || osName.contains("linux") -> PlatformKind.LINUX
            else -> PlatformKind.UNKNOWN
        }

        val archTokens = when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> listOf("arm64", "aarch64")
            osArch.contains("x86_64") || osArch.contains("amd64") -> listOf("x64", "x86_64", "amd64")
            osArch.contains("x86") -> listOf("x86", "i386")
            else -> emptyList()
        }

        val osTokens = when (osKind) {
            PlatformKind.MAC_OS -> listOf("macos", "darwin", "mac")
            PlatformKind.WINDOWS -> listOf("windows", "win")
            PlatformKind.LINUX -> listOf("linux")
            PlatformKind.UNKNOWN -> emptyList()
        }

        return DetectedPlatform(
            kind = osKind,
            osTokens = osTokens,
            archTokens = archTokens,
        )
    }

    private data class GithubReleaseDto(
        val version: String,
        val changelog: String,
        val releasePageUrl: String,
        val assets: List<GithubAssetDto>,
    )

    private data class GithubAssetDto(
        val name: String,
        val downloadUrl: String,
    )

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<SemanticVersion> {
        override fun compareTo(other: SemanticVersion): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            return patch.compareTo(other.patch)
        }
    }

    private data class DetectedPlatform(
        val kind: PlatformKind,
        val osTokens: List<String>,
        val archTokens: List<String>,
    )

    private enum class PlatformKind {
        MAC_OS,
        WINDOWS,
        LINUX,
        UNKNOWN,
    }

    private companion object {
        val VERSION_REGEX = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?""")
        private const val DEFAULT_OWNER = "yuhnin-m"
        private const val DEFAULT_REPOSITORY = "Adb-DECK"
    }
}
