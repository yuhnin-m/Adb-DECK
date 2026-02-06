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
import adbdeck.feature.notifications.generated.resources.notifications_toolbar_search_placeholder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.menubuttons.AdbMenuButtonOption
import com.adbdeck.core.ui.menubuttons.AdbOutlinedMenuButton
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
                AdbFilledButton(
                    onClick = onRefresh,
                    text = stringResource(AdbCommonStringRes.actionRefresh),
                    loading = state.isRefreshing,
                    enabled = state.activeDeviceId != null,
                    leadingIcon = Icons.Outlined.Refresh,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )

                AdbOutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.width(TOOLBAR_SEARCH_FIELD_WIDTH),
                    placeholder = stringResource(Res.string.notifications_toolbar_search_placeholder),
                    size = AdbTextFieldSize.MEDIUM,
                    trailingIcon = if (state.searchQuery.isNotEmpty()) Icons.Outlined.Clear else null,
                    onTrailingIconClick = if (state.searchQuery.isNotEmpty()) onClearSearch else null,
                )

                Spacer(modifier = Modifier.weight(1f))

                AdbFilledButton(
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.paddingSmall)
                    .padding(bottom = Dimensions.paddingXSmall),
            ) {
                AdbSingleSegmentedButtons(
                    options = filterOptions,
                    selectedValue = state.filter,
                    onValueSelected = onFilterChanged,
                    modifier = Modifier.align(Alignment.Center),
                    size = AdbSegmentedButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    NotificationsSortMenu(
                        selectedSortOrder = state.sortOrder,
                        onSortOrderChanged = onSortOrderChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsSortMenu(
    selectedSortOrder: NotificationsSortOrder,
    onSortOrderChanged: (NotificationsSortOrder) -> Unit,
) {
    val selectedLabel = sortOrderLabel(selectedSortOrder)
    val newestLabel = sortOrderLabel(NotificationsSortOrder.NEWEST_FIRST)
    val oldestLabel = sortOrderLabel(NotificationsSortOrder.OLDEST_FIRST)
    val byPackageLabel = sortOrderLabel(NotificationsSortOrder.BY_PACKAGE)
    val sortOptions = remember(newestLabel, oldestLabel, byPackageLabel) {
        NotificationsSortOrder.entries.map { order ->
            AdbMenuButtonOption(
                value = order,
                label = when (order) {
                    NotificationsSortOrder.NEWEST_FIRST -> newestLabel
                    NotificationsSortOrder.OLDEST_FIRST -> oldestLabel
                    NotificationsSortOrder.BY_PACKAGE -> byPackageLabel
                },
            )
        }
    }

    AdbOutlinedMenuButton(
        text = selectedLabel,
        leadingIcon = Icons.AutoMirrored.Outlined.Sort,
        size = AdbButtonSize.MEDIUM,
        cornerRadius = AdbCornerRadius.MEDIUM,
        options = sortOptions,
        selectedOption = selectedSortOrder,
        showSelectedCheckmark = true,
        onOptionSelected = onSortOrderChanged,
    )
}

@Composable
private fun sortOrderLabel(order: NotificationsSortOrder): String = when (order) {
    NotificationsSortOrder.NEWEST_FIRST -> stringResource(Res.string.notifications_sort_newest)
    NotificationsSortOrder.OLDEST_FIRST -> stringResource(Res.string.notifications_sort_oldest)
    NotificationsSortOrder.BY_PACKAGE -> stringResource(Res.string.notifications_sort_package)
}

private val TOOLBAR_SEARCH_FIELD_WIDTH = Dimensions.sidebarWidth + Dimensions.paddingXLarge
