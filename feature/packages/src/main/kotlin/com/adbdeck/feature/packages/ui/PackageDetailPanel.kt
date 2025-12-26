package com.adbdeck.feature.packages.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageDetails
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.packages.PackageDetailState
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.packages.PackagesState
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        DetailPanelHeader(pkg = pkg, onClose = component::onClearSelection)
        HorizontalDivider()

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
        AdbPlainButton(
            onClick = onClose,
            leadingIcon = Icons.Outlined.Close,
            contentDescription = "Закрыть панель",
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )
    }
}

/**
 * Прокручиваемый контент детальной панели.
 *
 * Секции:
 * - управление приложением;
 * - деструктивные действия;
 * - основная информация;
 * - временные метки;
 * - пути;
 * - флаги;
 * - runtime-разрешения.
 */
@Composable
private fun DetailPanelContent(
    pkg: AppPackage,
    details: PackageDetails,
    state: PackagesState,
    component: PackagesComponent,
) {
    val sectionColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val sectionBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
    ) {
        DetailActionsSection(
            pkg = pkg,
            state = state,
            component = component,
            sectionColor = sectionColor,
            sectionBorder = sectionBorder,
        )

        PackageInfoSection(
            details = details,
            sectionColor = sectionColor,
            sectionBorder = sectionBorder,
        )

        if (details.firstInstallTime.isNotBlank() || details.lastUpdateTime.isNotBlank()) {
            PackageTimestampsSection(
                details = details,
                sectionColor = sectionColor,
                sectionBorder = sectionBorder,
            )
        }

        PackagePathsSection(
            details = details,
            sectionColor = sectionColor,
            sectionBorder = sectionBorder,
        )

        AdbSectionCard(
            title = "Флаги",
            titleUppercase = true,
            containerColor = sectionColor,
            border = sectionBorder,
            contentSpacing = Dimensions.paddingSmall,
        ) {
            FlagsSection(details = details)
        }

        if (details.runtimePermissions.isNotEmpty()) {
            PermissionsSection(
                pkg = pkg,
                permissions = details.runtimePermissions,
                isActionRunning = state.isActionRunning,
                onGrant = { perm -> component.onGrantPermission(pkg, perm) },
                onRevoke = { perm -> component.onRevokePermission(pkg, perm) },
                sectionColor = sectionColor,
                sectionBorder = sectionBorder,
            )
        }
    }
}

/**
 * Карточки действий, разделенные по уровню риска.
 */
@Composable
private fun DetailActionsSection(
    pkg: AppPackage,
    state: PackagesState,
    component: PackagesComponent,
    sectionColor: Color,
    sectionBorder: BorderStroke,
) {
    val isRunning = state.isActionRunning
    val clipboard = LocalClipboardManager.current

    AdbSectionCard(
        title = "Управление",
        subtitle = "Быстрые операции с приложением",
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        AdbOutlinedButton(
            onClick = { component.onLaunchApp(pkg) },
            enabled = !isRunning,
            text = "Запустить",
            leadingIcon = Icons.Outlined.PlayArrow,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onForceStop(pkg) },
            enabled = !isRunning,
            text = "Остановить",
            leadingIcon = Icons.Outlined.Stop,
            type = AdbButtonType.DANGER,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onOpenAppInfo(pkg) },
            enabled = !isRunning,
            text = "Открыть App Info",
            leadingIcon = Icons.Outlined.Info,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = {
                val savePath = showSaveApkDialog(pkg.packageName) ?: return@AdbOutlinedButton
                component.onExportApk(pkg, savePath)
            },
            enabled = !isRunning,
            text = "Выгрузить APK",
            leadingIcon = Icons.Outlined.FileDownload,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onTrackInLogcat(pkg) },
            enabled = !isRunning,
            text = "Отследить в Logcat",
            leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = {
                clipboard.setText(AnnotatedString(pkg.packageName))
                component.onCopyPackageName(pkg)
            },
            enabled = !isRunning,
            text = "Скопировать имя пакета",
            leadingIcon = Icons.Outlined.ContentCopy,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
    }

    AdbSectionCard(
        title = "Опасные действия",
        subtitle = "Операции, влияющие на данные приложения",
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.error,
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        contentSpacing = Dimensions.paddingSmall,
    ) {
        AdbOutlinedButton(
            onClick = { component.onRequestClearData(pkg) },
            enabled = !isRunning,
            text = "Очистить данные",
            type = AdbButtonType.DANGER,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onRequestUninstall(pkg) },
            enabled = !isRunning,
            text = "Удалить приложение",
            leadingIcon = Icons.Outlined.Delete,
            type = AdbButtonType.DANGER,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
    }
}

