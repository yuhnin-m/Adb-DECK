package com.adbdeck.core.adb.impl.intents

import com.adbdeck.core.adb.api.intents.DeepLinkParams
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.intents.IntentParams
import com.adbdeck.core.adb.api.intents.LaunchResult
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [com.adbdeck.core.adb.api.intents.IntentLaunchClient] на основе `adb shell am start`.
 *
 * Строит список аргументов и вызывает [com.adbdeck.core.process.ProcessRunner.run].
 * Команда-превью строится отдельно (через [buildList] + [joinToString])
 * для удобного отображения в UI, а выполнение — через корректный список
 * аргументов без shell-интерпретации.
 *
 * @param processRunner Интерфейс запуска внешних процессов.
 */
class SystemIntentLaunchClient(
    private val processRunner: ProcessRunner,
) : IntentLaunchClient {

    // ── Строители командной строки (для предпросмотра) ────────────────────────

    override fun buildDeepLinkCommand(
        deviceId: String,
        adbPath: String,
        params: DeepLinkParams,
    ): String = buildList {
        add("$adbPath -s $deviceId shell am start")
        if (params.action.isNotBlank()) add("-a ${params.action}")
        if (params.uri.isNotBlank()) add("-d \"${params.uri}\"")
        if (params.packageName.isNotBlank()) add("-p ${params.packageName}")
        if (params.component.isNotBlank()) add("-n ${params.component}")
        if (params.category.isNotBlank()) add("-c ${params.category}")
    }.joinToString(" ")

    override fun buildIntentCommand(
        deviceId: String,
        adbPath: String,
        params: IntentParams,
    ): String = buildList {
        add("$adbPath -s $deviceId shell am start")
        if (params.action.isNotBlank()) add("-a ${params.action}")
        if (params.dataUri.isNotBlank()) add("-d \"${params.dataUri}\"")
        if (params.packageName.isNotBlank()) add("-p ${params.packageName}")
        if (params.component.isNotBlank()) add("-n ${params.component}")
        params.categories.filter { it.isNotBlank() }.forEach { add("-c $it") }
        if (params.flags.isNotBlank()) add("-f ${params.flags}")
        params.extras
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .forEach { extra -> add("${extra.type.flag} ${extra.key} ${extra.value}") }
    }.joinToString(" ")

    // ── Запуск (аргументы передаются как список, без shell-раскрытия) ─────────

    override suspend fun launchDeepLink(
        deviceId: String,
        adbPath: String,
        params: DeepLinkParams,
    ): Result<LaunchResult> = runCatchingPreserveCancellation {
        val command = buildDeepLinkCommand(deviceId, adbPath, params)
        val result = processRunner.run(buildDeepLinkArgs(adbPath, deviceId, params))
        LaunchResult(
            exitCode = result.exitCode,
            stdout = result.stdout.trim(),
            stderr = result.stderr.trim(),
            commandPreview = command,
        )
    }

    override suspend fun launchIntent(
        deviceId: String,
        adbPath: String,
        params: IntentParams,
    ): Result<LaunchResult> = runCatchingPreserveCancellation {
        val command = buildIntentCommand(deviceId, adbPath, params)
        val result = processRunner.run(buildIntentArgs(adbPath, deviceId, params))
        LaunchResult(
            exitCode = result.exitCode,
            stdout = result.stdout.trim(),
            stderr = result.stderr.trim(),
            commandPreview = command,
        )
    }

    // ── Приватные построители списков аргументов ──────────────────────────────

    private fun buildDeepLinkArgs(
        adbPath: String,
        deviceId: String,
        params: DeepLinkParams,
    ): List<String> = buildList {
        add(adbPath); add("-s"); add(deviceId)
        add("shell"); add("am"); add("start")
        if (params.action.isNotBlank()) { add("-a"); add(params.action) }
        if (params.uri.isNotBlank()) { add("-d"); add(params.uri) }
        if (params.packageName.isNotBlank()) { add("-p"); add(params.packageName) }
        if (params.component.isNotBlank()) { add("-n"); add(params.component) }
        if (params.category.isNotBlank()) { add("-c"); add(params.category) }
    }

    private fun buildIntentArgs(
        adbPath: String,
        deviceId: String,
        params: IntentParams,
    ): List<String> = buildList {
        add(adbPath); add("-s"); add(deviceId)
        add("shell"); add("am"); add("start")
        if (params.action.isNotBlank()) { add("-a"); add(params.action) }
        if (params.dataUri.isNotBlank()) { add("-d"); add(params.dataUri) }
        if (params.packageName.isNotBlank()) { add("-p"); add(params.packageName) }
        if (params.component.isNotBlank()) { add("-n"); add(params.component) }
        params.categories.filter { it.isNotBlank() }.forEach { cat -> add("-c"); add(cat) }
        if (params.flags.isNotBlank()) { add("-f"); add(params.flags) }
        params.extras
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .forEach { extra -> add(extra.type.flag); add(extra.key); add(extra.value) }
    }
}
