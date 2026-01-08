package com.adbdeck.feature.deviceinfo.parser

/**
 * Разбирает вывод `pm list packages -U` в соответствие `uid -> packageNames`.
 *
 * Пример строки:
 * `package:com.example.app uid:10301`
 */
internal fun parsePackageUidMap(output: String): Map<Int, List<String>> {
    if (output.isBlank()) return emptyMap()

    val uidToPackages = linkedMapOf<Int, LinkedHashSet<String>>()
    output.lineSequence()
        .forEach { rawLine ->
            val line = rawLine.trim()
            val match = PACKAGE_UID_LINE_REGEX.matchEntire(line) ?: return@forEach
            val packageName = match.groupValues.getOrElse(1) { "" }.trim()
            val uid = match.groupValues.getOrElse(2) { "" }.toIntOrNull() ?: return@forEach
            if (packageName.isEmpty()) return@forEach
            uidToPackages.getOrPut(uid) { linkedSetOf() }.add(packageName)
        }

    return uidToPackages.mapValues { (_, packageNames) -> packageNames.toList() }
}

private val PACKAGE_UID_LINE_REGEX = Regex(
    pattern = """^package:([^\s]+)\s+uid:(\d+)\b.*$""",
)

