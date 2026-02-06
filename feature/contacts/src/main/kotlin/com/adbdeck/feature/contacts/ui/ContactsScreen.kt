package com.adbdeck.feature.contacts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.models.ContactsListState
import com.adbdeck.feature.contacts.models.ContactsState
import com.adbdeck.feature.contacts.models.toMainContentState
import com.adbdeck.feature.contacts.models.toOverlayState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Главный экран управления контактами Android-устройства.
 */
@Composable
fun ContactsScreen(component: ContactsComponent) {
    Box(modifier = Modifier.fillMaxSize()) {
        ContactsMainContent(component = component)
        ContactsOverlay(component = component)
    }

    ContactsDialogs(component = component)
}

@Composable
private fun ContactsMainContent(component: ContactsComponent) {
    val flow = remember(component) {
        component.state
            .map(ContactsState::toMainContentState)
            .distinctUntilChanged()
    }
    val initial = remember(component) { component.state.value.toMainContentState() }
    val state by flow.collectAsState(initial = initial)
    val selectedContact = remember(state.listState, state.selectedContactId) {
        (state.listState as? ContactsListState.Success)
            ?.contacts
            ?.firstOrNull { it.id == state.selectedContactId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.showSafetyBanner) {
            AdbBanner(
                message = "ADB не предназначен для управления пользовательскими данными. " +
                    "Но для отладки иногда очень удобно. Надеемся, вы знаете, что делаете.",
                type = AdbBannerType.WARNING,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                dismissText = "ОК",
                onDismiss = { component.onDismissSafetyBanner() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        ContactsToolbar(
            listState = state.listState,
            searchQuery = state.searchQuery,
            accounts = state.availableAccounts,
            selectedAccount = state.selectedAccount,
            hasSelectedContact = selectedContact != null,
            selectedContact = selectedContact,
            component = component,
        )
        HorizontalDivider()

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ContactsListPane(
                listState = state.listState,
                filteredContacts = state.filteredContacts,
                searchQuery = state.searchQuery,
                selectedContactId = state.selectedContactId,
                onRefresh = component::onRefresh,
                onSelectContact = component::onSelectContact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            AnimatedVisibility(
                visible = selectedContact != null,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                val contact = selectedContact ?: return@AnimatedVisibility
                Row {
                    VerticalDivider()
                    ContactDetailPanel(
                        contact = contact,
                        state = state.detailState,
                        component = component,
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        HorizontalDivider()

        ContactsStatusBar(listState = state.listState)
    }
}

@Composable
private fun BoxScope.ContactsOverlay(component: ContactsComponent) {
    val flow = remember(component) {
        component.state
            .map(ContactsState::toOverlayState)
            .distinctUntilChanged()
    }
    val initial = remember(component) { component.state.value.toOverlayState() }
    val state by flow.collectAsState(initial = initial)

    state.actionFeedback?.let { feedback ->
        AdbBanner(
            message = feedback.message,
            type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
            onDismiss = { component.onDismissFeedback() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    state.operationState?.let { operation ->
        OperationProgressDialog(
            operation = operation,
            onCancel = { component.onCancelOperation() },
        )
    }
}
