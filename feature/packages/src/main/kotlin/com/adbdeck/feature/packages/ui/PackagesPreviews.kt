package com.adbdeck.feature.packages.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageDetails
import com.adbdeck.core.adb.api.packages.PackageType
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.packages.ActionFeedback
import com.adbdeck.feature.packages.PackageDetailState
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.packages.PackagesListState
import com.adbdeck.feature.packages.PackageSortOrder
import com.adbdeck.feature.packages.PackagesState
import com.adbdeck.feature.packages.PackageTypeFilter
import com.adbdeck.feature.packages.PendingPackageAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

// ── Тестовые данные ───────────────────────────────────────────────────────────

private val previewPackages = listOf(
    AppPackage(
        packageName = "com.example.myapp",
        apkPath = "/data/app/~~abc123==/com.example.myapp-1/base.apk",
        type = PackageType.USER,
        isEnabled = true,
    ),
    AppPackage(
        packageName = "com.android.settings",
        apkPath = "/system/priv-app/Settings/Settings.apk",
        type = PackageType.SYSTEM,
        isEnabled = true,
    ),
    AppPackage(
        packageName = "com.google.android.gms",
        apkPath = "/data/app/~~xyz789==/com.google.android.gms-2/base.apk",
        type = PackageType.USER,
        isEnabled = true,
    ),
    AppPackage(
        packageName = "com.android.phone",
        apkPath = "/system/priv-app/TeleService/TeleService.apk",
        type = PackageType.SYSTEM,
        isEnabled = true,
    ),
    AppPackage(
        packageName = "org.mozilla.firefox",
        apkPath = "/data/app/~~ffx001==/org.mozilla.firefox-1/base.apk",
        type = PackageType.USER,
        isEnabled = false,
    ),
)

private val previewDetails = PackageDetails(
    packageName = "com.example.myapp",
    appLabel = "My Application",
    versionName = "2.5.1",
    versionCode = 251,
    uid = 10123,
    codePath = "/data/app/~~abc123==/com.example.myapp-1",
    dataDir = "/data/data/com.example.myapp",
    nativeLibPath = "/data/app/~~abc123==/com.example.myapp-1/lib/arm64",
    firstInstallTime = "2024-01-15 10:30:00",
    lastUpdateTime = "2024-11-20 14:22:00",
    targetSdk = 34,
    minSdk = 26,
    isSystem = false,
    isEnabled = true,
    isDebuggable = true,
    isSuspended = false,
    launcherActivity = "com.example.myapp.MainActivity",
    runtimePermissions = mapOf(
        "android.permission.CAMERA" to true,
        "android.permission.READ_CONTACTS" to false,
        "android.permission.ACCESS_FINE_LOCATION" to true,
        "android.permission.RECORD_AUDIO" to false,
    ),
)

// ── Preview-заглушки компонента ────────────────────────────────────────────────

/** Заглушка [PackagesComponent] для composable-превью. */
private class PackagesPreviewComponent(
    state: PackagesState,
) : PackagesComponent {
    override val state: StateFlow<PackagesState> = MutableStateFlow(state)

    override fun onRefresh() = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onTypeFilterChanged(filter: PackageTypeFilter) = Unit
    override fun onSortOrderChanged(order: PackageSortOrder) = Unit
    override fun onSelectPackage(pkg: AppPackage) = Unit
    override fun onClearSelection() = Unit
    override fun onRevealPackage(packageName: String) = Unit
    override fun onLaunchApp(pkg: AppPackage) = Unit
    override fun onForceStop(pkg: AppPackage) = Unit
    override fun onOpenAppInfo(pkg: AppPackage) = Unit
    override fun onTrackInLogcat(pkg: AppPackage) = Unit
    override fun onExportApk(pkg: AppPackage, localPath: String) = Unit
    override fun onCopyPackageName(pkg: AppPackage) = Unit
    override fun onRequestClearData(pkg: AppPackage) = Unit
    override fun onRequestUninstall(pkg: AppPackage) = Unit
    override fun onConfirmAction() = Unit
    override fun onCancelAction() = Unit
    override fun onGrantPermission(pkg: AppPackage, permission: String) = Unit
    override fun onRevokePermission(pkg: AppPackage, permission: String) = Unit
    override fun onDismissFeedback() = Unit
}

// ── Состояния для превью ──────────────────────────────────────────────────────

/** Успешно загруженный список с выбранным пакетом и деталями. */
private val stateWithDetails = PackagesState(
    listState = PackagesListState.Success(previewPackages),
    filteredPackages = previewPackages,
    selectedPackage = previewPackages.first(),
    detailState = PackageDetailState.Success(previewDetails),
)

/** Список загружается (нет выделенного пакета). */
private val stateLoading = PackagesState(
    listState = PackagesListState.Loading,
    filteredPackages = emptyList(),
)

/** Устройство не выбрано. */
private val stateNoDevice = PackagesState(
    listState = PackagesListState.NoDevice,
)

/** Список с диалогом подтверждения удаления. */
private val stateWithDialog = PackagesState(
    listState = PackagesListState.Success(previewPackages),
    filteredPackages = previewPackages,
    selectedPackage = previewPackages.first(),
    pendingAction = PendingPackageAction.Uninstall(previewPackages.first()),
)

/** Список с баннером обратной связи. */
private val stateWithFeedback = PackagesState(
    listState = PackagesListState.Success(previewPackages),
    filteredPackages = previewPackages,
    actionFeedback = ActionFeedback("Приложение запущено", isError = false),
)

// ── Обёртка превью ─────────────────────────────────────────────────────────────

@Composable
private fun PackagesPreviewWrapper(
    isDarkTheme: Boolean,
    state: PackagesState,
) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PackagesScreen(component = PackagesPreviewComponent(state))
        }
    }
}

// ── Превью: список с деталями ──────────────────────────────────────────────────

@Preview
@Composable
private fun PackagesWithDetailsLightPreview() {
    PackagesPreviewWrapper(isDarkTheme = false, state = stateWithDetails)
}

@Preview
@Composable
private fun PackagesWithDetailsDarkPreview() {
    PackagesPreviewWrapper(isDarkTheme = true, state = stateWithDetails)
}

// ── Превью: загрузка ───────────────────────────────────────────────────────────

@Preview
@Composable
private fun PackagesLoadingLightPreview() {
    PackagesPreviewWrapper(isDarkTheme = false, state = stateLoading)
}

@Preview
@Composable
private fun PackagesLoadingDarkPreview() {
    PackagesPreviewWrapper(isDarkTheme = true, state = stateLoading)
}

// ── Превью: нет устройства ─────────────────────────────────────────────────────

@Preview
@Composable
private fun PackagesNoDeviceLightPreview() {
    PackagesPreviewWrapper(isDarkTheme = false, state = stateNoDevice)
}

@Preview
@Composable
private fun PackagesNoDeviceDarkPreview() {
    PackagesPreviewWrapper(isDarkTheme = true, state = stateNoDevice)
}

// ── Превью: диалог подтверждения ───────────────────────────────────────────────

@Preview
@Composable
private fun PackagesDialogLightPreview() {
    PackagesPreviewWrapper(isDarkTheme = false, state = stateWithDialog)
}

// ── Превью: обратная связь ─────────────────────────────────────────────────────

@Preview
@Composable
private fun PackagesFeedbackDarkPreview() {
    PackagesPreviewWrapper(isDarkTheme = true, state = stateWithFeedback)
}
