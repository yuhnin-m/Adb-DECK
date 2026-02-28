package com.adbdeck.core.utils

import java.util.Locale

/**
 * Утилита запуска системного терминала с ADB shell-командой.
 */
object TerminalLauncher {

    /**
     * Открывает системный терминал и запускает внутри `adb shell`.
     *
     * Если задан [deviceId], команда запускается как `adb -s <deviceId> shell`.
     * Если [root] = `true`, дополнительно добавляется `su`.
     *
     * @throws IllegalStateException если не удалось запустить процесс терминала.
     */
    fun openAdbShell(
        adbPath: String,
        deviceId: String? = null,
        root: Boolean = false,
    ) {
        val adbCommandArgs = buildAdbCommandArgs(
            adbPath = adbPath,
            deviceId = deviceId,
            root = root,
        )

        when (currentOs()) {
            HostOs.WINDOWS -> launchWindows(adbCommandArgs)
            HostOs.MAC -> launchMacOs(adbCommandArgs)
            HostOs.LINUX -> launchLinux(adbCommandArgs)
        }
    }

    private fun buildAdbCommandArgs(
        adbPath: String,
        deviceId: String?,
        root: Boolean,
    ): List<String> = buildList {
        val normalizedAdbPath = adbPath.trim().ifBlank { "adb" }
        add(normalizedAdbPath)

        deviceId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { normalizedDeviceId ->
                add("-s")
                add(normalizedDeviceId)
            }

        add("shell")
        if (root) {
            add("su")
        }
    }

    private fun launchWindows(commandArgs: List<String>) {
        val adbCommand = commandArgs.joinToString(" ") { arg -> quoteForWindowsCmd(arg) }
        launchProcess(listOf("cmd.exe", "/k", adbCommand))
    }

    private fun launchMacOs(commandArgs: List<String>) {
        val adbCommand = buildPosixShellCommand(commandArgs)
        val terminalScriptCommand = "bash -lc ${quoteForDoubleQuotedShell(adbCommand)}"

        launchProcess(
            listOf(
                "osascript",
                "-e",
                "tell application \"Terminal\"",
                "-e",
                "activate",
                "-e",
                "do script \"${escapeForAppleScript(terminalScriptCommand)}\"",
                "-e",
                "end tell",
            ),
        )
    }

    private fun launchLinux(commandArgs: List<String>) {
        val adbCommand = buildPosixShellCommand(commandArgs)
        val launchExpression = """
            if command -v xdg-terminal-exec >/dev/null 2>&1; then
              xdg-terminal-exec sh -lc ${quoteForPosixShellToken(adbCommand)};
            elif command -v x-terminal-emulator >/dev/null 2>&1; then
              x-terminal-emulator -e sh -lc ${quoteForPosixShellToken(adbCommand)};
            else
              sh -lc ${quoteForPosixShellToken(adbCommand)};
            fi
        """.trimIndent()
        launchProcess(listOf("sh", "-c", launchExpression))
    }

    private fun launchProcess(command: List<String>) {
        runCatching {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        }.getOrElse { error ->
            throw IllegalStateException("Failed to launch terminal", error)
        }
    }

    private fun buildPosixShellCommand(commandArgs: List<String>): String =
        commandArgs.joinToString(" ") { arg -> quoteForPosixShellToken(arg) }

    private fun quoteForPosixShellToken(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"

    private fun quoteForDoubleQuotedShell(value: String): String =
        "\"" +
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`") +
            "\""

    private fun quoteForWindowsCmd(value: String): String {
        if (value.isEmpty()) return "\"\""
        val needsQuotes = value.any { char ->
            char.isWhitespace() || char == '"' || char == '^' || char == '&' ||
                char == '|' || char == '<' || char == '>'
        }
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\\\"")}\""
    }

    private fun escapeForAppleScript(command: String): String =
        command
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private fun currentOs(): HostOs {
        val name = System.getProperty("os.name")
            .orEmpty()
            .lowercase(Locale.getDefault())
        return when {
            name.contains("win") -> HostOs.WINDOWS
            name.contains("mac") -> HostOs.MAC
            else -> HostOs.LINUX
        }
    }

    private enum class HostOs {
        WINDOWS,
        MAC,
        LINUX,
    }
}
