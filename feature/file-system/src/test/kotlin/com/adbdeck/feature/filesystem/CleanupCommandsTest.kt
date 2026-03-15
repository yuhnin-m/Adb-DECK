package com.adbdeck.feature.filesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CleanupCommandsTest {

    @Test
    fun `commands are deterministic and ordered by enum ordinal`() {
        val commands = CleanupCommands.commandsFor(
            setOf(CleanupOption.APP_CACHE, CleanupOption.TEMP, CleanupOption.DOWNLOADS),
        )

        assertEquals(
            listOf(
                TEMP_COMMAND,
                DOWNLOADS_COMMAND,
                APP_CACHE_COMMAND,
            ),
            commands,
        )
    }

    @Test
    fun `commands are identical for mac windows linux`() {
        val expected = listOf(TEMP_COMMAND, DOWNLOADS_COMMAND, APP_CACHE_COMMAND)
        val osNames = listOf("Mac OS X", "Windows 11", "Linux")

        osNames.forEach { osName ->
            withSystemProperty("os.name", osName) {
                val commands = CleanupCommands.commandsFor(
                    setOf(CleanupOption.TEMP, CleanupOption.DOWNLOADS, CleanupOption.APP_CACHE),
                )
                assertEquals(expected, commands)
            }
        }
    }

    @Test
    fun `commands do not use fragile wildcard cleanup`() {
        val commands = CleanupCommands.commandsFor(
            setOf(CleanupOption.TEMP, CleanupOption.DOWNLOADS, CleanupOption.APP_CACHE),
        )

        assertFalse(commands[0].contains("/data/local/tmp/*"))
        assertFalse(commands[1].contains("/sdcard/Download/*"))
        assertTrue(commands[2].contains("find /sdcard/Android/data/*/cache"))
    }

    @Test
    fun `cleanup commands avoid shell control flow tokens that break on some devices`() {
        val commands = CleanupCommands.commandsFor(
            setOf(CleanupOption.TEMP, CleanupOption.DOWNLOADS, CleanupOption.APP_CACHE),
        )

        commands.forEach { command ->
            assertFalse(command.contains("; then"))
            assertFalse(command.contains("; do"))
        }
    }

    @Test
    fun `app cache command does not depend on loop variable escaping`() {
        val command = CleanupCommands.commandsFor(setOf(CleanupOption.APP_CACHE)).single()

        assertFalse(command.contains("\$d"))
        assertTrue(command.contains("-exec rm -rf {} +"))
    }

    private fun withSystemProperty(
        name: String,
        value: String,
        block: () -> Unit,
    ) {
        val original = System.getProperty(name)
        try {
            System.setProperty(name, value)
            block()
        } finally {
            if (original == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, original)
            }
        }
    }

    private companion object {
        private const val TEMP_COMMAND =
            "find /data/local/tmp -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"

        private const val DOWNLOADS_COMMAND =
            "find /sdcard/Download -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"

        private const val APP_CACHE_COMMAND =
            "find /sdcard/Android/data/*/cache -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"
    }
}
