package com.adbdeck.core.adb.impl.packages

import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.packages.PackageDetails
import com.adbdeck.core.adb.api.packages.PackageType
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [com.adbdeck.core.adb.api.packages.PackageClient] поверх системного `adb`.
 *
 * Все операции делегируются [com.adbdeck.core.process.ProcessRunner], который запускает процессы и
 * возвращает stdout/stderr. I/O выполняется в потоке Dispatchers.IO (внутри ProcessRunner).
 *
 * @param processRunner Абстракция запуска процессов.
 */
class SystemPackageClient(
    private val processRunner: ProcessRunner,
) : PackageClient {

    // ── Список пакетов ─────────────────────────────────────────────────────────

    /**
     * Запускает `pm list packages -f` и парсит вывод вида:
     * ```
     * package:/data/app/~~hash==/com.example.app-1/base.apk=com.example.app
     * ```
     * Пакеты сортируются по имени.
     */
    override suspend fun getPackages(
        deviceId: String,
        adbPath: String,
    ): Result<List<AppPackage>> = runCatchingPreserveCancellation {
        val listResult = processRunner.run(adbPath, "-s", deviceId, "shell", "pm", "list", "packages", "-f")
        if (!listResult.isSuccess && listResult.stdout.isBlank()) {
            error("Не удалось получить список пакетов: ${listResult.stderr.take(200)}")
        }

        val basePackages = parsePackageList(listResult.stdout)

        // Best-effort: дополнительно получаем отключенные пакеты (`pm list packages -d`),
        // чтобы корректно выставлять AppPackage.isEnabled в списке.
        val disabledResult = processRunner.run(
            adbPath,
            "-s",
            deviceId,
            "shell",
            "pm",
            "list",
            "packages",
            "-d",
        )
        val disabledPackages = if (disabledResult.isSuccess || disabledResult.stdout.isNotBlank()) {
            parsePackageNames(disabledResult.stdout)
        } else {
            emptySet()
        }

        basePackages.map { pkg ->
            pkg.copy(isEnabled = pkg.packageName !in disabledPackages)
        }
    }

    /** Разбирает вывод `pm list packages -f` в список [AppPackage]. */
    private fun parsePackageList(output: String): List<AppPackage> =
        output.lineSequence()
            .filter { it.startsWith("package:") }
            .mapNotNull { line ->
                // Формат: "package:<apkPath>=<packageName>"
                // Используем lastIndexOf, т.к. путь APK тоже может содержать '='
                val withoutPrefix = line.removePrefix("package:")
                val eqIndex = withoutPrefix.lastIndexOf('=')
                if (eqIndex < 0) return@mapNotNull null

                val apkPath = withoutPrefix.substring(0, eqIndex).trim()
                val packageName = withoutPrefix.substring(eqIndex + 1).trim()
                if (packageName.isBlank()) return@mapNotNull null

                AppPackage(
                    packageName = packageName,
                    apkPath = apkPath,
                    type = apkPathToPackageType(apkPath),
                )
            }
            .sortedBy { it.packageName }
            .toList()

    /**
     * Разбирает вывод `pm list packages` в множество имен пакетов.
     *
     * Поддерживает форматы:
     * - `package:com.example.app`
     * - `package:/data/app/.../base.apk=com.example.app`
     */
    private fun parsePackageNames(output: String): Set<String> =
        output.lineSequence()
            .filter { it.startsWith("package:") }
            .mapNotNull { line ->
                val value = line.removePrefix("package:").trim()
                if (value.isBlank()) return@mapNotNull null

                if ('=' in value) {
                    value.substringAfterLast('=').trim().takeIf { it.isNotBlank() }
                } else {
                    value.takeIf { it.isNotBlank() }
                }
            }
            .toSet()

    /**
     * Определяет тип пакета по пути APK.
     * Системные приложения располагаются в `/system`, `/product`, `/vendor`, `/odm`, `/oem`.
     */
    private fun apkPathToPackageType(apkPath: String): PackageType {
        val systemPrefixes = listOf("/system", "/product", "/vendor", "/odm", "/oem", "/priv-app")
        return if (systemPrefixes.any { apkPath.startsWith(it) }) PackageType.SYSTEM
        else PackageType.USER
    }

    // ── Детали пакета ──────────────────────────────────────────────────────────

    /**
     * Запускает `dumpsys package <packageName>` и парсит ключевые поля.
     *
     * Поля версии, SDK находятся в одной строке:
     * `versionCode=100 minSdk=21 targetSdk=34`
     *
     * Флаги: `flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA DEBUGGABLE ]`
     *
     * Runtime-разрешения: секция `runtimePermissions:` с парами `<perm>: granted=<bool>`.
     */
    override suspend fun getPackageDetails(
        deviceId: String,
        packageName: String,
        adbPath: String,
    ): Result<PackageDetails> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "shell", "dumpsys", "package", packageName)
        if (!result.isSuccess && result.stdout.isBlank()) {
            error("Не удалось получить информацию о пакете '$packageName': ${result.stderr.take(200)}")
        }
        parseDumpsysOutput(packageName, result.stdout)
    }

    /** Разбирает вывод `dumpsys package <pkg>` в [PackageDetails]. */
    private fun parseDumpsysOutput(packageName: String, output: String): PackageDetails {
        val lines = output.lines()

        // ── Вспомогательные функции ──────────────────────────────
        /** Найти первую строку, начинающуюся с [prefix] (после trim). */
        fun firstLine(prefix: String): String? =
            lines.firstOrNull { it.trim().startsWith(prefix) }?.trim()

        /** Извлечь значение после [prefix] в первой совпадающей строке. */
        fun firstValue(prefix: String): String =
            firstLine(prefix)?.removePrefix(prefix)?.trim() ?: ""

        // ── Версия / SDK ─────────────────────────────────────────
        // Строка: "    versionCode=100 minSdk=21 targetSdk=34"
        val versionLine = firstLine("versionCode=")
        val versionCode = versionLine?.let { Regex("""versionCode=(\d+)""").find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0L
        val minSdk = versionLine?.let { Regex("""minSdk=(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0
        val targetSdk = versionLine?.let { Regex("""targetSdk=(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0

        val versionName = firstValue("versionName=")

        // ── Идентификатор пользователя ───────────────────────────
        val uid = firstValue("userId=").toIntOrNull() ?: 0

        // ── Пути ─────────────────────────────────────────────────
        val codePath = firstValue("codePath=")
        val dataDir = firstValue("dataDir=")
        val nativeLibPath = firstValue("legacyNativeLibraryDir=")
            .ifBlank { firstValue("nativeLibraryDir=") }

        // ── Временные метки ───────────────────────────────────────
        val firstInstallTime = firstValue("firstInstallTime=")
        val lastUpdateTime = firstValue("lastUpdateTime=")

        // ── Флаги ────────────────────────────────────────────────
        // Строка: "    flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA DEBUGGABLE ]"
        val flagsLine = firstLine("flags=[")
        val isDebuggable = flagsLine?.contains("DEBUGGABLE") == true
        val isSystemFromFlags = flagsLine?.contains(" SYSTEM") == true

        // privateFlags: "    privateFlags=[ PRIVATE_FLAG_PRIVILEGED ... ]"
        val privateFlagsLine = firstLine("privateFlags=[")
        val isSystemFromPrivate = privateFlagsLine?.contains("PRIVILEGED") == true

        val isSystem = isSystemFromFlags || isSystemFromPrivate

        // ── Enabled ───────────────────────────────────────────────
        // "    enabled=true" / "    enabled=false"
        val enabledValue = firstValue("enabled=")
        val isEnabled = enabledValue != "false"  // по умолчанию включён

        // ── Suspended ─────────────────────────────────────────────
        val suspendedLine = firstLine("suspended=")
        val isSuspended = suspendedLine?.contains("suspended=true") == true

        // ── Launcher activity ─────────────────────────────────────
        // Попытка найти главную Activity для запуска в секции Activities
        val launcherActivity = extractLauncherActivity(lines, packageName)

        // ── Runtime permissions ───────────────────────────────────
        val runtimePermissions = parseRuntimePermissions(lines)

        // ── Метка приложения (best-effort) ────────────────────────
        // Иногда присутствует в виде: "      label=MyApp"
        val appLabel = firstValue("label=").takeIf { it.isNotBlank() && it != "null" } ?: ""

        return PackageDetails(
            packageName = packageName,
            appLabel = appLabel,
            versionName = versionName,
            versionCode = versionCode,
            uid = uid,
            codePath = codePath,
            dataDir = dataDir,
            nativeLibPath = nativeLibPath,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            targetSdk = targetSdk,
            minSdk = minSdk,
            isSystem = isSystem,
            isEnabled = isEnabled,
            isDebuggable = isDebuggable,
            isSuspended = isSuspended,
            launcherActivity = launcherActivity,
            runtimePermissions = runtimePermissions,
        )
    }

    /**
     * Ищет главную launcher-активность в секции Activities вывода dumpsys.
     *
     * Ищет строку с `android.intent.action.MAIN` / `android.intent.category.LAUNCHER`
     * или просто Activity с именем, похожим на лончер (например `MainActivity`).
     */
    private fun extractLauncherActivity(lines: List<String>, packageName: String): String {
        // Ищем строки вида: "        <packageName>/<ActivityName>"
        // в секции, следующей после "Activity Resolver Table:"
        val activityPattern = Regex("""^\s+${Regex.escape(packageName)}/([.\w]+)\s*$""")
        return lines
            .firstOrNull { activityPattern.containsMatchIn(it) }
            ?.trim()
            ?: ""
    }

    /**
     * Парсит секцию `runtimePermissions:` вывода dumpsys.
     *
     * Формат строк:
     * ```
     *   android.permission.READ_EXTERNAL_STORAGE: granted=true, flags=[ USER_SET]
     *   android.permission.CAMERA: granted=false, flags=[ USER_SET]
     * ```
     */
    private fun parseRuntimePermissions(lines: List<String>): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        var inRuntimeSection = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                // Начало секции runtime permissions
                trimmed == "runtime permissions:" || trimmed.startsWith("runtimePermissions:") -> {
                    inRuntimeSection = true
                }
                // Конец секции — новая секция на том же или меньшем уровне отступа
                inRuntimeSection && !line.startsWith("      ") && !line.startsWith("\t\t") && trimmed.isNotEmpty() && !trimmed.startsWith("android.") -> {
                    inRuntimeSection = false
                }
                // Строка с разрешением внутри секции
                inRuntimeSection && trimmed.contains("granted=") -> {
                    // Формат: "android.permission.NAME: granted=true, ..."
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val permName = trimmed.substring(0, colonIdx).trim()
                        val granted = trimmed.contains("granted=true")
                        if (permName.startsWith("android.permission.") || permName.contains(".permission.")) {
                            result[permName] = granted
                        }
                    }
                }
            }
        }
        return result
    }

    // ── Действия с пакетами ────────────────────────────────────────────────────

    /**
     * Запускает приложение через `monkey` с LAUNCHER intent.
     * monkey надёжнее `am start`, т.к. не требует знать имя Activity.
     */
    override suspend fun launchApp(
        deviceId: String,
        packageName: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell",
            "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1",
        )
        // monkey выводит "Events injected: 1" при успехе
        // При ошибке выводит "Error: ..."
        if (result.stdout.contains("Error:", ignoreCase = false) ||
            result.stdout.contains("does not have a launcher", ignoreCase = true)
        ) {
            error(result.stdout.trim().take(200))
        }
    }

    /** Принудительно останавливает приложение: `am force-stop <pkg>`. */
    override suspend fun forceStop(
        deviceId: String,
        packageName: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "shell", "am", "force-stop", packageName)
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось остановить приложение" })
        }
    }

    /** Очищает данные приложения: `pm clear <pkg>`. Успех — вывод содержит `Success`. */
    override suspend fun clearData(
        deviceId: String,
        packageName: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "shell", "pm", "clear", packageName)
        if (!result.stdout.contains("Success")) {
            error(result.stdout.ifBlank { result.stderr }.trim().take(200))
        }
    }

    /**
     * Удаляет приложение: `pm uninstall [--keep-data] <pkg>`.
     * Успех — вывод содержит `Success`.
     */
    override suspend fun uninstall(
        deviceId: String,
        packageName: String,
        keepData: Boolean,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val cmd = buildList {
            add(adbPath); add("-s"); add(deviceId)
            add("shell"); add("pm"); add("uninstall")
            if (keepData) add("-k")
            add(packageName)
        }
        val result = processRunner.run(cmd)
        if (!result.stdout.contains("Success")) {
            val msg = result.stdout.ifBlank { result.stderr }.trim()
            error(msg.ifBlank { "Не удалось удалить пакет '$packageName'" })
        }
    }

    /** Открывает системный экран «Информация о приложении» через Intent. */
    override suspend fun openAppInfo(
        deviceId: String,
        packageName: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell",
            "am", "start",
            "-a", "android.settings.APPLICATION_DETAILS_SETTINGS",
            "-d", "package:$packageName",
        )
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось открыть информацию о приложении" })
        }
    }

    /**
     * Выгружает base APK выбранного пакета на хост.
     *
     * Шаги:
     * 1. `pm path <packageName>` -> получает путь вида `package:/data/app/.../base.apk`.
     * 2. `adb pull <remoteApkPath> <localPath>`.
     */
    override suspend fun exportBaseApk(
        deviceId: String,
        packageName: String,
        localPath: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val pathResult = processRunner.run(
            adbPath,
            "-s",
            deviceId,
            "shell",
            "pm",
            "path",
            packageName,
        )
        if (!pathResult.isSuccess && pathResult.stdout.isBlank()) {
            error(pathResult.stderr.ifBlank { "Не удалось получить путь APK для '$packageName'" })
        }

        val remoteApkPath = pathResult.stdout
            .lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("package:") }
            ?.removePrefix("package:")
            ?.trim()
            .orEmpty()

        if (remoteApkPath.isBlank()) {
            error("Не найден base APK для '$packageName'")
        }

        val pullResult = processRunner.run(
            adbPath,
            "-s",
            deviceId,
            "pull",
            remoteApkPath,
            localPath,
        )
        if (!pullResult.isSuccess) {
            error(pullResult.stderr.ifBlank { pullResult.stdout }.ifBlank { "Не удалось выгрузить APK" })
        }
    }

    /** Выдаёт runtime-разрешение: `pm grant <pkg> <perm>`. */
    override suspend fun grantPermission(
        deviceId: String,
        packageName: String,
        permission: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell", "pm", "grant", packageName, permission,
        )
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось выдать разрешение $permission" })
        }
    }

    /** Отзывает runtime-разрешение: `pm revoke <pkg> <perm>`. */
    override suspend fun revokePermission(
        deviceId: String,
        packageName: String,
        permission: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell", "pm", "revoke", packageName, permission,
        )
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось отозвать разрешение $permission" })
        }
    }
}
