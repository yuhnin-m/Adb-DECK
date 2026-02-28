package com.adbdeck.feature.apkinstall.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.feature.apkinstall.ApkInstallComponent

/**
 * Экран установки APK на активное устройство.
 */
@Composable
fun ApkInstallScreen(component: ApkInstallComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ApkInstallContent(
            state = state,
            onApkPathChanged = component::onApkPathChanged,
            onPickApkFile = component::onPickApkFile,
            onApkPathDropped = component::onApkPathDropped,
            onInstallApk = component::onInstallApk,
            onClearLog = component::onClearLog,
            modifier = Modifier.weight(1f),
        )

        state.feedback?.let { feedback ->
            HorizontalDivider()
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingMedium,
                        vertical = Dimensions.paddingSmall,
                    ),
            )
        }
    }
}
