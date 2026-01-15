package com.adbdeck.feature.quicktoggles.service

import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.feature.quicktoggles.ANIMATION_ANIMATOR_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_TRANSITION_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_WINDOW_SCALE_KEY
import com.adbdeck.feature.quicktoggles.QuickToggleId
import com.adbdeck.feature.quicktoggles.QuickToggleState
import com.adbdeck.feature.quicktoggles.approxEquals
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Реализация [QuickTogglesService] поверх [DeviceInfoClient.runShellCommand].
 */
class DefaultQuickTogglesService(
    private val deviceInfoClient: DeviceInfoClient,
) : QuickTogglesService {

    override suspend fun readStatuses(
        deviceId: String,
        adbPath: String,
    ): QuickToggleStatusSnapshot {
        val states = linkedMapOf<QuickToggleId, QuickToggleState>()
        val errors = linkedMapOf<QuickToggleId, String>()
        val animationValues = linkedMapOf<QuickToggleId, List<AnimationScaleValue>>()

        val results = coroutineScope {
            QuickToggleId.entries
                .map { toggleId ->
                    async {
                        toggleId to readStatus(
                            deviceId = deviceId,
                            adbPath = adbPath,
                            toggleId = toggleId,
                        )
                    }
                }
                .awaitAll()
        }

        results.forEach { (toggleId, result) ->
            states[toggleId] = result.state
            result.error?.takeIf { it.isNotBlank() }?.let { errors[toggleId] = it }
            if (result.animationValues.isNotEmpty()) {
                animationValues[toggleId] = result.animationValues
            }
        }

        return QuickToggleStatusSnapshot(
            states = states,
            readErrors = errors,
            animationValues = animationValues,
        )
    }

    override suspend fun readStatus(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
    ): QuickToggleReadResult {
        return when (toggleId) {
            QuickToggleId.WIFI -> readWifiStatus(deviceId, adbPath)
            QuickToggleId.MOBILE_DATA -> readMobileDataStatus(deviceId, adbPath)
            QuickToggleId.BLUETOOTH -> readBluetoothStatus(deviceId, adbPath)
            QuickToggleId.AIRPLANE_MODE -> readAirplaneModeStatus(deviceId, adbPath)
            QuickToggleId.ANIMATIONS -> readAnimationsStatus(deviceId, adbPath)
            QuickToggleId.STAY_AWAKE -> readStayAwakeStatus(deviceId, adbPath)
        }
    }

    override suspend fun setToggle(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
        targetState: QuickToggleState,
    ): Result<Unit> {
        if (targetState != QuickToggleState.ON && targetState != QuickToggleState.OFF) {
            return Result.failure(IllegalArgumentException("Unknown target state is not supported"))
        }

        return when (toggleId) {
            QuickToggleId.WIFI -> runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf("svc", "wifi", if (targetState == QuickToggleState.ON) "enable" else "disable"),
            )

            QuickToggleId.MOBILE_DATA -> runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf("svc", "data", if (targetState == QuickToggleState.ON) "enable" else "disable"),
            )

            QuickToggleId.BLUETOOTH -> runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf("svc", "bluetooth", if (targetState == QuickToggleState.ON) "enable" else "disable"),
            )

            QuickToggleId.AIRPLANE_MODE -> setAirplaneModeWithFallback(
                deviceId = deviceId,
                adbPath = adbPath,
                targetState = targetState,
            )

            QuickToggleId.ANIMATIONS -> setAnimationPreset(
                deviceId = deviceId,
                adbPath = adbPath,
                targetState = targetState,
            )

            QuickToggleId.STAY_AWAKE -> runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf(
                    "settings",
                    "put",
                    "global",
                    "stay_on_while_plugged_in",
                    if (targetState == QuickToggleState.ON) "3" else "0",
                ),
            )
        }
    }

    override suspend fun readAnimationScales(
        deviceId: String,
        adbPath: String,
    ): List<AnimationScaleValue> {
        return listOf(
            readSingleAnimationScale(deviceId, adbPath, ANIMATION_WINDOW_SCALE_KEY),
            readSingleAnimationScale(deviceId, adbPath, ANIMATION_TRANSITION_SCALE_KEY),
            readSingleAnimationScale(deviceId, adbPath, ANIMATION_ANIMATOR_SCALE_KEY),
        )
    }

    override suspend fun setAnimationScale(
        deviceId: String,
        adbPath: String,
        key: String,
        value: Float,
    ): Result<Unit> {
        val boundedValue = value.coerceIn(0f, 10f)
        val formattedValue = formatAnimationValue(boundedValue)
        return runShellRequireSuccess(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("settings", "put", "global", key, formattedValue),
        )
    }

    override suspend fun openSettings(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
    ): Result<Unit> {
        val intentAction = when (toggleId) {
            QuickToggleId.WIFI -> "android.settings.WIFI_SETTINGS"
            QuickToggleId.MOBILE_DATA -> "android.settings.DATA_ROAMING_SETTINGS"
            QuickToggleId.BLUETOOTH -> "android.settings.BLUETOOTH_SETTINGS"
            QuickToggleId.AIRPLANE_MODE -> "android.settings.WIRELESS_SETTINGS"
            QuickToggleId.ANIMATIONS,
            QuickToggleId.STAY_AWAKE,
            -> "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"
        }

        return runShellRequireSuccess(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("am", "start", "-a", intentAction),
        )
    }

    private suspend fun readWifiStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        var lastError: String? = null

        runShell(deviceId, adbPath, "cmd", "wifi", "status").fold(
            onSuccess = { output ->
                parseWifiState(output)?.let { return QuickToggleReadResult(it) }
            },
            onFailure = { error ->
                lastError = error.message
            },
        )

        runShell(deviceId, adbPath, "dumpsys", "wifi").fold(
            onSuccess = { output ->
                parseWifiState(output)?.let { return QuickToggleReadResult(it) }
            },
            onFailure = { error ->
                lastError = lastError ?: error.message
            },
        )

        return QuickToggleReadResult(
            state = QuickToggleState.UNKNOWN,
            error = lastError ?: "Wi-Fi status is unavailable",
        )
    }

    private suspend fun readMobileDataStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        var lastError: String? = null

        runShell(deviceId, adbPath, "dumpsys", "connectivity").fold(
            onSuccess = { output ->
                parseMobileDataStateFromConnectivity(output)?.let { return QuickToggleReadResult(it) }
            },
            onFailure = { error ->
                lastError = error.message
            },
        )

        runShell(deviceId, adbPath, "settings", "get", "global", "mobile_data").fold(
            onSuccess = { output ->
                parseBooleanSwitch(output)?.let { enabled ->
                    return QuickToggleReadResult(if (enabled) QuickToggleState.ON else QuickToggleState.OFF)
                }
            },
            onFailure = { error ->
                lastError = lastError ?: error.message
            },
        )

        return QuickToggleReadResult(
            state = QuickToggleState.UNKNOWN,
            error = lastError ?: "Mobile data status is unavailable",
        )
    }

    private suspend fun readBluetoothStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        var lastError: String? = null

        runShell(deviceId, adbPath, "dumpsys", "bluetooth_manager").fold(
            onSuccess = { output ->
                parseBluetoothState(output)?.let { return QuickToggleReadResult(it) }
            },
            onFailure = { error ->
                lastError = error.message
            },
        )

        runShell(deviceId, adbPath, "dumpsys", "bluetooth").fold(
            onSuccess = { output ->
                parseBluetoothState(output)?.let { return QuickToggleReadResult(it) }
            },
            onFailure = { error ->
                lastError = lastError ?: error.message
            },
        )

        return QuickToggleReadResult(
            state = QuickToggleState.UNKNOWN,
            error = lastError ?: "Bluetooth status is unavailable",
        )
    }

    private suspend fun readAirplaneModeStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        val result = runShell(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("settings", "get", "global", "airplane_mode_on"),
        )
        return result.fold(
            onSuccess = { output ->
                parseBooleanSwitch(output)?.let { enabled ->
                    QuickToggleReadResult(if (enabled) QuickToggleState.ON else QuickToggleState.OFF)
                } ?: QuickToggleReadResult(
                    state = QuickToggleState.UNKNOWN,
                    error = "Airplane mode status is unavailable",
                )
            },
            onFailure = { error ->
                QuickToggleReadResult(
                    state = QuickToggleState.UNKNOWN,
                    error = error.message,
                )
            },
        )
    }

    private suspend fun readAnimationsStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        val values = readAnimationScales(
            deviceId = deviceId,
            adbPath = adbPath,
        )

        val firstError = values.firstOrNull { !it.error.isNullOrBlank() }?.error
        val state = when {
            firstError != null -> QuickToggleState.UNKNOWN
            values.all { it.value != null && it.value.approxEquals(0f) } -> QuickToggleState.OFF
            values.all { it.value != null && it.value.approxEquals(1f) } -> QuickToggleState.ON
            else -> QuickToggleState.CUSTOM
        }

        return QuickToggleReadResult(
            state = state,
            error = firstError,
            animationValues = values,
        )
    }

    private suspend fun readSingleAnimationScale(
        deviceId: String,
        adbPath: String,
        key: String,
    ): AnimationScaleValue {
        val result = runShell(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("settings", "get", "global", key),
        )

        return result.fold(
            onSuccess = { raw ->
                val value = raw.trim()
                when {
                    value.isBlank() || value.equals("null", ignoreCase = true) -> {
                        AnimationScaleValue(
                            key = key,
                            value = null,
                        )
                    }

                    looksLikeShellError(value) -> {
                        AnimationScaleValue(
                            key = key,
                            value = null,
                            error = value,
                        )
                    }

                    else -> {
                        val parsed = value.toFloatOrNull()
                        if (parsed != null) {
                            AnimationScaleValue(
                                key = key,
                                value = parsed,
                            )
                        } else {
                            AnimationScaleValue(
                                key = key,
                                value = null,
                                error = "Unexpected value: $value",
                            )
                        }
                    }
                }
            },
            onFailure = { error ->
                AnimationScaleValue(
                    key = key,
                    value = null,
                    error = error.message ?: "$key is unavailable",
                )
            },
        )
    }

    private suspend fun readStayAwakeStatus(
        deviceId: String,
        adbPath: String,
    ): QuickToggleReadResult {
        val result = runShell(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("settings", "get", "global", "stay_on_while_plugged_in"),
        )
        return result.fold(
            onSuccess = { output ->
                output.trim().toIntOrNull()?.let { numeric ->
                    QuickToggleReadResult(
                        if (numeric > 0) QuickToggleState.ON else QuickToggleState.OFF,
                    )
                } ?: QuickToggleReadResult(
                    state = QuickToggleState.UNKNOWN,
                    error = "Stay-awake status is unavailable",
                )
            },
            onFailure = { error ->
                QuickToggleReadResult(
                    state = QuickToggleState.UNKNOWN,
                    error = error.message,
                )
            },
        )
    }

    private suspend fun setAirplaneModeWithFallback(
        deviceId: String,
        adbPath: String,
        targetState: QuickToggleState,
    ): Result<Unit> {
        val enable = targetState == QuickToggleState.ON
        val primary = runShellRequireSuccess(
            deviceId = deviceId,
            adbPath = adbPath,
            command = listOf("cmd", "connectivity", "airplane-mode", if (enable) "enable" else "disable"),
        )
        if (primary.isSuccess) return primary

        val fallback = runCatching {
            runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf("settings", "put", "global", "airplane_mode_on", if (enable) "1" else "0"),
            ).getOrThrow()

            runShellRequireSuccess(
                deviceId = deviceId,
                adbPath = adbPath,
                command = listOf(
                    "am",
                    "broadcast",
                    "-a",
                    "android.intent.action.AIRPLANE_MODE",
                    "--ez",
                    "state",
                    if (enable) "true" else "false",
                ),
            ).getOrThrow()
        }

        if (fallback.isSuccess) return Result.success(Unit)

        val message = listOfNotNull(
            primary.exceptionOrNull()?.message,
            fallback.exceptionOrNull()?.message,
        ).joinToString(separator = " | ")
            .ifBlank { "Failed to change airplane mode" }
        return Result.failure(IllegalStateException(message))
    }

    private suspend fun setAnimationPreset(
        deviceId: String,
        adbPath: String,
        targetState: QuickToggleState,
    ): Result<Unit> {
        val value = if (targetState == QuickToggleState.ON) 1f else 0f
        return runCatching {
            setAnimationScale(deviceId, adbPath, ANIMATION_WINDOW_SCALE_KEY, value).getOrThrow()
            setAnimationScale(deviceId, adbPath, ANIMATION_TRANSITION_SCALE_KEY, value).getOrThrow()
            setAnimationScale(deviceId, adbPath, ANIMATION_ANIMATOR_SCALE_KEY, value).getOrThrow()
        }
    }

    private fun formatAnimationValue(value: Float): String {
        val rounded = (value * 10f).roundToInt() / 10f
        return if (rounded.approxEquals(rounded.toInt().toFloat())) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    private suspend fun runShellRequireSuccess(
        deviceId: String,
        adbPath: String,
        command: List<String>,
    ): Result<Unit> {
        return runShell(deviceId = deviceId, adbPath = adbPath, command = command)
            .mapCatching { output ->
                if (looksLikeShellError(output)) {
                    error(output.ifBlank { "Command failed: ${command.joinToString(" ")}" })
                }
            }
    }

    private suspend fun runShell(
        deviceId: String,
        adbPath: String,
        vararg command: String,
    ): Result<String> = runShell(deviceId, adbPath, command.toList())

    private suspend fun runShell(
        deviceId: String,
        adbPath: String,
        command: List<String>,
    ): Result<String> {
        return deviceInfoClient.runShellCommand(
            deviceId = deviceId,
            command = command,
            adbPath = adbPath,
        ).map { it.trim() }
    }

    private fun parseBooleanSwitch(raw: String): Boolean? {
        val value = raw.trim().lowercase()
        return when (value) {
            "1", "true", "on", "enabled" -> true
            "0", "false", "off", "disabled" -> false
            else -> null
        }
    }

    private fun parseWifiState(raw: String): QuickToggleState? {
        val normalized = raw.lowercase()
        return when {
            normalized.contains("wi-fi is enabled") ||
                normalized.contains("wifi is enabled") ||
                WIFI_ENABLED_RE.containsMatchIn(raw) -> {
                QuickToggleState.ON
            }

            normalized.contains("wi-fi is disabled") ||
                normalized.contains("wifi is disabled") ||
                WIFI_DISABLED_RE.containsMatchIn(raw) -> {
                QuickToggleState.OFF
            }

            else -> null
        }
    }

    private fun parseMobileDataStateFromConnectivity(raw: String): QuickToggleState? {
        MOBILE_DATA_ENABLED_STATE_RE
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
            ?.let { state ->
                return when (state) {
                    "true", "on", "connected" -> QuickToggleState.ON
                    "false", "off", "disconnected" -> QuickToggleState.OFF
                    else -> null
                }
            }

        MOBILE_DATA_MDATA_ENABLED_RE
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { enabled ->
                return if (enabled.equals("true", ignoreCase = true)) {
                    QuickToggleState.ON
                } else {
                    QuickToggleState.OFF
                }
            }

        val normalized = raw.lowercase()
        return when {
            MOBILE_DATA_STATE_CONNECTED_RE.containsMatchIn(raw) ||
                DATA_CONNECTION_STATE_CONNECTED_RE.containsMatchIn(raw) -> {
                QuickToggleState.ON
            }

            normalized.contains("transport_cellular") && normalized.contains("connected") -> {
                QuickToggleState.ON
            }

            normalized.contains("transport_cellular") &&
                (normalized.contains("disconnected") || normalized.contains("suspended")) -> {
                QuickToggleState.OFF
            }

            else -> null
        }
    }

    private fun parseBluetoothState(raw: String): QuickToggleState? {
        val normalized = raw.lowercase()

        BLUETOOTH_ENABLED_RE
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { value ->
                return if (value.equals("true", ignoreCase = true)) {
                    QuickToggleState.ON
                } else {
                    QuickToggleState.OFF
                }
            }

        BLUETOOTH_STATE_RE
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { state ->
                return if (state.equals("on", ignoreCase = true) || state.equals("turning_on", ignoreCase = true)) {
                    QuickToggleState.ON
                } else {
                    QuickToggleState.OFF
                }
            }

        return when {
            normalized.contains("bluetooth is enabled") -> QuickToggleState.ON
            normalized.contains("bluetooth is disabled") -> QuickToggleState.OFF
            else -> null
        }
    }

    private fun looksLikeShellError(output: String): Boolean {
        val normalized = output.trim().lowercase()
        if (normalized.isEmpty()) return false
        if (normalized.startsWith("error")) return true
        if (normalized.contains("unknown command")) return true
        if (normalized.contains("not found")) return true
        if (normalized.contains("permission denied")) return true
        if (normalized.contains("security exception")) return true
        return false
    }

    private companion object {
        private val WIFI_ENABLED_RE = Regex("""mwifienabled\s*[:=]\s*true""", RegexOption.IGNORE_CASE)
        private val WIFI_DISABLED_RE = Regex("""mwifienabled\s*[:=]\s*false""", RegexOption.IGNORE_CASE)

        private val MOBILE_DATA_ENABLED_STATE_RE = Regex(
            """(?:mobile|cellular)\s+data\s+(?:enabled|state)\s*[:=]\s*(true|false|on|off|connected|disconnected)""",
            RegexOption.IGNORE_CASE,
        )
        private val MOBILE_DATA_MDATA_ENABLED_RE = Regex("""\bmdataenabled\s*[:=]\s*(true|false)\b""", RegexOption.IGNORE_CASE)
        private val MOBILE_DATA_STATE_CONNECTED_RE = Regex("""mobile data state\s*[:=]\s*connected""", RegexOption.IGNORE_CASE)
        private val DATA_CONNECTION_STATE_CONNECTED_RE = Regex("""data connection state\s*[:=]\s*connected""", RegexOption.IGNORE_CASE)

        private val BLUETOOTH_ENABLED_RE = Regex("""\benabled\s*[:=]\s*(true|false)\b""", RegexOption.IGNORE_CASE)
        private val BLUETOOTH_STATE_RE = Regex("""\bstate\s*[:=]\s*(on|off|turning_on|turning_off)\b""", RegexOption.IGNORE_CASE)
    }
}
