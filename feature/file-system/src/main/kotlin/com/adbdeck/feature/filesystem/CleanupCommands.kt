package com.adbdeck.feature.filesystem

/**
 * Генерация shell-команд для очистки временных директорий на Android-устройстве.
 *
 * Команды выполняются через `adb shell sh -c ...`, поэтому они не зависят от ОС хоста.
 */
internal object CleanupCommands {

    fun commandsFor(options: Set<CleanupOption>): List<String> =
        options.sortedBy { it.ordinal }.mapNotNull(::commandFor)

    private fun commandFor(option: CleanupOption): String? = when (option) {
        CleanupOption.TEMP ->
            "find /data/local/tmp -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"

        CleanupOption.DOWNLOADS ->
            "find /sdcard/Download -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"

        CleanupOption.APP_CACHE ->
            "find /sdcard/Android/data/*/cache -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"
    }
}
