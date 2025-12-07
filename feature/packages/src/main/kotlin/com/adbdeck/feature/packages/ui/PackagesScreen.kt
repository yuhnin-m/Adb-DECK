package com.adbdeck.feature.packages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageType
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.packages.PackageTypeFilter
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.packages.PackagesListState
import com.adbdeck.feature.packages.PackagesState
import com.adbdeck.feature.packages.PendingPackageAction

/**
 * Корневой composable экрана пакетов.
 *
 * Компоновка (desktop master-detail):
 * ```
 * Column {
 *   PackagesToolbar          (фильтры, поиск, обновление)
 *   HorizontalDivider
 *   Row {
 *     PackagesContent        (список или пустое/загрузка/ошибка — занимает 55% или 100%)
 *     VerticalDivider        (только при выбранном пакете)
 *     PackageDetailPanel     (только при выбранном пакете — 45%)
 *   }
 *   HorizontalDivider
 *   PackagesStatusBar
 * }
 * + ConfirmationDialog (при pendingAction)
 * + AdbBanner (при actionFeedback)
 * ```
 *
 * @param component Компонент пакетов.
 */
@Composable
fun PackagesScreen(component: PackagesComponent) {
    val state by component.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Панель инструментов ───────────────────────────────
            PackagesToolbar(state = state, component = component)
            HorizontalDivider()

            // ── Основная область (мастер + деталь) ───────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val hasDetail = state.selectedPackage != null

                // Список пакетов: 55% ширины при открытой панели, 100% — без
                Box(modifier = if (hasDetail) Modifier.weight(0.55f) else Modifier.fillMaxWidth()) {
                    PackagesContent(state = state, component = component, clipboard = clipboard)
                }

                // Вертикальный разделитель и панель деталей
                if (hasDetail) {
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    PackageDetailPanel(
                        state = state,
                        component = component,
                        modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    )
                }
            }

            HorizontalDivider()

            // ── Строка состояния ──────────────────────────────────
            PackagesStatusBar(state = state)
        }

        // ── Feedback-баннер (поверх контента, снизу) ─────────────
        state.actionFeedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.VARNING else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier.align(Alignment.BottomCenter).padding(Dimensions.paddingDefault),
            )
        }
    }

    // ── Диалог подтверждения деструктивных действий ───────────────
    state.pendingAction?.let { action ->
        ConfirmationDialog(action = action, component = component)
    }
}

// ── Панель инструментов ───────────────────────────────────────────────────────

/**
 * Панель с кнопкой обновления, фильтрами по типу и полем поиска.
 */
