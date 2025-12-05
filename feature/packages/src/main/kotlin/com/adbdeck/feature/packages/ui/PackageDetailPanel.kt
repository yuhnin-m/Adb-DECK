package com.adbdeck.feature.packages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageDetails
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.packages.PackageDetailState
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.packages.PackagesState

/**
 * Панель детальной информации о выбранном пакете.
 *
 * Показывается справа от списка пакетов в мастер-деталь раскладке.
 * Отображает три возможных состояния: загрузку, ошибку и успешно загруженные данные.
 *
 * @param state     Текущее состояние экрана (для selectedPackage и detailState).
 * @param component Компонент для вызова действий над пакетом.
 * @param modifier  Modifier для управления размером и положением панели.
 */
@Composable
fun PackageDetailPanel(
    state: PackagesState,
    component: PackagesComponent,
    modifier: Modifier = Modifier,
) {
    val pkg = state.selectedPackage ?: return

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Заголовок панели ──────────────────────────────────────
        DetailPanelHeader(pkg = pkg, onClose = component::onClearSelection)
        HorizontalDivider()

        // ── Контент в зависимости от состояния ───────────────────
        when (val detail = state.detailState) {
            is PackageDetailState.Idle -> Unit
            is PackageDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            is PackageDetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⚠  ${detail.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimensions.paddingDefault),
                    )
                }
            }
            is PackageDetailState.Success -> {
                DetailPanelContent(
                    pkg = pkg,
                    details = detail.details,
                    state = state,
                    component = component,
                )
            }
        }
    }
}

// ── Заголовок панели ──────────────────────────────────────────────────────────

/**
 * Заголовок детальной панели с именем пакета и кнопкой закрытия.
 */
@Composable
private fun DetailPanelHeader(
    pkg: AppPackage,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.topBarHeight)
            .padding(start = Dimensions.paddingDefault, end = Dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pkg.packageName.substringAfterLast('.').ifBlank { pkg.packageName },
                style = MaterialTheme.typography.titleSmall,
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
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Закрыть панель",
                modifier = Modifier.size(Dimensions.iconSizeNav),
            )
        }
    }
}

// ── Основной контент панели ───────────────────────────────────────────────────

/**
 * Прокручиваемый контент детальной панели.
 *
 * Секции:
 * 1. Быстрые действия (Launch / Stop / Clear / Uninstall / App Info)
 * 2. Основная информация (версия, UID, SDK)
 * 3. Пути (APK, данные, нативные либы)
 * 4. Временные метки
 * 5. Флаги (Debuggable, System, Enabled, Suspended)
 * 6. Runtime-разрешения
 */
@Composable
private fun DetailPanelContent(
    pkg: AppPackage,
    details: PackageDetails,
    state: PackagesState,
    component: PackagesComponent,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
    ) {
        // ── Быстрые действия ──────────────────────────────────────
        DetailActionsSection(pkg = pkg, state = state, component = component)

        HorizontalDivider()

        // ── Основная информация ───────────────────────────────────
        DetailSection(title = "Информация") {
            if (details.appLabel.isNotBlank()) {
                InfoRow(label = "Название", value = details.appLabel)
            }
            if (details.versionName.isNotBlank()) {
                InfoRow(label = "Версия", value = "${details.versionName} (${details.versionCode})")
            }
            if (details.uid != 0) {
                InfoRow(label = "UID", value = details.uid.toString())
            }
            if (details.targetSdk != 0) {
                InfoRow(label = "Target SDK", value = details.targetSdk.toString())
            }
            if (details.minSdk != 0) {
                InfoRow(label = "Min SDK", value = details.minSdk.toString())
            }
        }

        // ── Временные метки ───────────────────────────────────────
        if (details.firstInstallTime.isNotBlank() || details.lastUpdateTime.isNotBlank()) {
            DetailSection(title = "Временные метки") {
                if (details.firstInstallTime.isNotBlank()) {
                    InfoRow(label = "Установлено", value = details.firstInstallTime)
                }
                if (details.lastUpdateTime.isNotBlank()) {
                    InfoRow(label = "Обновлено", value = details.lastUpdateTime)
                }
            }
        }

        // ── Пути ─────────────────────────────────────────────────
        DetailSection(title = "Пути") {
            if (details.codePath.isNotBlank()) {
                InfoRow(label = "APK", value = details.codePath, monospace = true)
            }
            if (details.dataDir.isNotBlank()) {
                InfoRow(label = "Данные", value = details.dataDir, monospace = true)
            }
            if (details.nativeLibPath.isNotBlank()) {
                InfoRow(label = "Нативные либы", value = details.nativeLibPath, monospace = true)
            }
        }

        // ── Флаги ─────────────────────────────────────────────────
        DetailSection(title = "Флаги") {
            FlagsSection(details = details)
        }

        // ── Runtime-разрешения ────────────────────────────────────
        if (details.runtimePermissions.isNotEmpty()) {
            HorizontalDivider()
            PermissionsSection(
                pkg = pkg,
                permissions = details.runtimePermissions,
                isActionRunning = state.isActionRunning,
                onGrant = { perm -> component.onGrantPermission(pkg, perm) },
                onRevoke = { perm -> component.onRevokePermission(pkg, perm) },
            )
        }
    }
}

