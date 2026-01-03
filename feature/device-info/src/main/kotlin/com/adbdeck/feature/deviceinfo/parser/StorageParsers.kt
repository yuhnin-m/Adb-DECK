package com.adbdeck.feature.deviceinfo.parser

/**
 * Строка раздела из `df -h`.
 */
internal data class StorageEntry(
    val fileSystem: String,
    val size: String,
    val used: String,
    val available: String,
    val usePercent: String,
    val mountPoint: String,
)

/**
 * Разбирает вывод `df -h` в список разделов.
 */
internal fun parseStorageEntries(output: String): List<StorageEntry> {
    val rows = output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { it.startsWith("Filesystem", ignoreCase = true) }
        .mapNotNull { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 6) return@mapNotNull null

            val mount = parts.drop(5).joinToString(" ")
            StorageEntry(
                fileSystem = parts[0],
                size = parts[1],
                used = parts[2],
                available = parts[3],
                usePercent = parts[4],
                mountPoint = mount,
            )
        }
        .filter { entry ->
            entry.mountPoint.startsWith('/') &&
                !entry.mountPoint.startsWith("/apex") &&
                !entry.mountPoint.startsWith("/dev") &&
                !entry.mountPoint.startsWith("/proc") &&
                !entry.mountPoint.startsWith("/sys")
        }
        .toList()

    val preferredOrder = listOf(
        "/data",
        "/storage/emulated",
        "/sdcard",
        "/system",
        "/vendor",
        "/cache",
    )

    return rows.sortedWith(
        compareBy<StorageEntry> { entry ->
            preferredOrder.indexOfFirst { entry.mountPoint.startsWith(it) }.let { index ->
                if (index >= 0) index else Int.MAX_VALUE
            }
        }.thenBy { it.mountPoint }
    )
}

/**
 * Находит самый заполненный раздел из списка.
 */
internal fun findMostUsedStorageEntry(entries: List<StorageEntry>): StorageEntry? {
    fun usagePercent(raw: String): Int = raw.trim().trimEnd('%').toIntOrNull() ?: -1
    return entries.maxByOrNull { usagePercent(it.usePercent) }
}

/**
 * Извлекает highlight-строку из `dumpsys diskstats`.
 */
internal fun parseDiskstatsHighlight(output: String): String? {
    return findFirstMatchingLine(output) { line ->
        line.contains("Data-Free", ignoreCase = true) ||
            line.contains("App Size", ignoreCase = true) ||
            line.contains("Cache Size", ignoreCase = true)
    }?.let(::normalizeForUi)
}
