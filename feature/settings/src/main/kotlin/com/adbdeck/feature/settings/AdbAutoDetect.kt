package com.adbdeck.feature.settings

import java.io.File

/**
 * Тупой и надежный поиск кандидатов ADB:
 * 1) PATH (`which -a adb` / `where adb`)
 * 2) стандартные SDK-пути для текущей ОС
 */
internal object AdbAutoDetect {

    fun detectCandidates(): List<String> {
        val ordered = linkedSetOf<String>()
        detectFromPath().forEach { candidate ->
            addCandidateIfExists(ordered, candidate)
        }
        standardPaths().forEach { candidate ->
            addCandidateIfExists(ordered, candidate)
        }
        return ordered.toList()
    }

    private fun detectFromPath(): List<String> {
        val command = when (detectHostOs()) {
            HostOs.WINDOWS -> listOf("where", "adb")
            else -> listOf("which", "-a", "adb")
        }
        return runCommandForLines(command)
    }

    private fun standardPaths(): List<String> {
        val home = System.getProperty("user.home").orEmpty()
        return when (detectHostOs()) {
            HostOs.MAC -> listOf(
                File(home, "Library/Android/sdk/platform-tools/adb").path,
                File(home, "Android/Sdk/platform-tools/adb").path,
                "/opt/homebrew/bin/adb",
                "/usr/local/bin/adb",
            )

            HostOs.LINUX -> listOf(
                File(home, "Android/Sdk/platform-tools/adb").path,
                File(home, "Android/sdk/platform-tools/adb").path,
                "/usr/bin/adb",
                "/usr/local/bin/adb",
            )

            HostOs.WINDOWS -> buildList {
                val localAppData = System.getenv("LOCALAPPDATA").orEmpty()
                if (localAppData.isNotBlank()) {
                    add(File(localAppData, "Android/Sdk/platform-tools/adb.exe").path)
                }
                add("C:\\Android\\sdk\\platform-tools\\adb.exe")
                add("C:\\Program Files\\Android\\Android Studio\\platform-tools\\adb.exe")
            }

            HostOs.UNKNOWN -> emptyList()
        }
    }

    private fun addCandidateIfExists(
        target: MutableSet<String>,
        rawPath: String,
    ) {
        val normalized = normalizeCandidatePath(rawPath) ?: return
        val file = File(normalized)
        if (!file.isFile) return
        if (detectHostOs() != HostOs.WINDOWS && !file.canExecute()) return
        target.add(normalized)
    }

    private fun normalizeCandidatePath(rawPath: String): String? {
        val cleaned = rawPath.trim().removeSurrounding("\"")
        if (cleaned.isBlank()) return null
        return File(cleaned).absoluteFile.normalize().path
    }

    private fun runCommandForLines(command: List<String>): List<String> {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().toList()
            }
            process.waitFor()
            output
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun detectHostOs(): HostOs {
        val osName = System.getProperty("os.name")
            ?.lowercase()
            .orEmpty()
        return when {
            osName.contains("win") -> HostOs.WINDOWS
            osName.contains("mac") -> HostOs.MAC
            osName.contains("nix") || osName.contains("nux") || osName.contains("linux") -> HostOs.LINUX
            else -> HostOs.UNKNOWN
        }
    }

    private enum class HostOs {
        MAC,
        LINUX,
        WINDOWS,
        UNKNOWN,
    }
}