/**
 * Секция основной информации о пакете.
 */
@Composable
private fun PackageInfoSection(
    details: PackageDetails,
    sectionColor: Color,
    sectionBorder: BorderStroke,
) {
    AdbSectionCard(
        title = "Информация",
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
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
}

/**
 * Секция дат установки/обновления.
 */
@Composable
private fun PackageTimestampsSection(
    details: PackageDetails,
    sectionColor: Color,
    sectionBorder: BorderStroke,
) {
    AdbSectionCard(
        title = "Временные метки",
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        if (details.firstInstallTime.isNotBlank()) {
            InfoRow(label = "Установлено", value = details.firstInstallTime)
        }
        if (details.lastUpdateTime.isNotBlank()) {
            InfoRow(label = "Обновлено", value = details.lastUpdateTime)
        }
    }
}

/**
 * Секция путей приложения.
 */
@Composable
private fun PackagePathsSection(
    details: PackageDetails,
    sectionColor: Color,
    sectionBorder: BorderStroke,
) {
    AdbSectionCard(
        title = "Пути",
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
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
}

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

/**
 * Раскрываемая секция со списком runtime-разрешений и кнопками Grant/Revoke.
 */
@Composable
private fun PermissionsSection(
    pkg: AppPackage,
    permissions: Map<String, Boolean>,
    isActionRunning: Boolean,
    onGrant: (String) -> Unit,
    onRevoke: (String) -> Unit,
    sectionColor: Color,
    sectionBorder: BorderStroke,
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedPermissions = permissions.entries.sortedBy { it.key }

    AdbSectionCard(
        title = "Runtime-разрешения",
        subtitle = "Всего разрешений: ${permissions.size}",
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
        headerTrailing = {
            AdbPlainButton(
                onClick = { expanded = !expanded },
                leadingIcon = if (expanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                size = AdbButtonSize.SMALL,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        },
    ) {
        if (!expanded) {
            Text(
                text = "Нажмите +, чтобы показать список разрешений",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AdbSectionCard
        }

        Text(
            text = "Grant/Revoke права:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        sortedPermissions.forEachIndexed { index, (perm, granted) ->
            PermissionRow(
                permission = perm,
                granted = granted,
                isRunning = isActionRunning,
                onGrant = { onGrant(perm) },
                onRevoke = { onRevoke(perm) },
            )

            if (index != sortedPermissions.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

/**
 * Строка одного разрешения с индикатором статуса и кнопкой действия.
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            AdbOutlinedButton(
                onClick = onRevoke,
                enabled = !isRunning,
                text = "Отозвать",
                leadingIcon = Icons.Outlined.Remove,
                type = AdbButtonType.DANGER,
                size = AdbButtonSize.SMALL,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        } else {
            AdbOutlinedButton(
                onClick = onGrant,
                enabled = !isRunning,
                text = "Выдать",
                leadingIcon = Icons.Outlined.Add,
                type = AdbButtonType.SUCCESS,
                size = AdbButtonSize.SMALL,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        }
    }
}

/**
 * Строка "Label: value" для отображения параметров пакета.
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
            modifier = Modifier.width(108.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = if (monospace) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Открывает диалог сохранения APK-файла и возвращает абсолютный путь назначения.
 */
private fun showSaveApkDialog(packageName: String): String? {
    val defaultName = "$packageName.apk"
    val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
        dialogTitle = "Сохранить APK"
        selectedFile = File(defaultName)
        fileFilter = FileNameExtensionFilter("APK (*.apk)", "apk")
        isAcceptAllFileFilterUsed = false
    }

    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null

    var file = chooser.selectedFile
    if (!file.name.endsWith(".apk", ignoreCase = true)) {
        file = File("${file.absolutePath}.apk")
    }
    return file.absolutePath
}
