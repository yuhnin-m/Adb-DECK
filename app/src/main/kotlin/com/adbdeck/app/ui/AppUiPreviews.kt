package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.app.navigation.Screen
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.dashboard.DashboardState
import com.adbdeck.feature.devices.DeviceListState
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState
import com.adbdeck.feature.apkinstall.ApkInstallComponent
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.DeepLinksState
import com.adbdeck.feature.deeplinks.DeepLinksTab
import com.adbdeck.feature.deeplinks.IntentTemplate
import com.adbdeck.feature.deeplinks.LaunchHistoryEntry
import com.adbdeck.feature.deviceinfo.DeviceInfoComponent
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSection
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionLoadState
import com.adbdeck.feature.deviceinfo.DeviceInfoState
import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.feature.notifications.ui.PreviewNotificationsComponent
import com.adbdeck.feature.apkinstall.ApkInstallState
import com.adbdeck.feature.apkinstall.ApkInstallStatus
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import com.adbdeck.feature.fileexplorer.ExplorerListState
import com.adbdeck.feature.fileexplorer.ExplorerPanelState
import com.adbdeck.feature.fileexplorer.ExplorerSide
import com.adbdeck.feature.fileexplorer.FileExplorerComponent
import com.adbdeck.feature.fileexplorer.FileExplorerState
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatState
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.ContactsState
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.packages.PackageSortOrder
import com.adbdeck.feature.packages.PackagesState
import com.adbdeck.feature.packages.PackageTypeFilter
import com.adbdeck.feature.screentools.ScreenToolsComponent
import com.adbdeck.feature.screentools.ScreenToolsState
import com.adbdeck.feature.screentools.ScreenToolsStatus
import com.adbdeck.feature.screentools.ScreenToolsTab
import com.adbdeck.feature.screentools.ScreenshotQualityPreset
import com.adbdeck.feature.screentools.ScreenshotSectionState
import com.adbdeck.feature.screentools.ScreenrecordQualityPreset
import com.adbdeck.feature.screentools.ScreenrecordSectionState
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.SettingsUiState
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent
import com.adbdeck.feature.systemmonitor.SystemMonitorTab
import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import com.adbdeck.feature.systemmonitor.processes.ProcessesState
import com.adbdeck.feature.systemmonitor.processes.ProcessSortField
import com.adbdeck.feature.systemmonitor.storage.StorageComponent
import com.adbdeck.feature.systemmonitor.storage.StorageState
import com.adbdeck.feature.quicktoggles.ANIMATION_ANIMATOR_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_TRANSITION_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_WINDOW_SCALE_KEY
import com.adbdeck.feature.quicktoggles.AnimationScaleControl
import com.adbdeck.feature.quicktoggles.AnimationScaleStatus
import com.adbdeck.feature.quicktoggles.QuickToggleId
import com.adbdeck.feature.quicktoggles.QuickToggleState
import com.adbdeck.feature.quicktoggles.QuickTogglesComponent
import com.adbdeck.feature.quicktoggles.QuickTogglesState
import com.adbdeck.feature.quicktoggles.ToggleItem
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val previewDevices = listOf(
    AdbDevice("emulator-5554", DeviceState.DEVICE, "Pixel_8_API_34"),
    AdbDevice("192.168.0.15:5555", DeviceState.OFFLINE, "Wi-Fi"),
    AdbDevice("R58N123ABC", DeviceState.UNAUTHORIZED, "Samsung"),
)

private val previewLogEntries = listOf(
    LogcatEntry(
        id = 1L,
        raw = "03-04 12:31:40.111  2048  4096 I ActivityManager: Start proc com.example.app",
        date = "03-04",
        time = "12:31:40",
        millis = "111",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.INFO,
        tag = "ActivityManager",
        message = "Start proc com.example.app",
    ),
    LogcatEntry(
        id = 2L,
        raw = "03-04 12:31:41.222  2048  4096 W ExampleTag: Slow call detected",
        date = "03-04",
        time = "12:31:41",
        millis = "222",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.WARNING,
        tag = "ExampleTag",
        message = "Slow call detected",
    ),
    LogcatEntry(
        id = 3L,
        raw = "03-04 12:31:42.333  2048  4096 E CrashReporter: NullPointerException",
        date = "03-04",
        time = "12:31:42",
        millis = "333",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.ERROR,
        tag = "CrashReporter",
        message = "NullPointerException",
    ),
)

