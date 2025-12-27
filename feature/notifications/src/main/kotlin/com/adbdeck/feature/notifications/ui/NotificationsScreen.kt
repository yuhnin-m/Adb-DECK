package com.adbdeck.feature.notifications.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.notifications.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ──────────────────────────────────────────────────────────────────────────────
// Корневой экран
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Корневой экран просмотра уведомлений Android через ADB.
 *
 * Содержит:
 * - Тулбар с кнопкой обновления, фильтрами и поиском
 * - Список уведомлений (LazyColumn) с поддержкой выбора
 * - Анимированную правую панель деталей (slideIn/slideOut)
 * - Статусную строку
 * - Feedback-баннер
 */
@Composable
fun NotificationsScreen(component: NotificationsComponent) {
    val state by component.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsToolbar(state = state, component = component)
            HorizontalDivider()

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // ── Список ───────────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val ls = state.listState) {
                        is NotificationsListState.NoDevice ->
                            EmptyView(message = "Выберите устройство в верхней панели")

                        is NotificationsListState.Loading ->
                            LoadingView(message = "Загрузка уведомлений…")

                        is NotificationsListState.Error ->
                            ErrorView(
                                message = ls.message,
                                onRetry = component::onRefresh,
                            )

                        is NotificationsListState.Success -> {
                            if (state.displayedNotifications.isEmpty()) {
                                EmptyView(
                                    message = if (state.searchQuery.isNotBlank() || state.packageFilter.isNotBlank())
                                        "Ничего не найдено"
                                    else
                                        "Уведомлений нет"
                                )
                            } else {
                                NotificationsList(
                                    notifications = state.displayedNotifications,
                                    selectedKey   = state.selectedKey,
                                    savedKeys     = state.savedNotifications.map { it.record.key }.toSet(),
                                    onSelect      = component::onSelectNotification,
                                )
                            }
                        }
                    }
                }

                // ── Панель деталей ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = state.selectedRecord != null,
                    enter   = slideInHorizontally { it },
                    exit    = slideOutHorizontally { it },
                ) {
                    val record = state.selectedRecord
                    if (record != null) {
                        Row {
                            VerticalDivider()
                            NotificationDetailPanel(
                                modifier         = Modifier.width(400.dp).fillMaxHeight(),
                                record           = record,
                                selectedTab      = state.selectedTab,
                                savedNotifications = state.savedNotifications,
                                isSaved          = state.savedNotifications.any { it.record.key == record.key },
                                component        = component,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            NotificationsStatusBar(state = state)
        }

        // ── Feedback-баннер ──────────────────────────────────────────────────
        state.feedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimensions.paddingDefault)
                    .padding(bottom = Dimensions.statusBarHeight),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Тулбар
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsToolbar(
    state: NotificationsState,
    component: NotificationsComponent,
) {
    Surface(
        color         = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            // Первая строка: кнопки + поиск
            Row(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                // Кнопка Refresh
                IconButton(onClick = component::onRefresh) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Outlined.Refresh,
                            contentDescription = "Обновить",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Поиск
                OutlinedTextField(
                    value         = state.searchQuery,
                    onValueChange = component::onSearchChanged,
                    modifier      = Modifier.weight(1f).height(48.dp),
                    placeholder   = { Text("Поиск по пакету, заголовку, тексту", style = MaterialTheme.typography.bodySmall) },
                    singleLine    = true,
                    textStyle     = MaterialTheme.typography.bodySmall,
                    trailingIcon  = if (state.searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { component.onSearchChanged("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Очистить")
                        } }
                    } else null,
                )

                // Фильтр по пакету
                OutlinedTextField(
                    value         = state.packageFilter,
                    onValueChange = component::onPackageFilterChanged,
                    modifier      = Modifier.width(200.dp).height(48.dp),
                    placeholder   = { Text("Пакет", style = MaterialTheme.typography.bodySmall) },
                    singleLine    = true,
                    textStyle     = MaterialTheme.typography.bodySmall,
                    trailingIcon  = if (state.packageFilter.isNotEmpty()) {
                        { IconButton(onClick = { component.onPackageFilterChanged("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Очистить")
                        } }
                    } else null,
                )

                // Сортировка
                var sortMenuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(
                            imageVector        = Icons.Outlined.Sort,
                            contentDescription = "Сортировка",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        NotificationsSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text     = { Text(order.label()) },
                                onClick  = { component.onSortOrderChanged(order); sortMenuOpen = false },
                                leadingIcon = if (state.sortOrder == order) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // Вторая строка: фильтр-чипы
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = Dimensions.paddingSmall).padding(bottom = Dimensions.paddingXSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                NotificationsFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick  = { component.onFilterChanged(filter) },
                        label    = {
                            Text(
                                text  = filter.label(state),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Список уведомлений
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsList(
    notifications: List<NotificationRecord>,
    selectedKey: String?,
    savedKeys: Set<String>,
    onSelect: (NotificationRecord) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = notifications,
            key   = { it.key },
        ) { record ->
            NotificationRow(
                record      = record,
                isSelected  = record.key == selectedKey,
                isSaved     = record.key in savedKeys,
                onClick     = { onSelect(record) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun NotificationRow(
    record: NotificationRecord,
    isSelected: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingSmall),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        // Иконка важности
        ImportanceBadge(importance = record.importance, modifier = Modifier.padding(top = 2.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Пакет
            Text(
                text      = record.packageName,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
            )
            // Заголовок
            if (!record.title.isNullOrBlank()) {
                Text(
                    text     = record.title!!,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Текст
            if (!record.text.isNullOrBlank()) {
                Text(
                    text     = record.text!!,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Правые бейджи
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (record.isOngoing) {
                MiniChip(text = "ONGOING", containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            }
            if (isSaved) {
                MiniChip(
                    text           = "★",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
            if (record.postedAt != null) {
                Text(
                    text  = formatTimestamp(record.postedAt!!),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Панель деталей
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationDetailPanel(
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
                    text  = record.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    text = "Actions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
                record.actionsCount?.let { DetailRow("Количество", it.toString()) }
                if (record.actionTitles.isNotEmpty()) {
                    record.actionTitles.forEachIndexed { index, actionTitle ->
                        DetailRow("Действие ${index + 1}", actionTitle)
                    }
                } else {
                    Text(
                        text = "Подписи действий не получены из dumpsys",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (record.imageParameters.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Text(
                    text = "Визуальные параметры",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
                record.imageParameters.forEach { (key, value) ->
                    DetailRow(key, value)
                }
            }

            // Обнаруженный URI (для интеграции с Deep Links)
            val detectedUri = extractUri(record)
            if (detectedUri != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
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
                text  = "Действия",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    @Composable
    fun ActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
        OutlinedButton(
            onClick       = onClick,
            modifier      = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Dimensions.paddingSmall, vertical = 4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Dimensions.paddingXSmall))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        ActionBtn(Icons.Outlined.ContentCopy, "Копировать пакет") { component.onCopyPackageName(record) }
        if (!record.title.isNullOrBlank()) {
            ActionBtn(Icons.Outlined.Title, "Копировать заголовок") { component.onCopyTitle(record) }
        }
        if (!record.text.isNullOrBlank()) {
            ActionBtn(Icons.Outlined.TextFields, "Копировать текст") { component.onCopyText(record) }
        }
        if (!isSaved) {
            ActionBtn(Icons.Outlined.BookmarkAdd, "Сохранить в коллекцию") { component.onSaveNotification(record) }
        }
        ActionBtn(Icons.Outlined.Apps, "Открыть в Packages") { component.onOpenInPackages(record.packageName) }
        ActionBtn(Icons.Outlined.FileDownload, "Экспорт в JSON") {
            showSaveFileDialog(defaultName = "${record.packageName}_notif.json", ext = "json")?.let { path ->
                component.onExportToJson(record, path)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text     = "$label:",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Вкладка RAW_DUMP ─────────────────────────────────────────────────────────

@Composable
private fun RawDumpTab(record: NotificationRecord, component: NotificationsComponent) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.paddingSmall),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = { component.onCopyRawDump(record) },
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                    item      = item,
                    onSelect  = { component.onSelectNotification(item.record) },
                    onDelete  = { component.onDeleteSaved(item.id) },
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
                text  = item.record.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
// Статусная строка
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsStatusBar(state: NotificationsState) {
    val currentCount   = state.currentNotifications.size
    val historyCount   = state.snapshotHistory.filter { h ->
        state.currentNotifications.none { it.key == h.key }
    }.size
    val savedCount     = state.savedNotifications.size
    val displayedCount = state.displayedNotifications.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text  = "Показано: $displayedCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text  = "Текущих: $currentCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text  = "В истории: $historyCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text  = "Сохранено: $savedCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.activeDeviceId != null) {
            Text("│", color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text  = state.activeDeviceId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Вспомогательные composable-компоненты
// ──────────────────────────────────────────────────────────────────────────────

/** Цветной бейдж уровня важности уведомления. */
@Composable
private fun ImportanceBadge(importance: Int, modifier: Modifier = Modifier) {
    val (color, label) = when (importance) {
        5    -> MaterialTheme.colorScheme.error to "!!!"
        4    -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f) to "!!"
        3    -> MaterialTheme.colorScheme.primary to "!"
        2    -> MaterialTheme.colorScheme.outline to "↓"
        1    -> MaterialTheme.colorScheme.outlineVariant to "—"
        else -> MaterialTheme.colorScheme.outlineVariant to "?"
    }
    Surface(
        modifier = modifier.size(22.dp),
        shape    = RoundedCornerShape(4.dp),
        color    = color.copy(alpha = 0.15f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/** Маленький чип-бейдж для состояний строки. */
@Composable
private fun MiniChip(text: String, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Утилиты
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

/** Краткий формат времени для строки списка. */
private fun formatTimestamp(millis: Long): String = try {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    formatter.format(Instant.ofEpochMilli(millis))
} catch (e: Exception) { "" }

/** Полный формат даты и времени для деталей. */
private fun formatTimestampFull(millis: Long): String = try {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    formatter.format(Instant.ofEpochMilli(millis))
} catch (e: Exception) { millis.toString() }

/**
 * Попытаться извлечь URI из полей уведомления.
 * Ищет строки, начинающиеся с http:// https:// или любую строку вида <scheme>://.
 */
private fun extractUri(record: NotificationRecord): String? {
    val candidates = listOfNotNull(record.title, record.text, record.subText, record.bigText, record.summaryText)
    val uriRegex = Regex("""(https?://\S+|[a-zA-Z][a-zA-Z0-9+\-.]{2,}://\S+)""")
    return candidates.firstNotNullOfOrNull { text ->
        uriRegex.find(text)?.value?.trimEnd('.', ',', ')', ']')
    }
}

/** Показать системный диалог сохранения файла. Возвращает путь или null. */
private fun showSaveFileDialog(defaultName: String, ext: String): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Экспорт"
    chooser.selectedFile = java.io.File(defaultName)
    chooser.fileFilter = FileNameExtensionFilter("$ext-файлы", ext)
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val path = chooser.selectedFile.absolutePath
        if (!path.endsWith(".$ext")) "$path.$ext" else path
    } else null
}

// ── Метки фильтров и сортировки ──────────────────────────────────────────────

private fun NotificationsFilter.label(state: NotificationsState): String = when (this) {
    NotificationsFilter.ALL        -> "Все"
    NotificationsFilter.CURRENT    -> "Текущие (${state.currentNotifications.size})"
    NotificationsFilter.HISTORICAL -> "История"
    NotificationsFilter.SAVED      -> "Сохранённые"
}

private fun NotificationsSortOrder.label(): String = when (this) {
    NotificationsSortOrder.NEWEST_FIRST -> "Сначала новые"
    NotificationsSortOrder.OLDEST_FIRST -> "Сначала старые"
    NotificationsSortOrder.BY_PACKAGE   -> "По пакету"
}
