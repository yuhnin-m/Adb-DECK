package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.ExtraType
import com.adbdeck.core.adb.api.IntentExtra
import com.adbdeck.core.adb.api.LaunchMode
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.DeepLinksState
import com.adbdeck.feature.deeplinks.DeepLinksTab
import com.adbdeck.feature.deeplinks.IntentTemplate
import com.adbdeck.feature.deeplinks.LaunchHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Главный экран Deep Links / Intents.
 *
 * Макет:
 * - Левая панель (420dp): переключатель режима + форма + кнопка запуска
 * - Правая панель: три вкладки — Команда/Результат, История, Шаблоны
 * - Строка состояния снизу
 * - Диалог сохранения шаблона (если открыт)
 *
 * @param component Компонент экрана.
 */
@Composable
fun DeepLinksScreen(component: DeepLinksComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Основной контент ──────────────────────────────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Левая: форма
            DeepLinksFormPanel(
                state     = state,
                component = component,
                modifier  = Modifier.width(420.dp).fillMaxHeight(),
            )

            VerticalDivider()

            // Правая: вкладки
            DeepLinksRightPanel(
                state     = state,
                component = component,
                modifier  = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        HorizontalDivider()

        // ── Строка состояния ──────────────────────────────────────────────────
        DeepLinksStatusBar(state = state)
    }

    // ── Диалог сохранения шаблона ─────────────────────────────────────────────
    if (state.isSaveTemplateDialogOpen) {
        SaveTemplateDialog(
            name          = state.saveTemplateName,
            onNameChanged = { component.onSaveTemplateNameChanged(it) },
            onConfirm     = { component.onConfirmSaveTemplate() },
            onDismiss     = { component.onDismissSaveTemplateDialog() },
        )
    }
}

// ── Левая панель: форма ────────────────────────────────────────────────────────

@Composable
private fun DeepLinksFormPanel(
    state: DeepLinksState,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Переключатель режима
        TabRow(selectedTabIndex = state.mode.ordinal) {
            Tab(
                selected = state.mode == LaunchMode.DEEP_LINK,
                onClick  = { component.onModeChanged(LaunchMode.DEEP_LINK) },
                text     = { Text("Deep Link") },
            )
            Tab(
                selected = state.mode == LaunchMode.INTENT,
                onClick  = { component.onModeChanged(LaunchMode.INTENT) },
                text     = { Text("Intent") },
            )
        }

        // Прокручиваемые поля формы
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.mode) {
                LaunchMode.DEEP_LINK -> DeepLinkFormFields(state, component)
                LaunchMode.INTENT    -> IntentFormFields(state, component)
            }
        }

        HorizontalDivider()

        // Кнопка запуска
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Button(
                onClick  = { component.onLaunch() },
                enabled  = state.activeDeviceId != null && !state.isLaunching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLaunching) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector        = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isLaunching) "Запуск..." else "Запустить")
            }
        }
    }
}

// ── Поля формы Deep Link ───────────────────────────────────────────────────────

@Composable
private fun DeepLinkFormFields(state: DeepLinksState, component: DeepLinksComponent) {
    FormField(
        label       = "URI *",
        value       = state.dlUri,
        placeholder = "https://example.com/path?param=value",
        onValueChange = { component.onDlUriChanged(it) },
    )
    FormField(
        label       = "Action",
        value       = state.dlAction,
        placeholder = "android.intent.action.VIEW",
        onValueChange = { component.onDlActionChanged(it) },
    )
    FormField(
        label       = "Package (опционально)",
        value       = state.dlPackage,
        placeholder = "com.example.myapp",
        onValueChange = { component.onDlPackageChanged(it) },
    )
    FormField(
        label       = "Component (опционально)",
        value       = state.dlComponent,
        placeholder = "com.example.myapp/.MainActivity",
        onValueChange = { component.onDlComponentChanged(it) },
    )
    FormField(
        label       = "Category (опционально)",
        value       = state.dlCategory,
        placeholder = "android.intent.category.DEFAULT",
        onValueChange = { component.onDlCategoryChanged(it) },
    )
}

// ── Поля формы Intent ─────────────────────────────────────────────────────────

@Composable
private fun IntentFormFields(state: DeepLinksState, component: DeepLinksComponent) {
    FormField(
        label       = "Action",
        value       = state.itAction,
        placeholder = "android.intent.action.MAIN",
        onValueChange = { component.onItActionChanged(it) },
    )
    FormField(
        label       = "Data URI",
        value       = state.itDataUri,
        placeholder = "content://... или https://...",
        onValueChange = { component.onItDataUriChanged(it) },
    )
    FormField(
        label       = "Package",
        value       = state.itPackage,
        placeholder = "com.example.myapp",
        onValueChange = { component.onItPackageChanged(it) },
    )
    FormField(
        label       = "Component",
        value       = state.itComponent,
        placeholder = "com.example.myapp/.MainActivity",
        onValueChange = { component.onItComponentChanged(it) },
    )
    FormField(
        label       = "Flags (hex)",
        value       = state.itFlags,
        placeholder = "0x10000000",
        onValueChange = { component.onItFlagsChanged(it) },
    )

    // Categories
    HorizontalDivider()
    CategoriesEditor(
        categories = state.itCategories,
        onAdd      = { component.onItCategoryAdd(it) },
        onRemove   = { component.onItCategoryRemove(it) },
    )

    // Extras
    HorizontalDivider()
    ExtrasEditor(
        extras          = state.itExtras,
        onAdd           = { component.onItExtraAdd() },
        onRemove        = { component.onItExtraRemove(it) },
        onKeyChanged    = { i, k -> component.onItExtraKeyChanged(i, k) },
        onTypeChanged   = { i, t -> component.onItExtraTypeChanged(i, t) },
        onValueChanged  = { i, v -> component.onItExtraValueChanged(i, v) },
    )
}

