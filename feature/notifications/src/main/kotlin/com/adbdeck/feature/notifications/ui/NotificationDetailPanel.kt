package com.adbdeck.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.feature.notifications.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ──────────────────────────────────────────────────────────────────────────────
// Панель деталей уведомления
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Правая панель деталей выбранного уведомления.
 *
 * Содержит:
 * - Заголовок с packageName и title выбранного уведомления
 * - TabRow (Детали / Дамп / Сохранённые)
 * - Контент активной вкладки
 *
 * Объявлена как `internal`, чтобы быть видимой из [NotificationsScreen]
 * в том же модуле, но не снаружи.
 */
@Composable
internal fun NotificationDetailPanel(
    modifier: Modifier,
    record: NotificationRecord,
    selectedTab: NotificationsTab,
    savedNotifications: List<SavedNotification>,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // Заголовок панели
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = record.packageName,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (!record.title.isNullOrBlank()) {
                    Text(
                        text     = record.title!!,
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = component::onCloseDetail) {
                Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
            }
        }

        // Вкладки
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            NotificationsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick  = { component.onSelectTab(tab) },
                    text     = {
                        Text(
                            text  = when (tab) {
                                NotificationsTab.DETAILS  -> "Детали"
                                NotificationsTab.RAW_DUMP -> "Дамп"
                                NotificationsTab.SAVED    -> "Сохранённые (${savedNotifications.size})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            NotificationsTab.DETAILS ->
                DetailsTab(record = record, isSaved = isSaved, component = component)

            NotificationsTab.RAW_DUMP ->
                RawDumpTab(record = record, component = component)

            NotificationsTab.SAVED ->
                SavedTab(saved = savedNotifications, component = component)
        }
    }
}

// ── Вкладка DETAILS ──────────────────────────────────────────────────────────

@Composable
private fun DetailsTab(
    record: NotificationRecord,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    // extractUri выполняет regex по 5 полям — кэшируем по record
    val detectedUri = remember(record) { extractUri(record) }

    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.paddingSmall),
        ) {
            DetailRow("Пакет", record.packageName)
            DetailRow("ID / Tag", "${record.notificationId}${if (record.tag != null) " / ${record.tag}" else ""}")
            DetailRow("Ключ", record.key)
            record.channelId?.let    { DetailRow("Канал", it) }
            record.title?.let        { DetailRow("Заголовок", it) }
            record.text?.let         { DetailRow("Текст", it) }
            record.subText?.let      { DetailRow("Подтекст", it) }
            record.bigText?.let      { DetailRow("Big text", it) }
            record.summaryText?.let  { DetailRow("Summary", it) }
            record.category?.let     { DetailRow("Категория", it) }
            DetailRow("Важность", importanceLabel(record.importance))
            DetailRow("Флаги", "0x${record.flags.toString(16).uppercase().padStart(8, '0')}")
            DetailRow("Ongoing", record.isOngoing.toString())
            DetailRow("Clearable", record.isClearable.toString())
            record.postedAt?.let     { DetailRow("Дата", formatTimestampFull(it)) }
            record.group?.let        { DetailRow("Группа", it) }
            record.sortKey?.let      { DetailRow("Sort key", it) }

            if (record.actionsCount != null || record.actionTitles.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Text(
                    text     = "Actions",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
                record.actionsCount?.let { DetailRow("Количество", it.toString()) }
                if (record.actionTitles.isNotEmpty()) {
                    record.actionTitles.forEachIndexed { index, actionTitle ->
                        DetailRow("Действие ${index + 1}", actionTitle)
                    }
                } else {
                    Text(
                        text  = "Подписи действий не получены из dumpsys",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (record.imageParameters.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Text(
                    text     = "Визуальные параметры",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
                record.imageParameters.forEach { (key, value) ->
                    DetailRow(key, value)
                }
            }

            // Обнаруженный URI (для интеграции с Deep Links)
            if (detectedUri != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text     = detectedUri,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { component.onOpenInDeepLinks(detectedUri) }) {
                        Text("Deep Links", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))

            // Кнопки действий
            Text(
                text     = "Действия",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
            )
            ActionsGrid(record = record, isSaved = isSaved, component = component)
        }
    }
}

@Composable
private fun ActionsGrid(
    record: NotificationRecord,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        NotificationActionButton(Icons.Outlined.ContentCopy, "Копировать пакет") {
            component.onCopyPackageName(record)
        }
        if (!record.title.isNullOrBlank()) {
            NotificationActionButton(Icons.Outlined.Title, "Копировать заголовок") {
                component.onCopyTitle(record)
            }
        }
        if (!record.text.isNullOrBlank()) {
            NotificationActionButton(Icons.Outlined.TextFields, "Копировать текст") {
                component.onCopyText(record)
            }
        }
        if (!isSaved) {
            NotificationActionButton(Icons.Outlined.BookmarkAdd, "Сохранить в коллекцию") {
                component.onSaveNotification(record)
            }
        }
        NotificationActionButton(Icons.Outlined.Apps, "Открыть в Packages") {
            component.onOpenInPackages(record.packageName)
        }
        NotificationActionButton(Icons.Outlined.FileDownload, "Экспорт в JSON") {
            showSaveFileDialog(defaultName = "${record.packageName}_notif.json", ext = "json")
                ?.let { path -> component.onExportToJson(record, path) }
        }
    }
}

/**
 * Кнопка действия в панели деталей уведомления.
 * Вынесена как top-level private composable, чтобы Compose мог
 * независимо отслеживать её рекомпозицию.
 */
@Composable
private fun NotificationActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimensions.paddingSmall, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Dimensions.paddingXSmall))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text     = "$label:",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Вкладка RAW_DUMP ─────────────────────────────────────────────────────────

