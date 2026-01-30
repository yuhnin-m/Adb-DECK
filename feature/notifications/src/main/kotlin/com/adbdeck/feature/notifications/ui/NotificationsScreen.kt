package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_empty_none
import adbdeck.feature.notifications.generated.resources.notifications_empty_not_found
import adbdeck.feature.notifications.generated.resources.notifications_empty_select_device
import adbdeck.feature.notifications.generated.resources.notifications_loading
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.notifications.NotificationsComponent
import com.adbdeck.feature.notifications.NotificationsListState
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой экран просмотра уведомлений Android через ADB.
 */
@Composable
fun NotificationsScreen(component: NotificationsComponent) {
    val state by component.state.collectAsState()
    var isComposerPanelOpen by remember { mutableStateOf(false) }

    val savedKeys = remember(state.savedNotifications) {
        state.savedNotifications.asSequence().map { it.record.key }.toHashSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsToolbar(
                state = state,
                onRefresh = component::onRefresh,
                isComposerOpen = isComposerPanelOpen,
                onToggleComposer = { isComposerPanelOpen = !isComposerPanelOpen },
                onSearchChanged = component::onSearchChanged,
                onClearSearch = { component.onSearchChanged("") },
                onPackageFilterChanged = component::onPackageFilterChanged,
                onClearPackageFilter = { component.onPackageFilterChanged("") },
                onFilterChanged = component::onFilterChanged,
                onSortOrderChanged = component::onSortOrderChanged,
            )

            HorizontalDivider()

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val listState = state.listState) {
                        NotificationsListState.NoDevice -> {
                            EmptyView(message = stringResource(Res.string.notifications_empty_select_device))
                        }

                        NotificationsListState.Loading -> {
                            LoadingView(message = stringResource(Res.string.notifications_loading))
                        }

                        is NotificationsListState.Error -> {
                            ErrorView(
                                message = listState.message,
                                onRetry = component::onRefresh,
                            )
                        }

                        is NotificationsListState.Success -> {
                            if (state.displayedNotifications.isEmpty()) {
                                val emptyMessageRes = if (
                                    state.searchQuery.isNotBlank() || state.packageFilter.isNotBlank()
                                ) {
                                    Res.string.notifications_empty_not_found
                                } else {
                                    Res.string.notifications_empty_none
                                }
                                EmptyView(message = stringResource(emptyMessageRes))
                            } else {
                                NotificationsList(
                                    notifications = state.displayedNotifications,
                                    selectedKey = state.selectedKey,
                                    savedKeys = savedKeys,
                                    onSelect = component::onSelectNotification,
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isComposerPanelOpen && state.selectedRecord != null,
                    enter = slideInHorizontally { width -> width },
                    exit = slideOutHorizontally { width -> width },
                ) {
                    val record = state.selectedRecord
                    if (record != null) {
                        Row {
                            VerticalDivider()
                            NotificationDetailPanel(
                                modifier = Modifier.width(Dimensions.sidebarWidth * 2).fillMaxHeight(),
                                record = record,
                                selectedTab = state.selectedTab,
                                savedNotifications = state.savedNotifications,
                                isSaved = record.key in savedKeys,
                                component = component,
                            )
                        }
                    }
                }

                if (isComposerPanelOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(Dimensions.paddingXSmall / 4)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    )

                    NotificationsComposerPanel(
                        modifier = Modifier
                            .width(Dimensions.sidebarWidth * 2)
                            .fillMaxHeight(),
                        state = state,
                        onDismissRequest = { isComposerPanelOpen = false },
                        onSend = component::onPostNotification,
                    )
                }
            }

            HorizontalDivider()
            NotificationsStatusBar(state = state)
        }

        state.feedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier
                    .padding(horizontal = Dimensions.paddingDefault)
                    .padding(bottom = Dimensions.statusBarHeight)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}
