package com.adbdeck.feature.notifications.ui

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.ui.filedialogs.HostFileDialogFilter
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.SaveFileDialogConfig
import com.adbdeck.core.ui.filedialogs.SaveFileExtensionPolicy
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.filedialogs.showSaveFileDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SHORT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private val FULL_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private val NOTIFICATION_URI_REGEX = Regex("""(https?://\S+|[a-zA-Z][a-zA-Z0-9+\-.]{2,}://\S+)""")

internal fun formatNotificationShortTime(millis: Long): String =
    runCatching {
        SHORT_TIME_FORMATTER.format(Instant.ofEpochMilli(millis))
    }.getOrDefault("")

internal fun formatNotificationFullTime(millis: Long): String =
    runCatching {
        FULL_TIME_FORMATTER.format(Instant.ofEpochMilli(millis))
    }.getOrDefault(millis.toString())

internal fun extractNotificationUri(record: NotificationRecord): String? {
    val candidates = listOfNotNull(
        record.title,
        record.text,
        record.subText,
        record.bigText,
        record.summaryText,
    )

    return candidates.firstNotNullOfOrNull { text ->
        NOTIFICATION_URI_REGEX.find(text)?.value?.trimEnd('.', ',', ')', ']')
    }
}

internal fun showNotificationSaveFileDialog(
    defaultName: String,
    extension: String,
    dialogTitle: String,
    filterDescription: String,
): String? = showSaveFileDialog(
    SaveFileDialogConfig(
        title = dialogTitle,
        defaultFileName = defaultName,
        filters = listOf(
            HostFileDialogFilter(
                description = filterDescription,
                extensions = listOf(extension),
            ),
        ),
        // Сохраняем прежнее поведение Notifications: accept-all включен.
        isAcceptAllFileFilterUsed = true,
        extensionPolicy = SaveFileExtensionPolicy.AppendIfMissing(defaultExtension = extension),
    ),
)

internal fun showNotificationOpenImageFileDialog(
    dialogTitle: String,
): String? = showOpenFileDialog(
    OpenFileDialogConfig(
        title = dialogTitle,
        selectionMode = HostFileSelectionMode.FILES_ONLY,
        filters = listOf(
            HostFileDialogFilter(
                description = "Images (PNG, JPG, JPEG, WEBP)",
                extensions = listOf("png", "jpg", "jpeg", "webp"),
            ),
        ),
        isAcceptAllFileFilterUsed = true,
    ),
)
