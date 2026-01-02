package com.adbdeck.feature.notifications.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

// ──────────────────────────────────────────────────────────────────────────────
// Корневой экран
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Корневой экран просмотра уведомлений Android через ADB.
 *
 * Содержит:
 * - Тулбар с кнопкой обновления, фильтрами и поиском
 * - Список уведомлений (LazyColumn) с поддержкой выбора
 * - Анимированную правую панель деталей (slideIn/slideOut), реализованную в [NotificationDetailPanel]
 * - Статусную строку
 * - Feedback-баннер ([AdbBanner])
 */
@Composable
fun NotificationsScreen(component: NotificationsComponent) {
    val state by component.state.collectAsState()

    // Set ключей сохранённых уведомлений — пересчитывается только при изменении savedNotifications,
    // а не при каждой рекомпозиции (смена выбранной записи, feedback, смена вкладки и т.д.)
    val savedKeys = remember(state.savedNotifications) {
        state.savedNotifications.map { it.record.key }.toSet()
    }

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
                                    savedKeys     = savedKeys,
                                    onSelect      = component::onSelectNotification,
                                )
                            }
                        }
                    }
                }

                // ── Панель деталей (NotificationDetailPanel.kt) ──────────────
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
                                modifier           = Modifier.width(400.dp).fillMaxHeight(),
                                record             = record,
                                selectedTab        = state.selectedTab,
                                savedNotifications = state.savedNotifications,
                                isSaved            = savedKeys.contains(record.key),
                                component          = component,
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
                message   = feedback.message,
                type      = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier  = Modifier
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
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            // Первая строка: Refresh + поиск + фильтр по пакету + сортировка
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                IconButton(onClick = component::onRefresh) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
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
                                text        = { Text(order.label()) },
                                onClick     = { component.onSortOrderChanged(order); sortMenuOpen = false },
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
                modifier              = Modifier.fillMaxWidth().padding(horizontal = Dimensions.paddingSmall).padding(bottom = Dimensions.paddingXSmall),
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
                record     = record,
                isSelected = record.key == selectedKey,
                isSaved    = record.key in savedKeys,
                onClick    = { onSelect(record) },
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
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        ImportanceBadge(importance = record.importance, modifier = Modifier.padding(top = 2.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = record.packageName,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (!record.title.isNullOrBlank()) {
                Text(
                    text       = record.title!!,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
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

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (record.isOngoing) {
                MiniChip(text = "ONGOING", containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            }
            if (isSaved) {
                MiniChip(text = "★", containerColor = MaterialTheme.colorScheme.secondaryContainer)
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
// Статусная строка
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsStatusBar(state: NotificationsState) {
    val currentCount   = state.currentNotifications.size
    val historyCount   = state.snapshotHistory.count { h ->
        state.currentNotifications.none { it.key == h.key }
    }
    val savedCount     = state.savedNotifications.size
    val displayedCount = state.displayedNotifications.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text("Показано: $displayedCount",  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text("Текущих: $currentCount",     style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text("В истории: $historyCount",   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("│", color = MaterialTheme.colorScheme.outlineVariant)
        Text("Сохранено: $savedCount",     style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.activeDeviceId != null) {
            Text("│", color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text       = state.activeDeviceId,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Вспомогательные composable-компоненты (используются в этом файле)
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
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

/** Маленький чип-бейдж для строки списка. */
@Composable
private fun MiniChip(text: String, containerColor: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = containerColor) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Утилиты экрана
// ──────────────────────────────────────────────────────────────────────────────

/** Краткий формат времени (HH:mm:ss) для строки списка. */
private fun formatTimestamp(millis: Long): String = try {
    DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
} catch (e: Exception) { "" }

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
