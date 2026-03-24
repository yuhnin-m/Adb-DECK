package com.adbdeck.feature.update.download

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64

/**
 * Проверка целостности скачанного файла по SHA-512 (base64).
 */
object Sha512ChecksumVerifier {

    /**
     * @return `true`, если контрольная сумма файла совпала с ожидаемой.
     */
    fun verify(file: Path, expectedSha512Base64: String): Boolean {
        val expected = decodeExpectedHash(expectedSha512Base64) ?: return false
        val actual = computeSha512(file)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun decodeExpectedHash(raw: String): ByteArray? {
        val normalized = raw
            .trim()
            .removePrefix("sha512-")
            .removePrefix("SHA512-")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")

        if (normalized.isBlank()) return null

        return runCatching {
            Base64.getDecoder().decode(normalized)
        }.getOrNull()
    }

    private fun computeSha512(file: Path): ByteArray {
        val digest = MessageDigest.getInstance("SHA-512")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }
}
