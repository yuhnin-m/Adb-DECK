package com.adbdeck.feature.notifications.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.NotificationRecord
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.notifications.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

// ──────────────────────────────────────────────────────────────────────────────
// Preview-данные
// ──────────────────────────────────────────────────────────────────────────────

private fun previewRecord(
    pkg: String = "com.example.app",
    title: String? = "Новое сообщение",
    text: String? = "Привет, как дела?",
    importance: Int = 4,
    isOngoing: Boolean = false,
    postedAt: Long? = System.currentTimeMillis() - 60_000,
    actionsCount: Int? = null,
    actionTitles: List<String> = emptyList(),
    imageParameters: Map<String, String> = emptyMap(),
) = NotificationRecord(
    key            = "0|$pkg|1|null|10050",
    packageName    = pkg,
    notificationId = 1,
    tag            = null,
    importance     = importance,
    channelId      = "messages",
    title          = title,
    text           = text,
    subText        = null,
    bigText        = null,
    summaryText    = null,
    category       = "msg",
    flags          = 0,
    isOngoing      = isOngoing,
    isClearable    = !isOngoing,
    postedAt       = postedAt,
    group          = null,
    sortKey        = null,
    actionsCount   = actionsCount,
    actionTitles   = actionTitles,
    imageParameters = imageParameters,
    rawBlock       = "  NotificationRecord(0x12345): pkg=$pkg, id=1, tag=null, uid=10050\n    title=$title\n    text=$text\n",
)

private val previewRecords = listOf(
    previewRecord(pkg = "com.android.systemui", title = "USB-отладка включена", text = "Нажмите для управления", importance = 3, isOngoing = true),
    previewRecord(
        pkg = "com.example.messenger",
        title = "Иван Иванов",
        text = "Привет! Как дела?",
        importance = 4,
        postedAt = System.currentTimeMillis() - 30_000,
        actionsCount = 2,
        actionTitles = listOf("Ответить", "Отметить как прочитанное"),
        imageParameters = mapOf(
            "android.largeIcon" to "Icon(typ=BITMAP size=128x128)",
            "android.template" to "android.app.Notification\$MessagingStyle",
        ),
    ),
    previewRecord(pkg = "com.example.news", title = "Breaking News", text = "Важное событие произошло", importance = 4),
    previewRecord(pkg = "com.google.android.gms", title = null, text = "Google Play Services", importance = 2, isOngoing = true),
)

// ──────────────────────────────────────────────────────────────────────────────
// Preview-стаб компонента
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Стаб [NotificationsComponent] для использования в превью и тестах.
 * Все методы — no-op.
 */
class PreviewNotificationsComponent(
    initialState: NotificationsState = NotificationsState(),
) : NotificationsComponent {

    override val state: StateFlow<NotificationsState> = MutableStateFlow(initialState)

    override fun onRefresh() = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onPackageFilterChanged(pkg: String) = Unit
    override fun onFilterChanged(filter: NotificationsFilter) = Unit
    override fun onSortOrderChanged(order: NotificationsSortOrder) = Unit
    override fun onSelectNotification(record: NotificationRecord) = Unit
    override fun onCloseDetail() = Unit
    override fun onSelectTab(tab: NotificationsTab) = Unit
    override fun onCopyPackageName(record: NotificationRecord) = Unit
    override fun onCopyTitle(record: NotificationRecord) = Unit
    override fun onCopyText(record: NotificationRecord) = Unit
    override fun onCopyRawDump(record: NotificationRecord) = Unit
    override fun onSaveNotification(record: NotificationRecord) = Unit
    override fun onDeleteSaved(id: String) = Unit
    override fun onExportToJson(record: NotificationRecord, path: String) = Unit
    override fun onOpenInPackages(packageName: String) = Unit
    override fun onOpenInDeepLinks(uri: String) = Unit
    override fun onDismissFeedback() = Unit
}

// ──────────────────────────────────────────────────────────────────────────────
// @Preview функции
// ──────────────────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun NotificationsListLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        NotificationsScreen(
            component = PreviewNotificationsComponent(
                NotificationsState(
                    activeDeviceId       = "emulator-5554",
                    listState            = NotificationsListState.Success(previewRecords),
                    currentNotifications  = previewRecords,
                    snapshotHistory      = previewRecords,
                    displayedNotifications = previewRecords,
                    filter               = NotificationsFilter.ALL,
                )
            )
        )
    }
}

@Preview
@Composable
private fun NotificationsListDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        NotificationsScreen(
            component = PreviewNotificationsComponent(
                NotificationsState(
                    activeDeviceId       = "emulator-5554",
                    listState            = NotificationsListState.Success(previewRecords),
                    currentNotifications  = previewRecords,
                    snapshotHistory      = previewRecords,
                    displayedNotifications = previewRecords,
                    filter               = NotificationsFilter.ALL,
                )
            )
        )
    }
}

@Preview
@Composable
private fun NotificationsDetailPreview() {
    val selected = previewRecords[1]
    AdbDeckTheme(isDarkTheme = false) {
        NotificationsScreen(
            component = PreviewNotificationsComponent(
                NotificationsState(
                    activeDeviceId       = "emulator-5554",
                    listState            = NotificationsListState.Success(previewRecords),
                    currentNotifications  = previewRecords,
                    snapshotHistory      = previewRecords,
                    displayedNotifications = previewRecords,
                    selectedKey          = selected.key,
                    selectedRecord       = selected,
                    selectedTab          = NotificationsTab.DETAILS,
                )
            )
        )
    }
}

@Preview
@Composable
private fun NotificationsNoDevicePreview() {
    AdbDeckTheme(isDarkTheme = false) {
        NotificationsScreen(
            component = PreviewNotificationsComponent(
                NotificationsState(
                    listState = NotificationsListState.NoDevice,
                )
            )
        )
    }
}
