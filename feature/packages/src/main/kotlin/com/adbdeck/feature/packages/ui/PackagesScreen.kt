package com.adbdeck.feature.packages.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.core.ui.textfields.AdbTextFieldType
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
    val controlCornerRadius = AdbCornerRadius.MEDIUM

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
            AdbOutlinedButton(
                onClick = component::onRefresh,
                enabled = state.listState !is PackagesListState.Loading,
                loading = state.listState is PackagesListState.Loading,
                text = "Обновить",
                leadingIcon = Icons.Outlined.Refresh,
                contentDescription = "Обновить список пакетов",
                size = AdbButtonSize.MEDIUM,
                cornerRadius = controlCornerRadius,
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // ── Фильтр по типу (центр между refresh и search) ──
                AdbSingleSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption(value = PackageTypeFilter.ALL, label = "Все"),
                        AdbSegmentedOption(value = PackageTypeFilter.USER, label = "User"),
                        AdbSegmentedOption(value = PackageTypeFilter.SYSTEM, label = "System"),
                    ),
                    selectedValue = state.typeFilter,
                    onValueSelected = component::onTypeFilterChanged,
                    size = AdbSegmentedButtonSize.MEDIUM,
                    cornerRadius = controlCornerRadius,
                )
            }

            // ── Поле поиска (справа) ──────────────────────────────
            AdbOutlinedTextField(
                value = state.searchQuery,
                onValueChange = component::onSearchChanged,
                modifier = Modifier.width(320.dp),
                placeholder = "Поиск по имени или пути…",
                type = AdbTextFieldType.NEUTRAL,
                size = AdbTextFieldSize.MEDIUM,
                cornerRadius = controlCornerRadius,
                singleLine = true,
                leadingIcon = Icons.Outlined.Search,
                trailingIcon = if (state.searchQuery.isNotEmpty()) Icons.Outlined.Close else null,
                onTrailingIconClick = if (state.searchQuery.isNotEmpty()) {
                    { component.onSearchChanged("") }
                } else {
                    null
                },
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
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
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Surface(
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Dimensions.paddingDefault,
                vertical = Dimensions.paddingSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            // ── Строка заголовка ──────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pkg.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Метаданные ────────────────────────────────────────
            Row(
                modifier = Modifier.padding(start = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                PackageTypeBadge(type = pkg.type)
                PackageEnabledBadge(isEnabled = pkg.isEnabled)
                if (isSelected) {
                    MetaBadge(
                        label = "ACTIVE",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── Кнопки действий ───────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                AdbOutlinedButton(
                    onClick = onLaunch,
                    enabled = !isActionRunning,
                    text = "Запуск",
                    leadingIcon = Icons.Outlined.PlayArrow,
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
                AdbOutlinedButton(
                    onClick = onStop,
                    enabled = !isActionRunning,
                    text = "Стоп",
                    leadingIcon = Icons.Outlined.Stop,
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
                Spacer(modifier = Modifier.weight(1f))
                AdbPlainButton(
                    onClick = onCopy,
                    leadingIcon = Icons.Outlined.ContentCopy,
                    contentDescription = "Скопировать",
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
                AdbPlainButton(
                    onClick = onInfo,
                    enabled = !isActionRunning,
                    leadingIcon = Icons.Outlined.Info,
                    contentDescription = "Инфо",
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
                AdbPlainButton(
                    onClick = onUninstall,
                    enabled = !isActionRunning,
                    leadingIcon = Icons.Outlined.Delete,
                    contentDescription = "Удалить",
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(
    label: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PackageEnabledBadge(isEnabled: Boolean) {
    if (isEnabled) {
        MetaBadge(
            label = "ENABLED",
            color = Color(0xFF2E7D32),
        )
    } else {
        MetaBadge(
            label = "DISABLED",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// ── Бейдж типа пакета ─────────────────────────────────────────────────────────

/**
 * Небольшой цветной бейдж USER / SYSTEM.
 */
@Composable
private fun PackageTypeBadge(type: PackageType) {
    when (type) {
        PackageType.USER -> MetaBadge(
            label = "USER",
            color = MaterialTheme.colorScheme.primary,
        )
        PackageType.SYSTEM -> MetaBadge(
            label = "SYSTEM",
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun filterLabel(filter: PackageTypeFilter): String = when (filter) {
    PackageTypeFilter.ALL -> "Все"
    PackageTypeFilter.USER -> "User"
    PackageTypeFilter.SYSTEM -> "System"
}

// ── Строка состояния ──────────────────────────────────────────────────────────

/**
 * Нижняя строка состояния: состояние списка, количество пакетов и активные фильтры.
 */
@Composable
private fun PackagesStatusBar(state: PackagesState) {
    val totalCount = (state.listState as? PackagesListState.Success)?.packages?.size ?: 0
    val shownCount = state.filteredPackages.size

    val (stateLabel, stateColor) = when (state.listState) {
        is PackagesListState.NoDevice -> "Нет устройства" to MaterialTheme.colorScheme.onSurfaceVariant
        is PackagesListState.Loading -> "Загрузка" to Color(0xFFFF9800)
        is PackagesListState.Error -> "Ошибка" to MaterialTheme.colorScheme.error
        is PackagesListState.Success -> "Готово" to Color(0xFF2E7D32)
    }

    val countLabel = if (shownCount == totalCount) {
        "$totalCount пакетов"
    } else {
        "$shownCount из $totalCount"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        StatusPill(text = stateLabel, color = stateColor)
        StatusPill(text = countLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (state.typeFilter != PackageTypeFilter.ALL) {
            StatusPill(
                text = "Фильтр: ${filterLabel(state.typeFilter)}",
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim()
            val preview = if (query.length <= 24) query else "${query.take(24)}…"
            StatusPill(
                text = "Поиск: $preview",
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.isActionRunning) {
            StatusPill(
                text = "Выполняется операция",
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
            AdbFilledButton(
                onClick = component::onConfirmAction,
                text = "Подтвердить",
                type = AdbButtonType.DANGER,
                size = AdbButtonSize.MEDIUM,
            )
        },
        dismissButton = {
            AdbOutlinedButton(
                onClick = component::onCancelAction,
                text = "Отмена",
                size = AdbButtonSize.MEDIUM,
            )
        },
    )
}