// ── Секция быстрых действий ───────────────────────────────────────────────────

/**
 * Горизонтальный ряд кнопок для быстрых действий с пакетом.
 */
@Composable
private fun DetailActionsSection(
    pkg: AppPackage,
    state: PackagesState,
    component: PackagesComponent,
) {
    val isRunning = state.isActionRunning

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        OutlinedButton(
            onClick = { component.onLaunchApp(pkg) },
            enabled = !isRunning,
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Запустить", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = { component.onForceStop(pkg) },
            enabled = !isRunning,
        ) {
            Icon(
                Icons.Outlined.Stop,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Стоп",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedButton(
            onClick = { component.onOpenAppInfo(pkg) },
            enabled = !isRunning,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Инфо", style = MaterialTheme.typography.labelMedium)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        OutlinedButton(
            onClick = { component.onRequestClearData(pkg) },
            enabled = !isRunning,
        ) {
            Text("Очистить данные", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = { component.onRequestUninstall(pkg) },
            enabled = !isRunning,
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Удалить",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Флаги ─────────────────────────────────────────────────────────────────────

/**
 * Отображает флаги пакета в виде цветных чипов.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagsSection(details: PackageDetails) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        FlagChip(
            label = if (details.isSystem) "System" else "User",
            color = if (details.isSystem) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.primary,
        )
        FlagChip(
            label = if (details.isEnabled) "Enabled" else "Disabled",
            color = if (details.isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
        )
        if (details.isDebuggable) {
            FlagChip(label = "Debuggable", color = Color(0xFFFF9800))
        }
        if (details.isSuspended) {
            FlagChip(label = "Suspended", color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Один чип флага с заданным цветом.
 */
@Composable
private fun FlagChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
        )
    }
}

// ── Runtime-разрешения ────────────────────────────────────────────────────────

/**
 * Раскрываемая секция со списком runtime-разрешений и кнопками Grant/Revoke.
 *
 * По умолчанию скрыта — разворачивается по нажатию.
 */
@Composable
private fun PermissionsSection(
    pkg: AppPackage,
    permissions: Map<String, Boolean>,
    isActionRunning: Boolean,
    onGrant: (String) -> Unit,
    onRevoke: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Runtime-разрешения (${permissions.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(Dimensions.paddingSmall))
            permissions.entries.sortedBy { it.key }.forEach { (perm, granted) ->
                PermissionRow(
                    permission = perm,
                    granted = granted,
                    isRunning = isActionRunning,
                    onGrant = { onGrant(perm) },
                    onRevoke = { onRevoke(perm) },
                )
            }
        }
    }
}

/**
 * Строка одного разрешения с индикатором статуса и кнопками.
 */
@Composable
private fun PermissionRow(
    permission: String,
    granted: Boolean,
    isRunning: Boolean,
    onGrant: () -> Unit,
    onRevoke: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Короткое имя разрешения (последний сегмент)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.substringAfterLast('.'),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = permission,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (granted) {
            IconButton(
                onClick = onRevoke,
                enabled = !isRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Отозвать",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            IconButton(
                onClick = onGrant,
                enabled = !isRunning,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Выдать",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF4CAF50),
                )
            }
        }
    }
}

// ── Вспомогательные composable ────────────────────────────────────────────────

/**
 * Обёртка секции с заголовком.
 */
@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

/**
 * Строка «Label: value» для отображения пары информация/значение.
 *
 * @param label     Название параметра.
 * @param value     Значение параметра.
 * @param monospace Если `true`, значение отображается моноширинным шрифтом.
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            style = if (monospace) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            else MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
        )
    }
}
