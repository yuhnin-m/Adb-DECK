package com.adbdeck.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun CoreUiPreviewContainer(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun CoreStatesPreviewBody(isDarkTheme: Boolean) {
    CoreUiPreviewContainer(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            LoadingView(
                message = "Синхронизация устройств…",
                modifier = Modifier.weight(1f),
            )
            EmptyView(
                message = "Нет подключенных устройств",
                modifier = Modifier.weight(1f),
            )
            ErrorView(
                message = "Не удалось выполнить adb devices",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun CoreUiLightPreview() {
    CoreStatesPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun CoreUiDarkPreview() {
    CoreStatesPreviewBody(isDarkTheme = true)
}
