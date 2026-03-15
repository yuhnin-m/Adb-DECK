package com.adbdeck.core.adb.impl.files

import com.adbdeck.core.process.ProcessResult
import com.adbdeck.core.process.ProcessRunner
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemDeviceFileClientCommandQuotingTest {

    @Test
    fun `listDirectory builds stable sh payload on mac windows linux and keeps stat formats quoted`() {
        val processRunner = RecordingProcessRunner()
        val client = SystemDeviceFileClient(processRunner)
        val adbPath = "C:\\Program Files\\Android\\platform-tools\\adb.exe"
        val deviceId = "59291JEBF12529"
        val directoryPath = "/sdcard/QA O'Reilly \"alpha\" \$beta `date` \\tmp"

        val osNames = listOf("Mac OS X", "Windows 11", "Linux")
        osNames.forEach { osName ->
            withSystemProperty("os.name", osName) {
                processRunner.clear()

                val result = runBlocking {
                    client.listDirectory(
                        deviceId = deviceId,
                        directoryPath = directoryPath,
                        adbPath = adbPath,
                    )
                }

                assertTrue(result.isSuccess)
                val command = processRunner.singleCommand()
                assertEquals(listOf(adbPath, "-s", deviceId, "shell", "sh", "-c"), command.take(6))
                assertEquals(7, command.size)

                val script = unwrapSingleQuotedPayload(command[6])
                assertTrue(script.startsWith("dir=${shellDoubleQuotedLiteral(directoryPath)}"))
                assertFalse(script.contains("\$1"))
                assertTrue(script.contains("probe=\$(ls -ld \"\$dir\" 2>&1)"))
                assertTrue(script.contains("toybox stat -c \"%s|%Y\" \"\$entry\""))
                assertTrue(script.contains("stat -f \"%z|%m\" \"\$entry\""))
            }
        }
    }

    @Test
    fun `exists command interpolates path safely without positional placeholders`() {
        val processRunner = RecordingProcessRunner()
        val client = SystemDeviceFileClient(processRunner)
        val adbPath = "/opt/platform-tools/adb"
        val deviceId = "emulator-5554"
        val path = "/sdcard/it's \"quoted\" \$HOME `uname` \\folder"

        val result = runBlocking {
            client.exists(
                deviceId = deviceId,
                path = path,
                adbPath = adbPath,
            )
        }

        assertTrue(result.isSuccess)
        val command = processRunner.singleCommand()
        assertEquals(listOf(adbPath, "-s", deviceId, "shell", "sh", "-c"), command.take(6))

        val script = unwrapSingleQuotedPayload(command[6])
        assertEquals("[ -e ${shellDoubleQuotedLiteral(path)} ]", script)
        assertFalse(script.contains("\$1"))
    }

    @Test
    fun `create delete and rename commands escape each operand independently`() {
        val processRunner = RecordingProcessRunner()
        val client = SystemDeviceFileClient(processRunner)
        val adbPath = "/opt/platform-tools/adb"
        val deviceId = "emulator-5554"

        val createPath = "/sdcard/New Folder/O'Reilly \"Draft\""
        val deletePath = "/sdcard/Trash/\$old`name`"
        val sourcePath = "/sdcard/From/it's source.txt"
        val targetPath = "/sdcard/To/new \"target\".txt"

        runBlocking {
            assertTrue(client.createDirectory(deviceId, createPath, adbPath).isSuccess)
            assertTrue(client.delete(deviceId, deletePath, recursive = true, adbPath = adbPath).isSuccess)
            assertTrue(client.delete(deviceId, deletePath, recursive = false, adbPath = adbPath).isSuccess)
            assertTrue(client.rename(deviceId, sourcePath, targetPath, adbPath).isSuccess)
        }

        assertEquals(4, processRunner.commands.size)

        assertEquals(
            "mkdir -p ${shellDoubleQuotedLiteral(createPath)}",
            unwrapSingleQuotedPayload(processRunner.commands[0][6]),
        )
        assertEquals(
            "rm -rf -- ${shellDoubleQuotedLiteral(deletePath)}",
            unwrapSingleQuotedPayload(processRunner.commands[1][6]),
        )
        assertEquals(
            "rm -f -- ${shellDoubleQuotedLiteral(deletePath)}",
            unwrapSingleQuotedPayload(processRunner.commands[2][6]),
        )
        assertEquals(
            "mv ${shellDoubleQuotedLiteral(sourcePath)} ${shellDoubleQuotedLiteral(targetPath)}",
            unwrapSingleQuotedPayload(processRunner.commands[3][6]),
        )
    }

    @Test
    fun `push and pull keep host and device paths as raw adb arguments`() {
        val processRunner = RecordingProcessRunner()
        val client = SystemDeviceFileClient(processRunner)
        val adbPath = "C:\\platform-tools\\adb.exe"
        val deviceId = "59291JEBF12529"
        val localPushPath = "C:\\Users\\Mike\\Desktop\\Builds\\My App \"debug\" #1.apk"
        val remotePushPath = "/sdcard/Download/My App \"debug\" #1.apk"
        val localPullPath = "C:\\Users\\Mike\\Desktop\\Device Dump\\primary"

        runBlocking {
            assertTrue(client.push(deviceId, localPushPath, remotePushPath, adbPath).isSuccess)
            assertTrue(client.pull(deviceId, remotePushPath, localPullPath, adbPath).isSuccess)
        }

        assertEquals(2, processRunner.commands.size)
        assertEquals(
            listOf(adbPath, "-s", deviceId, "push", localPushPath, remotePushPath),
            processRunner.commands[0],
        )
        assertEquals(
            listOf(adbPath, "-s", deviceId, "pull", remotePushPath, localPullPath),
            processRunner.commands[1],
        )
    }

    private fun unwrapSingleQuotedPayload(value: String): String {
        assertTrue(value.length >= 2 && value.first() == '\'' && value.last() == '\'')
        return value.substring(1, value.length - 1).replace("'\"'\"'", "'")
    }

    private fun shellDoubleQuotedLiteral(value: String): String =
        "\"" + buildString(value.length + 8) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\$")
                    '`' -> append("\\`")
                    else -> append(ch)
                }
            }
        } + "\""

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

    private class RecordingProcessRunner : ProcessRunner {
        val commands: MutableList<List<String>> = mutableListOf()

        override suspend fun run(command: List<String>, timeoutMs: Long): ProcessResult {
            commands += command
            return ProcessResult(
                exitCode = 0,
                stdout = "",
                stderr = "",
            )
        }

        fun clear() {
            commands.clear()
        }

        fun singleCommand(): List<String> {
            assertEquals(1, commands.size)
            return commands.single()
        }
    }
}
