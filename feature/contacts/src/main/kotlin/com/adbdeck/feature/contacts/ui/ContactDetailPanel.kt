package com.adbdeck.feature.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.Contact
import com.adbdeck.core.adb.api.ContactDetails
import com.adbdeck.core.adb.api.EmailType
import com.adbdeck.core.adb.api.PhoneType
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.contacts.ContactDetailState
import com.adbdeck.feature.contacts.ContactsComponent
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Правая панель с детальной информацией о контакте.
 *
 * Отображает все поля [ContactDetails]: имя, телефоны, email, организацию,
 * заметки, адреса и технические данные (id, raw contacts).
 *
 * @param contact   Краткая информация о контакте (для заголовка).
 * @param state     Текущее состояние загрузки деталей.
 * @param component Компонент для делегирования событий.
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
            // ── Заголовок ─────────────────────────────────────────────────
            DetailHeader(
                contact = contact,
                onClose = { component.onCloseDetail() },
            )
            HorizontalDivider()

            // ── Содержимое ────────────────────────────────────────────────
            when (state) {
                is ContactDetailState.Idle    -> Unit
                is ContactDetailState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ContactDetailState.Error   -> ErrorView(
                    message  = state.message,
                    onRetry  = { component.onRefreshDetail() },
                    modifier = Modifier.fillMaxSize(),
                )
                is ContactDetailState.Success -> DetailContent(
                    details   = state.details,
                    contact   = contact,
                    component = component,
                )
            }
        }
    }
}

// ── Заголовок ────────────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(
    contact: Contact,
    onClose: () -> Unit,
) {
    Row(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Filled.Person,
            contentDescription = null,
            modifier           = Modifier.size(32.dp),
            tint               = MaterialTheme.colorScheme.primary,
        )

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text  = contact.displayName.ifEmpty { "Контакт #${contact.id}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (contact.accountType.isNotEmpty()) {
                Text(
                    text  = contact.accountType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Закрыть")
        }
    }
}

// ── Содержимое ───────────────────────────────────────────────────────────────

