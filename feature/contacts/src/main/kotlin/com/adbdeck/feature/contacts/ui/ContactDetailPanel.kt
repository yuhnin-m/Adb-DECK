package com.adbdeck.feature.contacts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.models.ContactDetailState

/**
 * Правая панель с детальной информацией о контакте.
 */
@Composable
fun ContactDetailPanel(
    contact: Contact,
    state: ContactDetailState,
    component: ContactsComponent,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                contact = contact,
                onClose = { component.onCloseDetail() },
            )
            HorizontalDivider()

            when (state) {
                is ContactDetailState.Idle -> Unit
                is ContactDetailState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ContactDetailState.Error -> ErrorView(
                    message = state.message,
                    onRetry = { component.onRefreshDetail() },
                    modifier = Modifier.fillMaxSize(),
                )
                is ContactDetailState.Success -> DetailContent(
                    details = state.details,
                    contact = contact,
                    component = component,
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    contact: Contact,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = contact.displayName.ifEmpty { "Контакт #${contact.id}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (contact.accountType.isNotEmpty()) {
                Text(
                    text = contact.accountType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AdbPlainButton(
            onClick = onClose,
            leadingIcon = Icons.Filled.Close,
            contentDescription = "Закрыть",
            size = AdbButtonSize.SMALL,
        )
    }
}