@Composable
private fun RawDumpTab(record: NotificationRecord, component: NotificationsComponent) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(Dimensions.paddingSmall),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick        = { component.onCopyRawDump(record) },
                contentPadding = PaddingValues(horizontal = Dimensions.paddingSmall, vertical = 4.dp),
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Dimensions.paddingXSmall))
                Text("Копировать", style = MaterialTheme.typography.labelSmall)
            }
        }
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.paddingSmall)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(Dimensions.paddingSmall),
            ) {
                Text(
                    text       = record.rawBlock.ifBlank { "(Исходный дамп недоступен — запись из сохранённых)" },
                    style      = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

// ── Вкладка SAVED ────────────────────────────────────────────────────────────

@Composable
private fun SavedTab(
    saved: List<SavedNotification>,
    component: NotificationsComponent,
) {
    if (saved.isEmpty()) {
        EmptyView(message = "Нет сохранённых уведомлений")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(saved, key = { it.id }) { item ->
                SavedNotificationRow(
                    item     = item,
                    onSelect = { component.onSelectNotification(item.record) },
                    onDelete = { component.onDeleteSaved(item.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SavedNotificationRow(
    item: SavedNotification,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = item.record.packageName,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (!item.record.title.isNullOrBlank()) {
                Text(
                    text     = item.record.title!!,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text  = "Сохранено: ${formatTimestampFull(item.savedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector        = Icons.Outlined.DeleteOutline,
                contentDescription = "Удалить",
                tint               = MaterialTheme.colorScheme.error,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Утилиты панели деталей (приватны в этом файле)
// ──────────────────────────────────────────────────────────────────────────────

/** Метка уровня важности для отображения в деталях. */
private fun importanceLabel(importance: Int): String = when (importance) {
    5    -> "5 — HIGH"
    4    -> "4 — DEFAULT"
    3    -> "3 — LOW"
    2    -> "2 — MIN"
    1    -> "1 — NONE"
    0    -> "0 — UNSPECIFIED"
    else -> importance.toString()
}

/** Полный формат даты и времени для деталей и сохранённых. */
private fun formatTimestampFull(millis: Long): String = try {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    formatter.format(Instant.ofEpochMilli(millis))
} catch (e: Exception) { millis.toString() }

/**
 * Попытаться извлечь URI из полей уведомления.
 * Ищет http/https или любую строку вида scheme://.
 * Вызывается через `remember(record)` в [DetailsTab].
 */
private fun extractUri(record: NotificationRecord): String? {
    val candidates = listOfNotNull(
        record.title, record.text, record.subText, record.bigText, record.summaryText,
    )
    val uriRegex = Regex("""(https?://\S+|[a-zA-Z][a-zA-Z0-9+\-.]{2,}://\S+)""")
    return candidates.firstNotNullOfOrNull { text ->
        uriRegex.find(text)?.value?.trimEnd('.', ',', ')', ']')
    }
}

/** Показать системный диалог сохранения файла. Возвращает путь или null. */
private fun showSaveFileDialog(defaultName: String, ext: String): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle  = "Экспорт"
    chooser.selectedFile = java.io.File(defaultName)
    chooser.fileFilter   = FileNameExtensionFilter("$ext-файлы", ext)
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val path = chooser.selectedFile.absolutePath
        if (!path.endsWith(".$ext")) "$path.$ext" else path
    } else null
}