@Composable
private fun DetailContent(
    details: ContactDetails,
    contact: Contact,
    component: ContactsComponent,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Имя
        val hasNameFields = details.firstName.isNotEmpty() ||
            details.lastName.isNotEmpty() ||
            details.middleName.isNotEmpty()
        if (hasNameFields) {
            DetailSection(title = "Имя") {
                if (details.firstName.isNotEmpty())
                    DetailRow("Имя", details.firstName)
                if (details.lastName.isNotEmpty())
                    DetailRow("Фамилия", details.lastName)
                if (details.middleName.isNotEmpty())
                    DetailRow("Отчество", details.middleName)
            }
        }

        // Телефоны
        if (details.phones.isNotEmpty()) {
            DetailSection(title = "Телефоны") {
                for (phone in details.phones) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text     = phone.value,
                            style    = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 6.dp).weight(1f),
                        )
                        PhoneTypeBadge(phone.type)
                    }
                }
            }
        }

        // Email
        if (details.emails.isNotEmpty()) {
            DetailSection(title = "Email") {
                for (email in details.emails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text     = email.value,
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp).weight(1f),
                        )
                        EmailTypeBadge(email.type)
                    }
                }
            }
        }

        // Организация
        details.organization?.let { org ->
            if (org.company.isNotEmpty() || org.title.isNotEmpty()) {
                DetailSection(title = "Организация") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Business,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        if (org.company.isNotEmpty()) {
                            Text(
                                text     = org.company,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    if (org.title.isNotEmpty()) {
                        DetailRow("Должность", org.title)
                    }
                }
            }
        }

        // Адреса
        if (details.addresses.isNotEmpty()) {
            DetailSection(title = "Адреса") {
                for (addr in details.addresses) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text     = addr.formatted,
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }

        // Заметки
        if (details.notes.isNotEmpty()) {
            DetailSection(title = "Заметки") {
                Text(
                    text  = details.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Аккаунты
        if (details.rawContacts.isNotEmpty()) {
            DetailSection(title = "Аккаунты") {
                for (raw in details.rawContacts) {
                    val accountLabel = when {
                        raw.accountName.isNotEmpty() && raw.accountType.isNotEmpty() ->
                            "${raw.accountName} (${raw.accountType})"
                        raw.accountName.isNotEmpty() -> raw.accountName
                        raw.accountType.isNotEmpty() -> raw.accountType
                        else -> "Локальный"
                    }
                    Text(
                        text  = accountLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ID
        DetailSection(title = "Идентификаторы") {
            DetailRow("Contact ID", details.id.toString(), monospace = true)
            if (details.rawContacts.isNotEmpty()) {
                DetailRow(
                    label = "Raw IDs",
                    value = details.rawContacts.joinToString(", ") { it.rawContactId.toString() },
                    monospace = true,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Кнопки действий в нижней части карточки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { component.onRefreshDetail() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Обновить", modifier = Modifier.padding(start = 6.dp))
            }
            Button(
                onClick = { component.onRequestDelete(contact) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Удалить", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val path = showSaveFileDialog(
                        defaultName = "${contact.displayName}.json",
                        ext = "json",
                        desc = "JSON-файл",
                    )
                    if (path != null) {
                        component.onExportContactToJson(contact, path)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Экспорт JSON", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = {
                    val path = showSaveFileDialog(
                        defaultName = "${contact.displayName}.vcf",
                        ext = "vcf",
                        desc = "vCard-файл",
                    )
                    if (path != null) {
                        component.onExportContactToVcf(contact, path)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Экспорт VCF", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Вспомогательные composable ───────────────────────────────────────────────

/**
 * Секция в детальной панели с заголовком и содержимым.
 */
@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

/**
 * Строка "ключ — значение" в секции деталей.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = if (monospace) FontFamily.Monospace else null,
        )
    }
}

/**
 * Бейдж типа телефона.
 */
@Composable
private fun PhoneTypeBadge(type: PhoneType) {
    val label = when (type) {
        PhoneType.MOBILE -> "Мобильный"
        PhoneType.HOME   -> "Домашний"
        PhoneType.WORK   -> "Рабочий"
        PhoneType.OTHER  -> "Другой"
    }
    Text(
        text  = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Бейдж типа email.
 */
@Composable
private fun EmailTypeBadge(type: EmailType) {
    val label = when (type) {
        EmailType.HOME  -> "Личный"
        EmailType.WORK  -> "Рабочий"
        EmailType.OTHER -> "Другой"
    }
    Text(
        text  = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Файловые диалоги ─────────────────────────────────────────────────────────

/**
 * Открыть диалог сохранения файла.
 *
 * @return Абсолютный путь к выбранному файлу, или `null` если пользователь отменил.
 */
internal fun showSaveFileDialog(
    defaultName: String,
    ext: String,
    desc: String,
): String? {
    val fc = JFileChooser().apply {
        dialogTitle          = "Сохранить как"
        selectedFile         = java.io.File(defaultName)
        fileFilter           = FileNameExtensionFilter("$desc (*.$ext)", ext)
        isAcceptAllFileFilterUsed = false
    }
    return if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = fc.selectedFile
        if (!file.name.endsWith(".$ext", ignoreCase = true)) {
            file = java.io.File("${file.absolutePath}.$ext")
        }
        file.absolutePath
    } else {
        null
    }
}

/**
 * Открыть диалог выбора файла для чтения.
 *
 * @param title      Заголовок диалога.
 * @param extensions Расширения фильтра (без точки).
 * @return Абсолютный путь к выбранному файлу, или `null` если пользователь отменил.
 */
internal fun showOpenFileDialog(title: String, vararg extensions: String): String? {
    val fc = JFileChooser().apply {
        dialogTitle = title
        if (extensions.isNotEmpty()) {
            fileFilter = FileNameExtensionFilter(
                extensions.joinToString("/") { it.uppercase() },
                *extensions,
            )
        }
        isAcceptAllFileFilterUsed = true
    }
    return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        fc.selectedFile.absolutePath
    } else {
        null
    }
}