private class PreviewDashboardComponent : DashboardComponent {
    override val state: StateFlow<DashboardState> = MutableStateFlow(
        DashboardState(
            adbStatusText = "✓ adb доступен: Android Debug Bridge version 1.0.41",
            deviceCount = previewDevices.size,
        )
    )

    override fun onOpenDevices() = Unit
    override fun onOpenLogcat() = Unit
    override fun onOpenSettings() = Unit
    override fun onRefreshDevices() = Unit
    override fun onCheckAdb() = Unit
}

private class PreviewDevicesComponent : DevicesComponent {
    override val state: StateFlow<DevicesState> = MutableStateFlow(
        DevicesState(
            listState        = DeviceListState.Success(previewDevices),
            selectedDeviceId = previewDevices.first().deviceId,
        )
    )

    override fun onRefresh() = Unit
    override fun onSelectDevice(device: AdbDevice) = Unit
    override fun onOpenDetails(device: AdbDevice) = Unit
    override fun onCloseDetails() = Unit
    override fun onRefreshDeviceInfo(device: AdbDevice) = Unit
    override fun onNavigateToLogcat() = Unit
    override fun onNavigateToPackages() = Unit
    override fun onNavigateToSystemMonitor() = Unit
    override fun onRequestReboot(device: AdbDevice) = Unit
    override fun onRequestRebootRecovery(device: AdbDevice) = Unit
    override fun onRequestRebootBootloader(device: AdbDevice) = Unit
    override fun onRequestDisconnect(device: AdbDevice) = Unit
    override fun onConfirmAction() = Unit
    override fun onCancelAction() = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewLogcatComponent : LogcatComponent {
    override val state: StateFlow<LogcatState> = MutableStateFlow(
        LogcatState(
            isRunning = true,
            activeDeviceId = previewDevices.first().deviceId,
            entries = previewLogEntries,
            filteredEntries = previewLogEntries,
            totalLineCount = previewLogEntries.size,
            displayMode = LogcatDisplayMode.COMPACT,
            showTime = true,
            showMillis = true,
            coloredLevels = true,
            autoScroll = true,
        )
    )

    override fun onStart() = Unit
    override fun onStop() = Unit
    override fun onClear() = Unit
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
    override fun onFontFamilyChanged(family: com.adbdeck.feature.logcat.LogcatFontFamily) = Unit
    override fun onFontSizeChanged(size: Int) = Unit
}

private class PreviewSettingsComponent : SettingsComponent {
    override val state: StateFlow<SettingsUiState> = MutableStateFlow(
        SettingsUiState(
            adbPath = "/opt/homebrew/bin/adb",
            bundletoolPath = "/opt/homebrew/bin/bundletool",
            adbCheckResult = "✓ Доступен: Android Debug Bridge version 1.0.41",
            bundletoolCheckResult = "✓ Доступен: 1.18.2",
            isCheckingBundletool = false,
            isSaved = true,
            currentTheme = AppTheme.DARK,
            logcatCompactMode = true,
            logcatShowDate = false,
            logcatShowTime = true,
            logcatShowMillis = true,
            logcatColoredLevels = true,
            logcatMaxBufferedLines = 5000,
            logcatAutoScroll = true,
        )
    )

    override fun onAdbPathChanged(path: String) = Unit
    override fun onBundletoolPathChanged(path: String) = Unit
    override fun onSave() = Unit
    override fun onCheckAdb() = Unit
    override fun onCheckBundletool() = Unit
    override fun onThemeChanged(theme: AppTheme) = Unit
    override fun onLogcatCompactModeChanged(value: Boolean) = Unit
    override fun onLogcatShowDateChanged(value: Boolean) = Unit
    override fun onLogcatShowTimeChanged(value: Boolean) = Unit
    override fun onLogcatShowMillisChanged(value: Boolean) = Unit
    override fun onLogcatColoredLevelsChanged(value: Boolean) = Unit
    override fun onLogcatMaxBufferedLinesChanged(lines: Int) = Unit
    override fun onLogcatAutoScrollChanged(value: Boolean) = Unit
    override fun onLogcatFontFamilyChanged(family: String) = Unit
    override fun onLogcatFontSizeChanged(size: Int) = Unit
}

private class PreviewPackagesComponent : PackagesComponent {
    override val state: StateFlow<PackagesState> = MutableStateFlow(PackagesState())

