package com.adbdeck.feature.apkinstall.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.apkinstall.ApkInstallComponent
import com.adbdeck.feature.apkinstall.ApkInstallState
import com.adbdeck.feature.apkinstall.ApkInstallStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Preview-реализация [ApkInstallComponent].
 */
private class PreviewApkInstallComponent(
    override val state: StateFlow<ApkInstallState>,
) : ApkInstallComponent {
    override fun onApkPathChanged(path: String) = Unit
    override fun onPickApkFile() = Unit
    override fun onApkPathDropped(path: String) = Unit
    override fun onAllowTestOnlyChanged(allow: Boolean) = Unit
    override fun onInstallApk() = Unit
    override fun onCopyStatusResult() = Unit
    override fun onClearLog() = Unit
    override fun onDismissFeedback() = Unit
}

@Preview
@Composable
private fun ApkInstallPreviewLight() {
    AdbDeckTheme(isDarkTheme = false) {
        ApkInstallScreen(
            component = PreviewApkInstallComponent(
                state = MutableStateFlow(
                    ApkInstallState(
                        activeDeviceId = "R58N123ABC",
                        deviceMessage = "Активное устройство: R58N123ABC",
                        apkPath = "/Users/demo/Downloads/sample-app.apk",
                        status = ApkInstallStatus("Готово к установке: sample-app.apk"),
                        logLines = listOf(
                            "Запуск установки: /Users/demo/Downloads/sample-app.apk",
                            "Performing Streamed Install",
                            "Success",
                        ),
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ApkInstallPreviewDarkInstalling() {
    AdbDeckTheme(isDarkTheme = true) {
        ApkInstallScreen(
            component = PreviewApkInstallComponent(
                state = MutableStateFlow(
                    ApkInstallState(
                        activeDeviceId = "R58N123ABC",
                        deviceMessage = "Активное устройство: R58N123ABC",
                        apkPath = "/Users/demo/Downloads/sample-app.apk",
                        isInstalling = true,
                        installingDeviceId = "R58N123ABC",
                        status = ApkInstallStatus(
                            message = "Performing Streamed Install",
                            progress = null,
                        ),
                        logLines = listOf(
                            "Запуск установки: /Users/demo/Downloads/sample-app.apk",
                            "Performing Streamed Install",
                            "[ 38%] Installing…",
                        ),
                    )
                )
            )
        )
    }
}
