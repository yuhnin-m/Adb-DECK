package com.adbdeck.feature.notifications.ui

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import java.awt.EventQueue
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
): String? {
    val chooser = JFileChooser().apply {
        this.dialogTitle = dialogTitle
        this.selectedFile = File(defaultName)
        this.fileFilter = FileNameExtensionFilter(filterDescription, extension)
    }

    fun extractSelectedPath(): String {
        val selectedPath = chooser.selectedFile.absolutePath
        return if (selectedPath.endsWith(".$extension", ignoreCase = true)) {
            selectedPath
        } else {
            "$selectedPath.$extension"
        }
    }

    if (EventQueue.isDispatchThread()) {
        return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            extractSelectedPath()
        } else {
            null
        }
    }

    var selectedPath: String? = null
    EventQueue.invokeAndWait {
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedPath = extractSelectedPath()
        }
    }
    return selectedPath
}

internal fun showNotificationOpenImageFileDialog(
    dialogTitle: String,
): String? {
    val chooser = JFileChooser().apply {
        this.dialogTitle = dialogTitle
        this.fileFilter = FileNameExtensionFilter(
            "Images (PNG, JPG, JPEG, WEBP)",
            "png",
            "jpg",
            "jpeg",
            "webp",
        )
        isAcceptAllFileFilterUsed = true
    }

    if (EventQueue.isDispatchThread()) {
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }

    var selectedPath: String? = null
    EventQueue.invokeAndWait {
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedPath = chooser.selectedFile?.absolutePath
        }
    }
    return selectedPath
}
