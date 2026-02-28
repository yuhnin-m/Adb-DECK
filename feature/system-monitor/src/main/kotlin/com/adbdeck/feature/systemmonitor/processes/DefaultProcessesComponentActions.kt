package com.adbdeck.feature.systemmonitor.processes

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_action_failed
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_no_active_device
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_process_not_android_app
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_process_package_missing
import adbdeck.feature.system_monitor.generated.resources.system_monitor_feedback_force_stop_success
import adbdeck.feature.system_monitor.generated.resources.system_monitor_feedback_kill_success
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

internal fun DefaultProcessesComponent.handleKillProcess(process: ProcessInfo) {
    runAction(
        successMessage = {
            getString(
                Res.string.system_monitor_feedback_kill_success,
                process.pid,
            )
        },
    ) { deviceId, adbPath ->
        systemMonitorClient.killProcess(deviceId, process.pid, adbPath)
    }
}

internal fun DefaultProcessesComponent.handleForceStopApp(process: ProcessInfo) {
    if (!process.looksLikePackage) {
        showFeedbackResource(
            messageRes = Res.string.system_monitor_error_process_not_android_app,
            isError = true,
            process.name,
        )
        return
    }

    val packageName = process.packageName.ifEmpty { process.name }
    runAction(
        successMessage = {
            getString(
                Res.string.system_monitor_feedback_force_stop_success,
                packageName,
            )
        },
    ) { deviceId, adbPath ->
        packageClient.forceStop(deviceId, packageName, adbPath)
    }
}

internal fun DefaultProcessesComponent.handleOpenPackageDetails(process: ProcessInfo) {
    if (!process.looksLikePackage) {
        showFeedbackResource(
            messageRes = Res.string.system_monitor_error_process_package_missing,
            isError = true,
        )
        return
    }
    val packageName = process.packageName.ifEmpty { process.name }
    openPackageDetails(packageName)
}

internal fun DefaultProcessesComponent.runAction(
    successMessage: suspend () -> String,
    block: suspend (deviceId: String, adbPath: String) -> Result<Unit>?,
) {
    val selected = deviceManager.selectedDeviceFlow.value
    if (selected == null || selected.state != DeviceState.DEVICE) {
        showFeedbackResource(
            messageRes = Res.string.system_monitor_error_no_active_device,
            isError = true,
        )
        return
    }

    val deviceId = selected.deviceId
    val adbPath = settingsRepository.resolvedAdbPath()

    var shouldStart = false
    _state.update { current ->
        if (current.isActionRunning) {
            current
        } else {
            shouldStart = true
            current.copy(isActionRunning = true)
        }
    }
    if (!shouldStart) return

    actionJob?.cancel()
    actionJob = scope.launch {
        try {
            val result = block(deviceId, adbPath) ?: return@launch
            if (!isRequestStillValid(deviceId)) return@launch

            result
                .onSuccess {
                    showFeedback(successMessage(), isError = false)
                    launchSnapshot(deviceId, stopMonitoringOnFailure = false)
                }
                .onFailure { e ->
                    showFeedback(
                        e.message ?: getString(Res.string.system_monitor_error_action_failed),
                        isError = true,
                    )
                }
        } finally {
            _state.update { it.copy(isActionRunning = false) }
            actionJob = null
        }
    }
}

internal fun DefaultProcessesComponent.handleDismissFeedback() {
    feedbackJob?.cancel()
    feedbackJob = null
    _state.update { it.copy(actionFeedback = null) }
}

internal fun DefaultProcessesComponent.showFeedback(message: String, isError: Boolean) {
    feedbackJob?.cancel()
    _state.update { it.copy(actionFeedback = ProcessActionFeedback(message, isError)) }
    feedbackJob = scope.launch {
        delay(3_000L)
        _state.update { current ->
            if (current.actionFeedback?.message == message) {
                current.copy(actionFeedback = null)
            } else {
                current
            }
        }
    }
}

internal fun DefaultProcessesComponent.showFeedbackResource(
    messageRes: StringResource,
    isError: Boolean,
    vararg args: Any,
) {
    scope.launch {
        val message = getString(messageRes, *args)
        showFeedback(message = message, isError = isError)
    }
}
