package com.adbdeck.feature.packages.ui

import adbdeck.feature.packages.generated.resources.Res
import adbdeck.feature.packages.generated.resources.packages_detail_action_clear_data
import adbdeck.feature.packages.generated.resources.packages_detail_action_copy_package_name
import adbdeck.feature.packages.generated.resources.packages_detail_action_export_package
import adbdeck.feature.packages.generated.resources.packages_detail_action_grant
import adbdeck.feature.packages.generated.resources.packages_detail_action_launch
import adbdeck.feature.packages.generated.resources.packages_detail_action_open_app_info
import adbdeck.feature.packages.generated.resources.packages_detail_action_revoke
import adbdeck.feature.packages.generated.resources.packages_detail_action_stop
import adbdeck.feature.packages.generated.resources.packages_detail_action_track_logcat
import adbdeck.feature.packages.generated.resources.packages_detail_action_uninstall
import adbdeck.feature.packages.generated.resources.packages_detail_close_panel
import adbdeck.feature.packages.generated.resources.packages_detail_error_format
import adbdeck.feature.packages.generated.resources.packages_error_detail_load
import adbdeck.feature.packages.generated.resources.packages_detail_flag_debuggable
import adbdeck.feature.packages.generated.resources.packages_detail_flag_disabled
import adbdeck.feature.packages.generated.resources.packages_detail_flag_enabled
import adbdeck.feature.packages.generated.resources.packages_detail_flag_suspended
import adbdeck.feature.packages.generated.resources.packages_detail_flag_system
import adbdeck.feature.packages.generated.resources.packages_detail_flag_user
import adbdeck.feature.packages.generated.resources.packages_detail_info_apk
import adbdeck.feature.packages.generated.resources.packages_detail_info_data
import adbdeck.feature.packages.generated.resources.packages_detail_info_installed
import adbdeck.feature.packages.generated.resources.packages_detail_info_min_sdk
import adbdeck.feature.packages.generated.resources.packages_detail_info_name
import adbdeck.feature.packages.generated.resources.packages_detail_info_native_libs
import adbdeck.feature.packages.generated.resources.packages_detail_info_target_sdk
import adbdeck.feature.packages.generated.resources.packages_detail_info_uid
import adbdeck.feature.packages.generated.resources.packages_detail_info_updated
import adbdeck.feature.packages.generated.resources.packages_detail_info_version
import adbdeck.feature.packages.generated.resources.packages_detail_permissions_collapse
import adbdeck.feature.packages.generated.resources.packages_detail_permissions_expand
import adbdeck.feature.packages.generated.resources.packages_detail_permissions_expand_hint
import adbdeck.feature.packages.generated.resources.packages_detail_permissions_grant_revoke
import adbdeck.feature.packages.generated.resources.packages_detail_permissions_total
import adbdeck.feature.packages.generated.resources.packages_detail_save_dialog_filter_desc
import adbdeck.feature.packages.generated.resources.packages_detail_save_dialog_title
import adbdeck.feature.packages.generated.resources.packages_detail_section_danger
import adbdeck.feature.packages.generated.resources.packages_detail_section_danger_subtitle
import adbdeck.feature.packages.generated.resources.packages_detail_section_flags
import adbdeck.feature.packages.generated.resources.packages_detail_section_info
import adbdeck.feature.packages.generated.resources.packages_detail_section_management
import adbdeck.feature.packages.generated.resources.packages_detail_section_management_subtitle
import adbdeck.feature.packages.generated.resources.packages_detail_section_paths
import adbdeck.feature.packages.generated.resources.packages_detail_section_permissions
import adbdeck.feature.packages.generated.resources.packages_detail_section_timestamps
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
import com.adbdeck.core.designsystem.AdbTheme
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
import org.jetbrains.compose.resources.stringResource

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
        modifier = modifier.background(AdbTheme.colorScheme.surface),
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
                        text = stringResource(
                            Res.string.packages_detail_error_format,
                            detail.message.ifBlank {
                                stringResource(Res.string.packages_error_detail_load)
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AdbTheme.colorScheme.error,
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
                color = AdbTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AdbPlainButton(
            onClick = onClose,
            leadingIcon = Icons.Outlined.Close,
            contentDescription = stringResource(Res.string.packages_detail_close_panel),
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
    val sectionColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val sectionBorder = BorderStroke(1.dp, AdbTheme.colorScheme.outline.copy(alpha = 0.32f))

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
            title = stringResource(Res.string.packages_detail_section_flags),
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
    val saveDialogTitle = stringResource(Res.string.packages_detail_save_dialog_title)
    val saveDialogFilterDescription = stringResource(Res.string.packages_detail_save_dialog_filter_desc)

    AdbSectionCard(
        title = stringResource(Res.string.packages_detail_section_management),
        subtitle = stringResource(Res.string.packages_detail_section_management_subtitle),
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        AdbOutlinedButton(
            onClick = { component.onLaunchApp(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_launch),
            leadingIcon = Icons.Outlined.PlayArrow,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onForceStop(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_stop),
            leadingIcon = Icons.Outlined.Stop,
            type = AdbButtonType.DANGER,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onOpenAppInfo(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_open_app_info),
            leadingIcon = Icons.Outlined.Info,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = {
                val savePath = showSaveApkDialog(
                    packageName = pkg.packageName,
                    dialogTitle = saveDialogTitle,
                    filterDescription = saveDialogFilterDescription,
                ) ?: return@AdbOutlinedButton
                component.onExportApk(pkg, savePath)
            },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_export_package),
            leadingIcon = Icons.Outlined.FileDownload,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onTrackInLogcat(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_track_logcat),
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
            text = stringResource(Res.string.packages_detail_action_copy_package_name),
            leadingIcon = Icons.Outlined.ContentCopy,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
    }

    AdbSectionCard(
        title = stringResource(Res.string.packages_detail_section_danger),
        subtitle = stringResource(Res.string.packages_detail_section_danger_subtitle),
        titleUppercase = true,
        titleColor = AdbTheme.colorScheme.error,
        containerColor = AdbTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, AdbTheme.colorScheme.error.copy(alpha = 0.35f)),
        contentSpacing = Dimensions.paddingSmall,
    ) {
        AdbOutlinedButton(
            onClick = { component.onRequestClearData(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_clear_data),
            type = AdbButtonType.DANGER,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            fullWidth = true,
        )
        AdbOutlinedButton(
            onClick = { component.onRequestUninstall(pkg) },
            enabled = !isRunning,
            text = stringResource(Res.string.packages_detail_action_uninstall),
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
        title = stringResource(Res.string.packages_detail_section_info),
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        if (details.appLabel.isNotBlank()) {
            InfoRow(label = stringResource(Res.string.packages_detail_info_name), value = details.appLabel)
        }
        if (details.versionName.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_version),
                value = "${details.versionName} (${details.versionCode})",
            )
        }
        if (details.uid != 0) {
            InfoRow(label = stringResource(Res.string.packages_detail_info_uid), value = details.uid.toString())
        }
        if (details.targetSdk != 0) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_target_sdk),
                value = details.targetSdk.toString(),
            )
        }
        if (details.minSdk != 0) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_min_sdk),
                value = details.minSdk.toString(),
            )
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
        title = stringResource(Res.string.packages_detail_section_timestamps),
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        if (details.firstInstallTime.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_installed),
                value = details.firstInstallTime,
            )
        }
        if (details.lastUpdateTime.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_updated),
                value = details.lastUpdateTime,
            )
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
        title = stringResource(Res.string.packages_detail_section_paths),
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
    ) {
        if (details.codePath.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_apk),
                value = details.codePath,
                monospace = true,
            )
        }
        if (details.dataDir.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_data),
                value = details.dataDir,
                monospace = true,
            )
        }
        if (details.nativeLibPath.isNotBlank()) {
            InfoRow(
                label = stringResource(Res.string.packages_detail_info_native_libs),
                value = details.nativeLibPath,
                monospace = true,
            )
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
            label = if (details.isSystem) {
                stringResource(Res.string.packages_detail_flag_system)
            } else {
                stringResource(Res.string.packages_detail_flag_user)
            },
            color = if (details.isSystem) AdbTheme.colorScheme.tertiary
            else AdbTheme.colorScheme.primary,
        )
        FlagChip(
            label = if (details.isEnabled) {
                stringResource(Res.string.packages_detail_flag_enabled)
            } else {
                stringResource(Res.string.packages_detail_flag_disabled)
            },
            color = if (details.isEnabled) AdbTheme.semanticColors.success else AdbTheme.colorScheme.error,
        )
        if (details.isDebuggable) {
            FlagChip(
                label = stringResource(Res.string.packages_detail_flag_debuggable),
                color = AdbTheme.semanticColors.warning,
            )
        }
        if (details.isSuspended) {
            FlagChip(label = stringResource(Res.string.packages_detail_flag_suspended), color = AdbTheme.colorScheme.error)
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
        title = stringResource(Res.string.packages_detail_section_permissions),
        subtitle = stringResource(Res.string.packages_detail_permissions_total, permissions.size),
        titleUppercase = true,
        containerColor = sectionColor,
        border = sectionBorder,
        contentSpacing = Dimensions.paddingSmall,
        headerTrailing = {
            AdbPlainButton(
                onClick = { expanded = !expanded },
                leadingIcon = if (expanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                contentDescription = if (expanded) {
                    stringResource(Res.string.packages_detail_permissions_collapse)
                } else {
                    stringResource(Res.string.packages_detail_permissions_expand)
                },
                size = AdbButtonSize.SMALL,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        },
    ) {
        if (!expanded) {
            Text(
                text = stringResource(Res.string.packages_detail_permissions_expand_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
            return@AdbSectionCard
        }

        Text(
            text = stringResource(Res.string.packages_detail_permissions_grant_revoke),
            style = MaterialTheme.typography.labelMedium,
            color = AdbTheme.colorScheme.onSurfaceVariant,
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
                HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                color = if (granted) AdbTheme.semanticColors.success else AdbTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = permission,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = AdbTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (granted) {
            AdbOutlinedButton(
                onClick = onRevoke,
                enabled = !isRunning,
                text = stringResource(Res.string.packages_detail_action_revoke),
                leadingIcon = Icons.Outlined.Remove,
                type = AdbButtonType.DANGER,
                size = AdbButtonSize.SMALL,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        } else {
            AdbOutlinedButton(
                onClick = onGrant,
                enabled = !isRunning,
                text = stringResource(Res.string.packages_detail_action_grant),
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
            color = AdbTheme.colorScheme.onSurfaceVariant,
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
            color = AdbTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = if (monospace) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun showSaveApkDialog(
    packageName: String,
    dialogTitle: String,
    filterDescription: String,
): String? {
    val defaultName = "$packageName.apk"
    val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
        this.dialogTitle = dialogTitle
        selectedFile = File(defaultName)
        fileFilter = FileNameExtensionFilter(filterDescription, "apk", "apks")
        isAcceptAllFileFilterUsed = false
    }

    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null

    var file = chooser.selectedFile
    val hasKnownExtension = file.name.endsWith(".apk", ignoreCase = true) ||
        file.name.endsWith(".apks", ignoreCase = true)
    if (!hasKnownExtension) {
        file = File("${file.absolutePath}.apk")
    }
    return file.absolutePath
}