// ── Компонент редактирования categories ──────────────────────────────────────

@Composable
private fun CategoriesEditor(
    categories: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var newCategory by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = "Categories",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        categories.forEachIndexed { index, category ->
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = category,
                    style    = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick  = { onRemove(index) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value         = newCategory,
                onValueChange = { newCategory = it },
                placeholder   = { Text("android.intent.category.DEFAULT", style = MaterialTheme.typography.bodySmall) },
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodySmall,
            )
            IconButton(
                onClick  = {
                    if (newCategory.isNotBlank()) {
                        onAdd(newCategory.trim())
                        newCategory = ""
                    }
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Добавить category")
            }
        }
    }
}

// ── Компонент редактирования extras ──────────────────────────────────────────

@Composable
private fun ExtrasEditor(
    extras: List<IntentExtra>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onKeyChanged: (Int, String) -> Unit,
    onTypeChanged: (Int, ExtraType) -> Unit,
    onValueChanged: (Int, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = "Extras",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        extras.forEachIndexed { index, extra ->
            ExtraRow(
                extra          = extra,
                onRemove       = { onRemove(index) },
                onKeyChanged   = { onKeyChanged(index, it) },
                onTypeChanged  = { onTypeChanged(index, it) },
                onValueChanged = { onValueChanged(index, it) },
            )
        }
        TextButton(
            onClick  = onAdd,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Добавить параметр", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ExtraRow(
    extra: IntentExtra,
    onRemove: () -> Unit,
    onKeyChanged: (String) -> Unit,
    onTypeChanged: (ExtraType) -> Unit,
    onValueChanged: (String) -> Unit,
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Ключ
        OutlinedTextField(
            value         = extra.key,
            onValueChange = onKeyChanged,
            placeholder   = { Text("key", style = MaterialTheme.typography.bodySmall) },
            modifier      = Modifier.weight(0.38f),
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodySmall,
        )

        // Тип
        Box(modifier = Modifier.weight(0.28f)) {
            TextButton(
                onClick  = { typeMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(extra.type.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            DropdownMenu(
                expanded         = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                ExtraType.entries.forEach { type ->
                    DropdownMenuItem(
                        text    = { Text(type.label) },
                        onClick = { typeMenuExpanded = false; onTypeChanged(type) },
                    )
                }
            }
        }

        // Значение
        OutlinedTextField(
            value         = extra.value,
            onValueChange = onValueChanged,
            placeholder   = { Text("value", style = MaterialTheme.typography.bodySmall) },
            modifier      = Modifier.weight(0.38f),
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodySmall,
        )

        // Удалить
        IconButton(
            onClick  = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Удалить extra", modifier = Modifier.size(16.dp))
        }
    }
}

// ── Вспомогательный компонент поля ───────────────────────────────────────────

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(placeholder, style = MaterialTheme.typography.bodySmall)
            },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ── Правая панель ─────────────────────────────────────────────────────────────

@Composable
private fun DeepLinksRightPanel(
    state: DeepLinksState,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TabRow(selectedTabIndex = state.rightTab.ordinal) {
            Tab(
                selected = state.rightTab == DeepLinksTab.COMMAND_RESULT,
                onClick  = { component.onRightTabChanged(DeepLinksTab.COMMAND_RESULT) },
                text     = { Text("Команда") },
            )
            Tab(
                selected = state.rightTab == DeepLinksTab.HISTORY,
                onClick  = { component.onRightTabChanged(DeepLinksTab.HISTORY) },
                text     = { Text("История (${state.history.size})") },
            )
            Tab(
                selected = state.rightTab == DeepLinksTab.TEMPLATES,
                onClick  = { component.onRightTabChanged(DeepLinksTab.TEMPLATES) },
                text     = { Text("Шаблоны (${state.templates.size})") },
            )
        }

        when (state.rightTab) {
            DeepLinksTab.COMMAND_RESULT -> CommandResultPanel(
                state    = state,
                modifier = Modifier.fillMaxSize(),
            )
            DeepLinksTab.HISTORY -> HistoryPanel(
                state     = state,
                component = component,
                modifier  = Modifier.fillMaxSize(),
            )
            DeepLinksTab.TEMPLATES -> TemplatesPanel(
                state     = state,
                component = component,
                modifier  = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Вкладка Команда / Результат ───────────────────────────────────────────────

@Composable
private fun CommandResultPanel(
    state: DeepLinksState,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Заголовок с кнопкой копирования
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "Предпросмотр команды",
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick  = { clipboardManager.setText(AnnotatedString(state.commandPreview)) },
                enabled  = state.commandPreview.isNotBlank(),
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.ContentCopy,
                    contentDescription = "Копировать команду",
                    modifier           = Modifier.size(18.dp),
                )
            }
        }

        // Блок с командой
        Surface(
            color    = MaterialTheme.colorScheme.surfaceVariant,
            shape    = MaterialTheme.shapes.small,
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
                        text  = state.commandPreview.ifBlank { "Заполните форму для предпросмотра команды" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (state.commandPreview.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Результат последнего запуска
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
                    imageVector        = if (result.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint               = if (result.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    text  = if (result.isSuccess) "Успешно (exit ${result.exitCode})" else "Ошибка (exit ${result.exitCode})",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (result.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
            }

            if (result.stdout.isNotBlank()) {
                Text("stdout:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    shape    = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Text(
                            text     = result.stdout,
                            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            if (result.stderr.isNotBlank()) {
                Text("stderr:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    shape    = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Text(
                            text     = result.stderr,
                            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        } ?: run {
            if (!state.isLaunching) {
                Spacer(Modifier.height(24.dp))
                EmptyView(
                    message  = "Результат появится после запуска",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Вкладка История ───────────────────────────────────────────────────────────

@Composable
private fun HistoryPanel(
    state: DeepLinksState,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.history.isEmpty()) {
            EmptyView("История запусков пуста", modifier = Modifier.fillMaxSize())
        } else {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                items(state.history, key = { it.id }) { entry ->
                    HistoryEntryRow(
                        entry     = entry,
                        onRestore = { component.onRestoreFromHistory(entry) },
                        onDelete  = { component.onDeleteHistoryEntry(entry.id) },
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
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                ModeChip(entry.mode)
                ResultChip(entry.isSuccess)
                Text(
                    text  = formatTimestamp(entry.launchedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text     = entry.commandPreview,
                style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector        = Icons.Outlined.Restore,
                contentDescription = "Восстановить параметры",
                modifier           = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector        = Icons.Outlined.Delete,
                contentDescription = "Удалить запись",
                modifier           = Modifier.size(18.dp),
                tint               = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Вкладка Шаблоны ───────────────────────────────────────────────────────────

@Composable
private fun TemplatesPanel(
    state: DeepLinksState,
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

        if (state.templates.isEmpty()) {
            EmptyView("Шаблоны не сохранены", modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.templates, key = { it.id }) { template ->
                    TemplateRow(
                        template  = template,
                        onLaunch  = { component.onLaunchTemplate(template) },
                        onRestore = { component.onRestoreFromTemplate(template) },
                        onDelete  = { component.onDeleteTemplate(template.id) },
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
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                ModeChip(template.mode)
                Text(
                    text       = template.name,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val paramsPreview = when (template.mode) {
                LaunchMode.DEEP_LINK -> template.deepLinkParams?.uri ?: ""
                LaunchMode.INTENT    -> listOfNotNull(
                    template.intentParams?.action,
                    template.intentParams?.component,
                ).filter { it.isNotBlank() }.joinToString(", ")
            }
            if (paramsPreview.isNotBlank()) {
                Text(
                    text     = paramsPreview,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onLaunch) {
            Icon(
                imageVector        = Icons.Outlined.PlayArrow,
                contentDescription = "Запустить шаблон",
                modifier           = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector        = Icons.Outlined.Restore,
                contentDescription = "Восстановить параметры",
                modifier           = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector        = Icons.Outlined.Delete,
                contentDescription = "Удалить шаблон",
                modifier           = Modifier.size(18.dp),
                tint               = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Диалог сохранения шаблона ─────────────────────────────────────────────────

@Composable
private fun SaveTemplateDialog(
    name: String,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Сохранить шаблон") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Введите название для текущей конфигурации:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value         = name,
                    onValueChange = onNameChanged,
                    placeholder   = { Text("Название шаблона") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

// ── Строка состояния ──────────────────────────────────────────────────────────

@Composable
private fun DeepLinksStatusBar(state: DeepLinksState) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = if (state.activeDeviceId != null)
                "Устройство: ${state.activeDeviceId}"
            else
                "Устройство не выбрано",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text  = "История: ${state.history.size} | Шаблоны: ${state.templates.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Вспомогательные чипы ──────────────────────────────────────────────────────

@Composable
private fun ModeChip(mode: LaunchMode) {
    val (label, color) = when (mode) {
        LaunchMode.DEEP_LINK -> Pair("DEEP LINK", MaterialTheme.colorScheme.tertiaryContainer)
        LaunchMode.INTENT    -> Pair("INTENT", MaterialTheme.colorScheme.secondaryContainer)
    }
    val textColor = when (mode) {
        LaunchMode.DEEP_LINK -> MaterialTheme.colorScheme.onTertiaryContainer
        LaunchMode.INTENT    -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = textColor,
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
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ── Утилита форматирования времени ────────────────────────────────────────────

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ms))
}