    override fun onRefresh() = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onTypeFilterChanged(filter: PackageTypeFilter) = Unit
    override fun onDisabledFilterChanged(enabled: Boolean) = Unit
    override fun onDebuggableFilterChanged(enabled: Boolean) = Unit
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

private class PreviewSystemMonitorComponent : SystemMonitorComponent {
    override val activeTab: StateFlow<SystemMonitorTab> = MutableStateFlow(SystemMonitorTab.PROCESSES)
    override val isProcessMonitoring: StateFlow<Boolean> = MutableStateFlow(false)
    override val processesComponent: ProcessesComponent = object : ProcessesComponent {
        override val state: StateFlow<ProcessesState> = MutableStateFlow(ProcessesState())
        override fun onStartMonitoring() = Unit
        override fun onStopMonitoring() = Unit
        override fun onRefresh() = Unit
        override fun onSearchChanged(query: String) = Unit
        override fun onSortFieldChanged(field: ProcessSortField) = Unit
        override fun onSelectProcess(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) = Unit
        override fun onClearSelection() = Unit
        override fun onKillProcess(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) = Unit
        override fun onForceStopApp(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) = Unit
        override fun onOpenPackageDetails(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) = Unit
        override fun onDismissFeedback() = Unit
    }
    override val storageComponent: StorageComponent = object : StorageComponent {
        override val state: StateFlow<StorageState> = MutableStateFlow(StorageState())
        override fun onRefresh() = Unit
    }
    override fun onTabSelected(tab: SystemMonitorTab) = Unit
}

private class PreviewContactsComponent : ContactsComponent {
    override val state: StateFlow<ContactsState> = MutableStateFlow(ContactsState())
    override fun onRefresh() = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onSelectTargetAccount(account: ContactAccount) = Unit
    override fun onSelectContact(contact: Contact) = Unit
    override fun onCloseDetail() = Unit
    override fun onRefreshDetail() = Unit
    override fun onShowAddForm() = Unit
    override fun onAddFormFirstNameChanged(value: String) = Unit
    override fun onAddFormLastNameChanged(value: String) = Unit
    override fun onAddFormAccountChanged(account: ContactAccount) = Unit
    override fun onAddFormPhone1Changed(value: String) = Unit
    override fun onAddFormPhone1TypeChanged(type: PhoneType) = Unit
    override fun onAddFormPhone2Changed(value: String) = Unit
    override fun onAddFormPhone2TypeChanged(type: PhoneType) = Unit
    override fun onAddFormEmailChanged(value: String) = Unit
    override fun onAddFormEmailTypeChanged(type: EmailType) = Unit
    override fun onAddFormOrganizationChanged(value: String) = Unit
    override fun onAddFormNotesChanged(value: String) = Unit
    override fun onSubmitAddForm() = Unit
    override fun onDismissAddForm() = Unit
    override fun onRequestDelete(contact: Contact) = Unit
    override fun onConfirmDelete() = Unit
    override fun onCancelDelete() = Unit
    override fun onExportAllToJson(path: String) = Unit
    override fun onExportAllToVcf(path: String) = Unit
    override fun onExportContactToJson(contact: Contact, path: String) = Unit
    override fun onExportContactToVcf(contact: Contact, path: String) = Unit
    override fun onImportFromJson(path: String) = Unit
    override fun onImportFromVcf(path: String) = Unit
    override fun onCancelOperation() = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewDeepLinksComponent : DeepLinksComponent {
    override val state: StateFlow<DeepLinksState> = MutableStateFlow(DeepLinksState())
    override fun onModeChanged(mode: LaunchMode) = Unit
    override fun onDlUriChanged(value: String) = Unit
    override fun onDlActionChanged(value: String) = Unit
    override fun onDlPackageChanged(value: String) = Unit
    override fun onDlComponentChanged(value: String) = Unit
    override fun onDlCategoryChanged(value: String) = Unit
    override fun onItActionChanged(value: String) = Unit
    override fun onItDataUriChanged(value: String) = Unit
    override fun onItPackageChanged(value: String) = Unit
    override fun onItComponentChanged(value: String) = Unit
    override fun onItCategoryAdd(category: String) = Unit
    override fun onItCategoryRemove(index: Int) = Unit
    override fun onItFlagsChanged(value: String) = Unit
    override fun onItExtraAdd() = Unit
    override fun onItExtraRemove(index: Int) = Unit
    override fun onItExtraKeyChanged(index: Int, key: String) = Unit
    override fun onItExtraTypeChanged(index: Int, type: ExtraType) = Unit
    override fun onItExtraValueChanged(index: Int, value: String) = Unit
    override fun onLaunch() = Unit
    override fun onRightTabChanged(tab: DeepLinksTab) = Unit
    override fun onRestoreFromHistory(entry: LaunchHistoryEntry) = Unit
    override fun onDeleteHistoryEntry(id: String) = Unit
    override fun onClearHistory() = Unit
    override fun onShowSaveTemplateDialog() = Unit
    override fun onSaveTemplateNameChanged(name: String) = Unit
    override fun onConfirmSaveTemplate() = Unit
    override fun onDismissSaveTemplateDialog() = Unit
    override fun onLaunchTemplate(template: IntentTemplate) = Unit
    override fun onRestoreFromTemplate(template: IntentTemplate) = Unit
    override fun onDeleteTemplate(id: String) = Unit
    override fun prefillDeepLinkUri(uri: String) = Unit
}

private class PreviewFileExplorerComponent : FileExplorerComponent {
    private val localItems = listOf(
        ExplorerFileItem(
            name = "Projects",
            fullPath = "/Users/demo/Projects",
            type = ExplorerFileType.DIRECTORY,
            modifiedEpochMillis = 1_740_000_000_000,
        ),
        ExplorerFileItem(
            name = "notes.txt",
            fullPath = "/Users/demo/notes.txt",
            type = ExplorerFileType.FILE,
            sizeBytes = 8_192,
            modifiedEpochMillis = 1_740_000_100_000,
        ),
    )

