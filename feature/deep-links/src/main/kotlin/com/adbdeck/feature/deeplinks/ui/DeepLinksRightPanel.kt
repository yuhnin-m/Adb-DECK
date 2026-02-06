package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.models.DeepLinksCommandResultUiState
import com.adbdeck.feature.deeplinks.models.DeepLinksRightPanelUiState
import com.adbdeck.feature.deeplinks.models.DeepLinksTab
import com.adbdeck.feature.deeplinks.models.IntentTemplate
import com.adbdeck.feature.deeplinks.models.LaunchHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DeepLinksRightPanel(
    state: DeepLinksRightPanelUiState,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TabRow(selectedTabIndex = state.rightTab.ordinal) {
            Tab(
                selected = state.rightTab == DeepLinksTab.COMMAND_RESULT,
                onClick = { component.onRightTabChanged(DeepLinksTab.COMMAND_RESULT) },
                text = { Text("Команда") },
            )
            Tab(
                selected = state.rightTab == DeepLinksTab.HISTORY,
                onClick = { component.onRightTabChanged(DeepLinksTab.HISTORY) },
                text = { Text("История (${state.history.size})") },
            )
            Tab(
                selected = state.rightTab == DeepLinksTab.TEMPLATES,
                onClick = { component.onRightTabChanged(DeepLinksTab.TEMPLATES) },
                text = { Text("Шаблоны (${state.templates.size})") },
            )
        }

        when (state.rightTab) {
            DeepLinksTab.COMMAND_RESULT -> CommandResultPanel(
                state = state.commandResult,
                modifier = Modifier.fillMaxSize(),
            )

            DeepLinksTab.HISTORY -> HistoryPanel(
                history = state.history,
                component = component,
                modifier = Modifier.fillMaxSize(),
            )

            DeepLinksTab.TEMPLATES -> TemplatesPanel(
                templates = state.templates,
                component = component,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CommandResultPanel(
    state: DeepLinksCommandResultUiState,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Предпросмотр команды",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(state.commandPreview)) },
                enabled = state.commandPreview.isNotBlank(),
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Копировать команду",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 180.dp),
        ) {
            val innerScroll = rememberScrollState()
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(innerScroll)
                        .padding(10.dp),
                ) {
                    Text(
                        text = state.commandPreview.ifBlank { "Заполните форму для предпросмотра команды" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (state.commandPreview.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        if (state.isLaunching) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Выполнение...", style = MaterialTheme.typography.bodySmall)
            }
        }

        state.lastResult?.let { result ->
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (result.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (result.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (result.isSuccess) "Успешно (exit ${result.exitCode})" else "Ошибка (exit ${result.exitCode})",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (result.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
            }

            if (result.stdout.isNotBlank()) {
                Text("stdout:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Text(
                            text = result.stdout,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            if (result.stderr.isNotBlank()) {
                Text("stderr:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Text(
                            text = result.stderr,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        } ?: run {
            if (!state.isLaunching) {
                Spacer(Modifier.height(24.dp))
                EmptyView(
                    message = "Результат появится после запуска",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HistoryPanel(
    history: List<LaunchHistoryEntry>,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (history.isEmpty()) {
            EmptyView("История запусков пуста", modifier = Modifier.fillMaxSize())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { component.onClearHistory() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Очистить историю", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { entry ->
                    HistoryEntryRow(
                        entry = entry,
                        onRestore = { component.onRestoreFromHistory(entry) },
                        onDelete = { component.onDeleteHistoryEntry(entry.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: LaunchHistoryEntry,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip(entry.mode)
                ResultChip(entry.isSuccess)
                Text(
                    text = formatTimestamp(entry.launchedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.commandPreview,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = "Восстановить параметры",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить запись",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TemplatesPanel(
    templates: List<IntentTemplate>,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            TextButton(onClick = { component.onShowSaveTemplateDialog() }) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Сохранить текущую конфигурацию", style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider()

        if (templates.isEmpty()) {
            EmptyView("Шаблоны не сохранены", modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onLaunch = { component.onLaunchTemplate(template) },
                        onRestore = { component.onRestoreFromTemplate(template) },
                        onDelete = { component.onDeleteTemplate(template.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: IntentTemplate,
    onLaunch: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip(template.mode)
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val paramsPreview = when (template.mode) {
                LaunchMode.DEEP_LINK -> template.deepLinkParams?.uri ?: ""
                LaunchMode.INTENT -> listOfNotNull(
                    template.intentParams?.action,
                    template.intentParams?.component,
                ).filter { it.isNotBlank() }.joinToString(", ")
            }
            if (paramsPreview.isNotBlank()) {
                Text(
                    text = paramsPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onLaunch) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Запустить шаблон",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = "Восстановить параметры",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить шаблон",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ModeChip(mode: LaunchMode) {
    val (label, color) = when (mode) {
        LaunchMode.DEEP_LINK -> Pair("DEEP LINK", MaterialTheme.colorScheme.tertiaryContainer)
        LaunchMode.INTENT -> Pair("INTENT", MaterialTheme.colorScheme.secondaryContainer)
    }
    val textColor = when (mode) {
        LaunchMode.DEEP_LINK -> MaterialTheme.colorScheme.onTertiaryContainer
        LaunchMode.INTENT -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ResultChip(isSuccess: Boolean) {
    val (label, color, textColor) = if (isSuccess) {
        Triple("OK", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    } else {
        Triple("ERR", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ms))
}
