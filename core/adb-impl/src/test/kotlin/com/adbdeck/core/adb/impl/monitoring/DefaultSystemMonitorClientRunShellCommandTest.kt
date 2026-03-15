package com.adbdeck.core.adb.impl.monitoring

import com.adbdeck.core.process.ProcessResult
import com.adbdeck.core.process.ProcessRunner
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultSystemMonitorClientRunShellCommandTest {

    @Test
    fun `run shell command keeps full sh -c payload intact on mac windows linux`() {
        val processRunner = RecordingProcessRunner()
        val client = DefaultSystemMonitorClient(processRunner)
        val adbPath = "/opt/platform-tools/adb"
        val deviceId = "emulator-5554"
        val shellCommand =
            "find /sdcard/Android/data/*/cache -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true"

        val osNames = listOf("Mac OS X", "Windows 11", "Linux")
        osNames.forEach { osName ->
            withSystemProperty("os.name", osName) {
                processRunner.clear()

                val result = runBlocking {
                    client.runShellCommand(
                        deviceId = deviceId,
                        shellCommand = shellCommand,
                        adbPath = adbPath,
                    )
                }

                assertTrue(result.isSuccess)
                assertEquals(1, processRunner.commands.size)
                assertEquals(
                    listOf(adbPath, "-s", deviceId, "shell", "sh", "-c", shellCommand),
                    processRunner.commands.single(),
                )
            }
        }
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
    }
}