    private val deviceItems = listOf(
        ExplorerFileItem(
            name = "Download",
            fullPath = "/sdcard/Download",
            type = ExplorerFileType.DIRECTORY,
            modifiedEpochMillis = 1_740_000_200_000,
        ),
        ExplorerFileItem(
            name = "screen.png",
            fullPath = "/sdcard/Download/screen.png",
            type = ExplorerFileType.FILE,
            sizeBytes = 1_527_000,
            modifiedEpochMillis = 1_740_000_300_000,
        ),
    )

    override val state: StateFlow<FileExplorerState> = MutableStateFlow(
        FileExplorerState(
            localPanel = ExplorerPanelState(
                currentPath = "/Users/demo",
                listState = ExplorerListState.Success(localItems),
                selectedPath = "/Users/demo/notes.txt",
            ),
            devicePanel = ExplorerPanelState(
                currentPath = "/sdcard",
                listState = ExplorerListState.Success(deviceItems),
                selectedPath = "/sdcard/Download/screen.png",
            ),
            activeDeviceId = "emulator-5554",
            deviceRoots = listOf("/sdcard", "/storage/emulated/0", "/data/local/tmp"),
        )
    )

    override fun onRefreshLocal() = Unit
    override fun onLocalUp() = Unit
    override fun onOpenLocalDirectory(path: String) = Unit
    override fun onSelectLocal(path: String) = Unit
    override fun onRefreshDevice() = Unit
    override fun onDeviceUp() = Unit
    override fun onOpenDeviceDirectory(path: String) = Unit
    override fun onSelectDevice(path: String) = Unit
    override fun onSelectDeviceRoot(path: String) = Unit
    override fun onRequestDelete(side: ExplorerSide) = Unit
    override fun onConfirmDelete() = Unit
    override fun onCancelDelete() = Unit
    override fun onRequestCreateDirectory(side: ExplorerSide) = Unit
    override fun onCreateDirectoryNameChanged(value: String) = Unit
    override fun onConfirmCreateDirectory() = Unit
    override fun onCancelCreateDirectory() = Unit
    override fun onRequestRename(side: ExplorerSide) = Unit
    override fun onRenameValueChanged(value: String) = Unit
    override fun onConfirmRename() = Unit
    override fun onCancelRename() = Unit
    override fun onPushSelected() = Unit
    override fun onPullSelected() = Unit
    override fun onConfirmTransferConflict() = Unit
    override fun onCancelTransferConflict() = Unit
    override fun onCancelTransfer() = Unit
    override fun onPathCopied(path: String) = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewScreenToolsComponent : ScreenToolsComponent {
    override val state: StateFlow<ScreenToolsState> = MutableStateFlow(
        ScreenToolsState(
            selectedTab = ScreenToolsTab.SCREENSHOT,
            activeDeviceId = previewDevices.first().deviceId,
            deviceMessage = "Активное устройство: ${previewDevices.first().deviceId}",
            screenshot = ScreenshotSectionState(
                outputDirectory = "/Users/demo/Pictures/ADBDeck",
                lastFilePath = "/Users/demo/Pictures/ADBDeck/screenshot_2026-03-04_10-40-00.png",
            ),
            screenrecord = ScreenrecordSectionState(
                outputDirectory = "/Users/demo/Videos/ADBDeck",
                lastFilePath = "/Users/demo/Videos/ADBDeck/screenrecord_2026-03-04_10-35-00.mp4",
            ),
        )
    )

    override fun onSelectTab(tab: ScreenToolsTab) = Unit
    override fun onScreenshotOutputDirectoryChanged(path: String) = Unit
    override fun onPickScreenshotOutputDirectory() = Unit
    override fun onScreenshotQualityChanged(quality: ScreenshotQualityPreset) = Unit
    override fun onScreenrecordOutputDirectoryChanged(path: String) = Unit
    override fun onPickScreenrecordOutputDirectory() = Unit
    override fun onScreenrecordQualityChanged(quality: ScreenrecordQualityPreset) = Unit
    override fun onTakeScreenshot() = Unit
    override fun onCopyLastScreenshotToClipboard() = Unit
    override fun onOpenLastScreenshotFile() = Unit
    override fun onOpenScreenshotFolder() = Unit
    override fun onStartRecording() = Unit
    override fun onStopRecording() = Unit
    override fun onOpenLastVideoFile() = Unit
    override fun onOpenVideoFolder() = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewApkInstallComponent : ApkInstallComponent {
    override val state: StateFlow<ApkInstallState> = MutableStateFlow(
        ApkInstallState(
            activeDeviceId = previewDevices.first().deviceId,
            deviceMessage = "Активное устройство: ${previewDevices.first().deviceId}",
            apkPath = "/Users/demo/Downloads/sample-app.apk",
            status = ApkInstallStatus("Готово к установке: sample-app.apk"),
            logLines = listOf("Запуск установки: /Users/demo/Downloads/sample-app.apk"),
        )
    )

    override fun onApkPathChanged(path: String) = Unit
    override fun onPickApkFile() = Unit
    override fun onApkPathDropped(path: String) = Unit
    override fun onInstallApk() = Unit
    override fun onClearLog() = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewDeviceInfoComponent : DeviceInfoComponent {
    override val state: StateFlow<DeviceInfoState> = MutableStateFlow(
        DeviceInfoState(
            activeDeviceId = previewDevices.first().deviceId,
            sections = listOf(
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.OVERVIEW,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("overview:0", "Device ID", previewDevices.first().deviceId),
                            DeviceInfoRow("overview:1", "Model", "Pixel 8 API 34"),
                            DeviceInfoRow("overview:2", "Android", "14 (SDK 34)"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.BUILD,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("build:0", "Build ID", "AP2A.240905.003"),
                            DeviceInfoRow("build:1", "Debuggable", "Yes"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.DISPLAY,
                    state = DeviceInfoSectionLoadState.Loading,
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.CPU_RAM,
                    state = DeviceInfoSectionLoadState.Error("meminfo command unavailable"),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.BATTERY,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("battery:0", "Battery level", "83%"),
                            DeviceInfoRow("battery:1", "Battery status", "Charging"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.NETWORK,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("network:0", "IP addresses", "192.168.0.24/24"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.STORAGE,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("storage:0", "/data", "18G / 64G (29%)"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.SECURITY,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("security:0", "SELinux", "Enforcing"),
                        )
                    ),
                ),
                DeviceInfoSection(
                    kind = DeviceInfoSectionKind.SYSTEM,
                    state = DeviceInfoSectionLoadState.Success(
                        listOf(
                            DeviceInfoRow("system:0", "Current focus", "mCurrentFocus=Window{...}"),
                        )
                    ),
                ),
            ),
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    )

    override fun onRefresh() = Unit
    override fun onExportJson(path: String) = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewQuickTogglesComponent : QuickTogglesComponent {
    override val state: StateFlow<QuickTogglesState> = MutableStateFlow(
        QuickTogglesState(
            activeDeviceId = previewDevices.first().deviceId,
            items = listOf(
                ToggleItem(
                    id = QuickToggleId.WIFI,
                    title = "Wi-Fi",
                    state = QuickToggleState.ON,
                ),
                ToggleItem(
                    id = QuickToggleId.MOBILE_DATA,
                    title = "Mobile data",
                    state = QuickToggleState.OFF,
                ),
                ToggleItem(
                    id = QuickToggleId.BLUETOOTH,
                    title = "Bluetooth",
                    state = QuickToggleState.UNKNOWN,
                    error = "Permission denied",
                    showOpenSettings = true,
                ),
                ToggleItem(
                    id = QuickToggleId.ANIMATIONS,
                    title = "Animations",
                    state = QuickToggleState.CUSTOM,
                    animationControls = listOf(
                        AnimationScaleControl(
                            key = ANIMATION_WINDOW_SCALE_KEY,
                            currentValue = null,
                            draftValue = 1f,
                            status = AnimationScaleStatus.OK,
                        ),
                        AnimationScaleControl(
                            key = ANIMATION_TRANSITION_SCALE_KEY,
                            currentValue = 0.5f,
                            draftValue = 0.5f,
                            status = AnimationScaleStatus.OK,
                        ),
                        AnimationScaleControl(
                            key = ANIMATION_ANIMATOR_SCALE_KEY,
                            currentValue = 1f,
                            draftValue = 2f,
                            status = AnimationScaleStatus.ERROR,
                            error = "Unexpected value",
                        ),
                    ),
                ),
            ),
        )
    )

    override fun onRefresh() = Unit
    override fun onRefreshToggle(toggleId: QuickToggleId) = Unit
    override fun onRequestToggle(toggleId: QuickToggleId, targetState: QuickToggleState) = Unit
    override fun onAnimationDraftChanged(key: String, value: Float) = Unit
    override fun onSetAnimationScale(key: String) = Unit
    override fun onConfirmToggle() = Unit
    override fun onCancelToggle() = Unit
    override fun onOpenSettings(toggleId: QuickToggleId) = Unit
    override fun onDismissFeedback() = Unit
}

private class PreviewRootComponent(
    initialScreen: Screen = Screen.Dashboard,
) : RootComponent {
    private val dashboard = PreviewDashboardComponent()
    private val devices = PreviewDevicesComponent()
    private val logcat = PreviewLogcatComponent()
    private val settings = PreviewSettingsComponent()
    private val packages = PreviewPackagesComponent()
    private val systemMonitor = PreviewSystemMonitorComponent()
    private val fileExplorer = PreviewFileExplorerComponent()
    private val contacts = PreviewContactsComponent()
    private val screenTools = PreviewScreenToolsComponent()
    private val apkInstall = PreviewApkInstallComponent()
    private val deepLinks = PreviewDeepLinksComponent()
    private val notifications = PreviewNotificationsComponent()
    private val deviceInfo = PreviewDeviceInfoComponent()
    private val quickToggles = PreviewQuickTogglesComponent()

    private val stack = MutableValue(createStack(initialScreen))

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = stack

    override fun navigate(screen: Screen) {
        stack.value = createStack(screen)
    }

    private fun createStack(screen: Screen): ChildStack<Screen, RootComponent.Child> =
        ChildStack(screen, childFor(screen))

    private fun childFor(screen: Screen): RootComponent.Child = when (screen) {
        Screen.Dashboard -> RootComponent.Child.Dashboard(dashboard)
        Screen.Devices -> RootComponent.Child.Devices(devices)
        Screen.Logcat -> RootComponent.Child.Logcat(logcat)
        Screen.Settings -> RootComponent.Child.Settings(settings)
        Screen.Packages -> RootComponent.Child.Packages(packages)
        Screen.SystemMonitor -> RootComponent.Child.SystemMonitor(systemMonitor)
        Screen.FileExplorer -> RootComponent.Child.FileExplorer(fileExplorer)
        Screen.Contacts -> RootComponent.Child.Contacts(contacts)
        Screen.ScreenTools -> RootComponent.Child.ScreenTools(screenTools)
        Screen.ApkInstall -> RootComponent.Child.ApkInstall(apkInstall)
        Screen.DeepLinks -> RootComponent.Child.DeepLinks(deepLinks)
        Screen.Notifications -> RootComponent.Child.Notifications(notifications)
        Screen.DeviceInfo -> RootComponent.Child.DeviceInfo(deviceInfo)
        Screen.QuickToggles -> RootComponent.Child.QuickToggles(quickToggles)
    }
}

private class PreviewDeviceSelectorComponent : DeviceSelectorComponent {
    override val devices: StateFlow<List<AdbDevice>> = MutableStateFlow(previewDevices)
    override val selectedDevice: StateFlow<AdbDevice?> = MutableStateFlow(previewDevices.first())
    override val isConnecting: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow(null)
    override val savedEndpoints: StateFlow<List<DeviceEndpoint>> = MutableStateFlow(
        listOf(
            DeviceEndpoint("192.168.0.15", 5555, "Office Pixel"),
            DeviceEndpoint("10.0.0.21", 5555, "QA Device"),
        )
    )

    override fun onRefresh() = Unit
    override fun onConnect(host: String, port: Int) = Unit
    override fun onDisconnect() = Unit
    override fun onSelectDevice(device: AdbDevice) = Unit
    override fun onConnectSaved(endpoint: DeviceEndpoint) = Unit
    override fun onSwitchToTcpIp(serialId: String, port: Int) = Unit
    override fun onRemoveEndpoint(endpoint: DeviceEndpoint) = Unit
    override fun onClearError() = Unit
}

@Composable
private fun AppComponentPreviewContainer(
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
private fun AppContentPreviewBody(isDarkTheme: Boolean) {
    val root = remember { PreviewRootComponent(Screen.Dashboard) }
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        AppContent(
            rootComponent = root,
            isDarkTheme = isDarkTheme,
            onToggleTheme = {},
            deviceSelectorComponent = selector,
        )
    }
}

@Preview
@Composable
private fun AppContentLightPreview() {
    AppContentPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun AppContentDarkPreview() {
    AppContentPreviewBody(isDarkTheme = true)
}

@Composable
private fun SidebarPreviewBody(isDarkTheme: Boolean) {
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        Sidebar(
            activeScreen = Screen.Devices,
            onNavigate = {},
            isDarkTheme = isDarkTheme,
            onToggleTheme = {},
            devicesCount = 3,
            isLogcatRunning = true,
            hasUnsavedSettings = true,
        )
    }
}

@Preview
@Composable
private fun SidebarLightPreview() {
    SidebarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun SidebarDarkPreview() {
    SidebarPreviewBody(isDarkTheme = true)
}

@Composable
private fun TopBarPreviewBody(isDarkTheme: Boolean) {
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        TopBar(
            title = "Devices",
            trailingContent = { DeviceBar(component = selector) },
        )
    }
}

@Preview
@Composable
private fun TopBarLightPreview() {
    TopBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun TopBarDarkPreview() {
    TopBarPreviewBody(isDarkTheme = true)
}

@Composable
private fun StatusBarPreviewBody(isDarkTheme: Boolean) {
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        StatusBar(statusText = "Подключено устройств: 3")
    }
}

@Preview
@Composable
private fun StatusBarLightPreview() {
    StatusBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun StatusBarDarkPreview() {
    StatusBarPreviewBody(isDarkTheme = true)
}

@Composable
private fun DeviceBarPreviewBody(isDarkTheme: Boolean) {
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.paddingLarge),
            verticalArrangement = Arrangement.Top,
        ) {
            DeviceBar(component = selector)
        }
    }
}

@Preview
@Composable
private fun DeviceBarLightPreview() {
    DeviceBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun DeviceBarDarkPreview() {
    DeviceBarPreviewBody(isDarkTheme = true)
}
