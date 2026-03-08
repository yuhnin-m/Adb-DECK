package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val previewLogcatEntries = listOf(
    LogcatEntry(
        id = 1L,
        raw = "03-04 13:10:11.001  1234  5678 I ActivityManager: Start proc com.example",
        date = "03-04",
        time = "13:10:11",
        millis = "001",
        pid = "1234",
        tid = "5678",
        level = LogcatLevel.INFO,
        tag = "ActivityManager",
        message = "Start proc com.example",
    ),
    LogcatEntry(
        id = 2L,
        raw = "03-04 13:10:11.111  1234  5678 D Network: GET /api/ping",
        date = "03-04",
        time = "13:10:11",
        millis = "111",
        pid = "1234",
        tid = "5678",
        level = LogcatLevel.DEBUG,
        tag = "Network",
        message = "GET /api/ping",
    ),
    LogcatEntry(
        id = 3L,
        raw = "03-04 13:10:12.222  1234  5678 W Sync: Retry in 5s",
        date = "03-04",
        time = "13:10:12",
        millis = "222",
        pid = "1234",
        tid = "5678",
        level = LogcatLevel.WARNING,
        tag = "Sync",
        message = "Retry in 5s",
    ),
    LogcatEntry(
        id = 4L,
        raw = "03-04 13:10:13.333  1234  5678 E CrashReporter: IllegalStateException",
        date = "03-04",
        time = "13:10:13",
        millis = "333",
        pid = "1234",
        tid = "5678",
        level = LogcatLevel.ERROR,
        tag = "CrashReporter",
        message = "IllegalStateException",
    ),
)

private class LogcatPreviewComponent : LogcatComponent {
    override val state: StateFlow<LogcatState> = MutableStateFlow(
        LogcatState(
            isRunning = true,
            activeDeviceId = "emulator-5554",
            entries = previewLogcatEntries,
            filteredEntries = previewLogcatEntries,
            totalLineCount = previewLogcatEntries.size,
            displayMode = LogcatDisplayMode.FULL,
            showDate = true,
            showTime = true,
            showMillis = true,
            coloredLevels = true,
            autoScroll = true,
        )
    )

    override fun onStart() = Unit
    override fun onStop() = Unit
    override fun onClear() = Unit
    override fun onImportFromFile(path: String) = Unit
    override fun onCloseImportedFile() = Unit
    override fun onExportToFile(path: String) = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onTagFilterChanged(tag: String) = Unit
    override fun onPackageFilterChanged(pkg: String) = Unit
    override fun onLevelFilterChanged(level: LogcatLevel?) = Unit
    override fun onDisplayModeChanged(mode: LogcatDisplayMode) = Unit
    override fun onToggleShowDate() = Unit
    override fun onToggleShowTime() = Unit
    override fun onToggleShowMillis() = Unit
    override fun onToggleColoredLevels() = Unit
    override fun onAutoScrollChanged(value: Boolean) = Unit
    override fun onSmoothStreamAnimationChanged(value: Boolean) = Unit
    override fun onFontFamilyChanged(family: com.adbdeck.feature.logcat.LogcatFontFamily) = Unit
    override fun onFontSizeChanged(size: Int) = Unit
}

@Composable
private fun LogcatPreviewBody(isDarkTheme: Boolean) {
    val component = LogcatPreviewComponent()
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LogcatScreen(component = component)
        }
    }
}

@Preview
@Composable
private fun LogcatLightPreview() {
    LogcatPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun LogcatDarkPreview() {
    LogcatPreviewBody(isDarkTheme = true)
}
