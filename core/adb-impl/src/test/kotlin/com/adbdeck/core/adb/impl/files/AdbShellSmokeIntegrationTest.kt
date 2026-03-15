package com.adbdeck.core.adb.impl.files

import com.adbdeck.core.adb.api.files.DeviceFileEntry
import com.adbdeck.core.process.SystemProcessRunner
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdbShellSmokeIntegrationTest {

    @Test
    fun `adb shell smoke create list rename delete with complex paths`() = runBlocking {
        assertTrue(
            java.lang.Boolean.getBoolean(INTEGRATION_ENABLED_PROP),
            "Integration smoke test is disabled. Use :core:adb-impl:adbIntegrationTest.",
        )

        val adbPath = resolveAdbPath()
        val processRunner = SystemProcessRunner()
        val client = SystemDeviceFileClient(processRunner)
        val deviceId = resolveDeviceId(adbPath, processRunner)

        val suffix = System.currentTimeMillis().toString()
        val basePath = "/data/local/tmp/adbdeck_smoke_$suffix"
        val childName = "child 'single' \"double\" \$money `tick`"
        val renamedName = "renamed #1 'ok' \"safe\" \$value `new`"
        val childPath = "$basePath/$childName"
        val renamedPath = "$basePath/$renamedName"

        // Cleanup "на входе" на случай остатков от предыдущего падения.
        client.delete(deviceId, basePath, recursive = true, adbPath = adbPath)

        try {
            client.createDirectory(deviceId, basePath, adbPath).getOrThrowWithContext("create base dir")
            client.createDirectory(deviceId, childPath, adbPath).getOrThrowWithContext("create child dir")

            val listAfterCreate = client.listDirectory(deviceId, basePath, adbPath)
                .getOrThrowWithContext("list after create")
            assertContainsDirectory(listAfterCreate, childName)

            client.rename(deviceId, childPath, renamedPath, adbPath).getOrThrowWithContext("rename dir")

            val oldExists = client.exists(deviceId, childPath, adbPath).getOrThrowWithContext("exists old path")
            val newExists = client.exists(deviceId, renamedPath, adbPath).getOrThrowWithContext("exists renamed path")
            assertFalse(oldExists, "Source path must not exist after rename: $childPath")
            assertTrue(newExists, "Target path must exist after rename: $renamedPath")

            val listAfterRename = client.listDirectory(deviceId, basePath, adbPath)
                .getOrThrowWithContext("list after rename")
            assertContainsDirectory(listAfterRename, renamedName)

            client.delete(deviceId, basePath, recursive = true, adbPath = adbPath).getOrThrowWithContext("delete base dir")
            val baseExists = client.exists(deviceId, basePath, adbPath).getOrThrowWithContext("exists base after delete")
            assertFalse(baseExists, "Base path must be deleted: $basePath")
        } finally {
            // Cleanup "на выходе", чтобы не оставлять мусор на устройстве.
            client.delete(deviceId, basePath, recursive = true, adbPath = adbPath)
        }
    }

    private fun assertContainsDirectory(
        entries: List<DeviceFileEntry>,
        expectedName: String,
    ) {
        val names = entries.filter { it.isDirectory }.map { it.name }
        assertTrue(
            expectedName in names,
            "Expected directory '$expectedName' in listing, got: $names",
        )
    }

    private fun resolveAdbPath(): String =
        System.getenv("ADB_PATH")
            ?.takeIf { it.isNotBlank() }
            ?: "adb"

    private suspend fun resolveDeviceId(
        adbPath: String,
        processRunner: SystemProcessRunner,
    ): String {
        val explicitId = System.getenv("ADB_DEVICE_ID")?.takeIf { it.isNotBlank() }
        if (explicitId != null) {
            val stateResult = processRunner.run(adbPath, "-s", explicitId, "get-state")
            assertTrue(stateResult.isSuccess, "ADB device '$explicitId' is not available. stderr=${stateResult.stderr}")
            assertTrue(stateResult.stdout.trim().contains("device"), "ADB device '$explicitId' is not in 'device' state.")
            return explicitId
        }

        val devicesResult = processRunner.run(adbPath, "devices")
        assertTrue(devicesResult.isSuccess, "Failed to query adb devices. stderr=${devicesResult.stderr}")

        val onlineDevices = devicesResult.stdout
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("List of devices attached") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                val id = parts.getOrNull(0)
                val state = parts.getOrNull(1)
                if (id.isNullOrBlank() || state != "device") null else id
            }
            .toList()

        assertTrue(
            onlineDevices.isNotEmpty(),
            "No connected adb device/emulator in 'device' state. adb devices output:\n${devicesResult.stdout}",
        )
        return onlineDevices.first()
    }

    private fun <T> Result<T>.getOrThrowWithContext(step: String): T =
        getOrElse { throwable ->
            throw AssertionError("$step failed: ${throwable.message}", throwable)
        }

    private companion object {
        private const val INTEGRATION_ENABLED_PROP = "adb.integration.enabled"
    }
}
