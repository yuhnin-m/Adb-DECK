package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    AdbSectionCard(
        title = title,
        titleUppercase = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (monospace) FontFamily.Monospace else null,
        )
    }
}

@Composable
internal fun PhoneTypeBadge(type: PhoneType) {
    val label = when (type) {
        PhoneType.HOME -> stringResource(Res.string.contacts_phone_type_home)
        PhoneType.MOBILE -> stringResource(Res.string.contacts_phone_type_mobile)
        PhoneType.WORK -> stringResource(Res.string.contacts_phone_type_work)
        PhoneType.OTHER -> stringResource(Res.string.contacts_phone_type_other)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun EmailTypeBadge(type: EmailType) {
    val label = when (type) {
        EmailType.HOME -> stringResource(Res.string.contacts_email_type_home)
        EmailType.WORK -> stringResource(Res.string.contacts_email_type_work)
        EmailType.OTHER -> stringResource(Res.string.contacts_email_type_other)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