@Composable
private fun PackagesToolbar(
    state: PackagesState,
    component: PackagesComponent,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            // ── Кнопка обновления ─────────────────────────────────
            IconButton(
                onClick = component::onRefresh,
                enabled = state.listState !is PackagesListState.Loading,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Обновить список пакетов",
                    modifier = Modifier.size(Dimensions.iconSizeNav),
                )
            }

            // ── Фильтр по типу ────────────────────────────────────
            listOf(
                PackageTypeFilter.ALL to "Все",
                PackageTypeFilter.USER to "User",
                PackageTypeFilter.SYSTEM to "System",
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = state.typeFilter == filter,
                    onClick = { component.onTypeFilterChanged(filter) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }

            Spacer(Modifier.width(Dimensions.paddingSmall))

            // ── Поле поиска ───────────────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = component::onSearchChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Поиск по имени или пути…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
                trailingIcon = if (state.searchQuery.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { component.onSearchChanged("") },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Очистить поиск",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ── Основная область контента ─────────────────────────────────────────────────

/**
 * Отображает список пакетов или соответствующее состояние (загрузка / ошибка / пусто / нет устройства).
 */
@Composable
private fun PackagesContent(
    state: PackagesState,
    component: PackagesComponent,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
) {
    when (val listState = state.listState) {
        is PackagesListState.NoDevice -> EmptyView(
            message = "Устройство не выбрано.\nВыберите устройство в верхней панели.",
        )
        is PackagesListState.Loading -> LoadingView(message = "Загрузка списка пакетов…")
        is PackagesListState.Error -> ErrorView(
            message = listState.message,
            onRetry = component::onRefresh,
        )
        is PackagesListState.Success -> {
            if (state.filteredPackages.isEmpty()) {
                EmptyView(
                    message = if (state.searchQuery.isNotBlank() || state.typeFilter != PackageTypeFilter.ALL)
                        "Нет пакетов, соответствующих фильтру"
                    else
                        "Нет установленных пакетов",
                )
            } else {
                PackagesList(
                    packages = state.filteredPackages,
                    selectedPackageName = state.selectedPackage?.packageName,
                    isActionRunning = state.isActionRunning,
                    component = component,
                    clipboard = clipboard,
                )
            }
        }
    }
}

// ── Список пакетов ────────────────────────────────────────────────────────────

/**
 * LazyColumn со строками пакетов. Использует `key { packageName }` для корректного recomposition.
 */
@Composable
private fun PackagesList(
    packages: List<AppPackage>,
    selectedPackageName: String?,
    isActionRunning: Boolean,
    component: PackagesComponent,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(packages, key = { it.packageName }) { pkg ->
            PackageRow(
                pkg = pkg,
                isSelected = pkg.packageName == selectedPackageName,
                isActionRunning = isActionRunning,
                onClick = { component.onSelectPackage(pkg) },
                onLaunch = { component.onLaunchApp(pkg) },
                onStop = { component.onForceStop(pkg) },
                onCopy = {
                    clipboard.setText(AnnotatedString(pkg.packageName))
                    component.onCopyPackageName(pkg)
                },
                onInfo = { component.onOpenAppInfo(pkg) },
                onUninstall = { component.onRequestUninstall(pkg) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        }
    }
}

// ── Строка пакета ─────────────────────────────────────────────────────────────

/**
 * Читаемое отображаемое имя, производное от имени пакета.
 *
 * Берёт последний сегмент reverse-DNS-имени и капитализирует его:
 * `com.android.settings` → `Settings`, `org.mozilla.firefox` → `Firefox`.
 *
 * Используется как заголовок строки в списке, пока настоящий appLabel не загружен.
 */
private val AppPackage.displayName: String
    get() = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

/**
 * Строка одного пакета в списке.
 *
 * Структура:
 * ```
 * [Иконка]  [Название приложения (displayName)]   [USER/SYSTEM]
 *           [com.package.name (monospace, мелко)]
 *           [Launch] [Stop] [Copy] [Info] [Удалить]
 * ```
 *
 * При выборе подсвечивается primaryContainer-фоном.
 */
@Composable
private fun PackageRow(
    pkg: AppPackage,
    isSelected: Boolean,
    isActionRunning: Boolean,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onCopy: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── Строка заголовка: название + бейдж типа ──────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Название и имя пакета — вертикально
            Column(modifier = Modifier.weight(1f)) {
                // Заголовок: человекочитаемое название (последний сегмент package name)
                Text(
                    text = pkg.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Подзаголовок: полное reverse-DNS имя пакета
                Text(
                    text = pkg.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PackageTypeBadge(type = pkg.type)
        }

        // ── Кнопки действий ──────────────────────────────────────
        Row(
            modifier = Modifier.padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onLaunch,
                enabled = !isActionRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, "Запустить", Modifier.size(16.dp))
            }
            IconButton(
                onClick = onStop,
                enabled = !isActionRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Stop, "Остановить", Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.ContentCopy, "Скопировать", Modifier.size(14.dp))
            }
            IconButton(
                onClick = onInfo,
                enabled = !isActionRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Outlined.Info, "Инфо", Modifier.size(16.dp))
            }
            IconButton(
                onClick = onUninstall,
                enabled = !isActionRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete, "Удалить", Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Бейдж типа пакета ─────────────────────────────────────────────────────────

/**
 * Небольшой цветной бейдж USER / SYSTEM.
 */
@Composable
private fun PackageTypeBadge(type: PackageType) {
    val (color, label) = when (type) {
        PackageType.USER -> Pair(MaterialTheme.colorScheme.primary, "USER")
        PackageType.SYSTEM -> Pair(MaterialTheme.colorScheme.tertiary, "SYS")
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = Dimensions.paddingSmall, vertical = 1.dp),
        )
    }
}

// ── Строка состояния ──────────────────────────────────────────────────────────

/**
 * Нижняя строка состояния: количество пакетов (всего / отфильтровано).
 */
@Composable
private fun PackagesStatusBar(state: PackagesState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        // Индикатор загрузки
        val dotColor = when (state.listState) {
            is PackagesListState.NoDevice -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            is PackagesListState.Loading -> Color(0xFFFF9800)
            is PackagesListState.Success -> Color(0xFF4CAF50)
            is PackagesListState.Error -> MaterialTheme.colorScheme.error
        }
        Box(Modifier.size(8.dp).background(dotColor, CircleShape))

        val totalCount = (state.listState as? PackagesListState.Success)?.packages?.size ?: 0
        val shownCount = state.filteredPackages.size

        val statusText = when (state.listState) {
            is PackagesListState.NoDevice -> "Нет активного устройства"
            is PackagesListState.Loading -> "Загрузка…"
            is PackagesListState.Error -> "Ошибка"
            is PackagesListState.Success -> if (shownCount == totalCount) "$totalCount пакетов"
            else "$shownCount из $totalCount пакетов"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.isActionRunning) {
            Text("│", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                "Выполняется операция…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Диалог подтверждения ──────────────────────────────────────────────────────

/**
 * Диалог подтверждения деструктивного действия (удаление / очистка данных).
 */
@Composable
private fun ConfirmationDialog(
    action: PendingPackageAction,
    component: PackagesComponent,
) {
    val (title, text) = when (action) {
        is PendingPackageAction.ClearData -> Pair(
            "Очистить данные",
            "Очистить данные приложения «${action.pkg.packageName}»?\n\nДанные, кэш и настройки будут удалены безвозвратно.",
        )
        is PendingPackageAction.Uninstall -> Pair(
            "Удалить приложение",
            "Удалить пакет «${action.pkg.packageName}»?\n\nПриложение будет полностью удалено с устройства.",
        )
    }

    AlertDialog(
        onDismissRequest = component::onCancelAction,
        title = { Text(title) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = component::onConfirmAction,
            ) {
                Text("Подтвердить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = component::onCancelAction) {
                Text("Отмена")
            }
        },
    )
}
