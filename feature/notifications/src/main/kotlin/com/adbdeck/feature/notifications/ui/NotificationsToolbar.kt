package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_filter_all
import adbdeck.feature.notifications.generated.resources.notifications_filter_current
import adbdeck.feature.notifications.generated.resources.notifications_filter_history
import adbdeck.feature.notifications.generated.resources.notifications_filter_saved
import adbdeck.feature.notifications.generated.resources.notifications_sort_newest
import adbdeck.feature.notifications.generated.resources.notifications_sort_oldest
import adbdeck.feature.notifications.generated.resources.notifications_sort_package
import adbdeck.feature.notifications.generated.resources.notifications_toolbar_close_composer
import adbdeck.feature.notifications.generated.resources.notifications_toolbar_compose
import adbdeck.feature.notifications.generated.resources.notifications_toolbar_package_placeholder
import adbdeck.feature.notifications.generated.resources.notifications_toolbar_search_placeholder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.notifications.NotificationsFilter
import com.adbdeck.feature.notifications.NotificationsSortOrder
import com.adbdeck.feature.notifications.NotificationsState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationsToolbar(
    state: NotificationsState,
    onRefresh: () -> Unit,
    isComposerOpen: Boolean,
    onToggleComposer: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onPackageFilterChanged: (String) -> Unit,
    onClearPackageFilter: () -> Unit,
    onFilterChanged: (NotificationsFilter) -> Unit,
    onSortOrderChanged: (NotificationsSortOrder) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimensions.paddingXSmall / 4,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingSmall,
                        vertical = Dimensions.paddingXSmall,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                AdbOutlinedButton(
                    onClick = onRefresh,
                    loading = state.isRefreshing,
                    enabled = state.activeDeviceId != null,
                    leadingIcon = Icons.Outlined.Refresh,
                    contentDescription = stringResource(AdbCommonStringRes.actionRefresh),
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )

                AdbOutlinedButton(
                    onClick = onToggleComposer,
                    text = if (isComposerOpen) {
                        stringResource(Res.string.notifications_toolbar_close_composer)
                    } else {
                        stringResource(Res.string.notifications_toolbar_compose)
                    },
                    leadingIcon = Icons.Outlined.Notifications,
                    enabled = state.activeDeviceId != null,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )

                AdbOutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.notifications_toolbar_search_placeholder),
                    size = AdbTextFieldSize.MEDIUM,
                    trailingIcon = if (state.searchQuery.isNotEmpty()) Icons.Outlined.Clear else null,
                    onTrailingIconClick = if (state.searchQuery.isNotEmpty()) onClearSearch else null,
                )

                AdbOutlinedTextField(
                    value = state.packageFilter,
                    onValueChange = onPackageFilterChanged,
                    modifier = Modifier.width(Dimensions.sidebarWidth),
                    placeholder = stringResource(Res.string.notifications_toolbar_package_placeholder),
                    size = AdbTextFieldSize.MEDIUM,
                    trailingIcon = if (state.packageFilter.isNotEmpty()) Icons.Outlined.Clear else null,
                    onTrailingIconClick = if (state.packageFilter.isNotEmpty()) onClearPackageFilter else null,
                )

                NotificationsSortMenu(
                    selectedSortOrder = state.sortOrder,
                    onSortOrderChanged = onSortOrderChanged,
                )
            }

            val allFilterLabel = stringResource(Res.string.notifications_filter_all)
            val currentFilterLabel = stringResource(Res.string.notifications_filter_current)
            val historyFilterLabel = stringResource(Res.string.notifications_filter_history)
            val savedFilterLabel = stringResource(Res.string.notifications_filter_saved)
            val filterOptions = remember(
                allFilterLabel,
                currentFilterLabel,
                historyFilterLabel,
                savedFilterLabel,
            ) {
                listOf(
                    AdbSegmentedOption(
                        value = NotificationsFilter.ALL,
                        label = allFilterLabel,
                    ),
                    AdbSegmentedOption(
                        value = NotificationsFilter.CURRENT,
                        label = currentFilterLabel,
                    ),
                    AdbSegmentedOption(
                        value = NotificationsFilter.HISTORICAL,
                        label = historyFilterLabel,
                    ),
                    AdbSegmentedOption(
                        value = NotificationsFilter.SAVED,
                        label = savedFilterLabel,
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.paddingSmall)
                    .padding(bottom = Dimensions.paddingXSmall),
            ) {
                AdbSingleSegmentedButtons(
                    options = filterOptions,
                    selectedValue = state.filter,
                    onValueSelected = onFilterChanged,
                    size = AdbSegmentedButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
            }
        }
    }
}

@Composable
private fun NotificationsSortMenu(
    selectedSortOrder: NotificationsSortOrder,
    onSortOrderChanged: (NotificationsSortOrder) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    val selectedLabel = sortOrderLabel(selectedSortOrder)

    Box {
        AdbOutlinedButton(
            onClick = { sortMenuOpen = true },
            text = selectedLabel,
            leadingIcon = Icons.AutoMirrored.Outlined.Sort,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )

        DropdownMenu(
            expanded = sortMenuOpen,
            onDismissRequest = { sortMenuOpen = false },
        ) {
            NotificationsSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(sortOrderLabel(order)) },
                    onClick = {
                        onSortOrderChanged(order)
                        sortMenuOpen = false
                    },
                    leadingIcon = if (selectedSortOrder == order) {
                        {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun sortOrderLabel(order: NotificationsSortOrder): String = when (order) {
    NotificationsSortOrder.NEWEST_FIRST -> stringResource(Res.string.notifications_sort_newest)
    NotificationsSortOrder.OLDEST_FIRST -> stringResource(Res.string.notifications_sort_oldest)
    NotificationsSortOrder.BY_PACKAGE -> stringResource(Res.string.notifications_sort_package)
}
