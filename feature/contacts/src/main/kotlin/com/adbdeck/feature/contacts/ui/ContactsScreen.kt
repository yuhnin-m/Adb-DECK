package com.adbdeck.feature.contacts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.contacts.ContactDetailState
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.ContactsListState
import com.adbdeck.feature.contacts.ContactsOperationState

/**
 * Главный экран управления контактами Android-устройства.
 *
 * Содержит:
 * - Панель инструментов (обновление, добавление, экспорт, импорт, поиск)
 * - Список контактов (левая часть)
 * - Детальная панель (правая часть, AnimatedVisibility)
 * - Диалог добавления контакта
 * - Диалог подтверждения удаления
 * - Feedback-баннер снизу
 *
 * @param component Компонент контактов.
 */
@Composable
fun ContactsScreen(component: ContactsComponent) {
    val state by component.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Тулбар ────────────────────────────────────────────────────
            ContactsToolbar(
                listState  = state.listState,
                searchQuery = state.searchQuery,
                accounts = state.availableAccounts,
                selectedAccount = state.selectedAccount,
                hasSelectedContact = state.selectedContactId != null,
                selectedContact    = (state.listState as? ContactsListState.Success)
                    ?.contacts?.firstOrNull { it.id == state.selectedContactId },
                component  = component,
            )
            HorizontalDivider()

            // ── Основной контент (список + детали) ────────────────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Левая часть — список контактов
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val listState = state.listState) {
                            is ContactsListState.NoDevice -> EmptyView(
                                message  = "Нет активного устройства",
                                modifier = Modifier.fillMaxSize(),
                            )
                            is ContactsListState.Loading  -> LoadingView(modifier = Modifier.fillMaxSize())
                            is ContactsListState.Error    -> ErrorView(
                                message  = listState.message,
                                onRetry  = { component.onRefresh() },
                                modifier = Modifier.fillMaxSize(),
                            )
                            is ContactsListState.Success  -> {
                                if (state.filteredContacts.isEmpty()) {
                                    EmptyView(
                                        message  = if (state.searchQuery.isBlank())
                                            "Контакты не найдены" else "Нет результатов для «${state.searchQuery}»",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    ContactsList(
                                        contacts          = state.filteredContacts,
                                        selectedContactId = state.selectedContactId,
                                        onSelectContact   = { component.onSelectContact(it) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Правая часть — child-панель деталей (как в Devices)
                val selectedContact = (state.listState as? ContactsListState.Success)
                    ?.contacts?.firstOrNull { it.id == state.selectedContactId }
                AnimatedVisibility(
                    visible = selectedContact != null,
                    enter   = slideInHorizontally { it },
                    exit    = slideOutHorizontally { it },
                ) {
                    if (selectedContact != null) {
                        Row {
                            VerticalDivider()
                            ContactDetailPanel(
                                contact   = selectedContact,
                                state     = state.detailState,
                                component = component,
                                modifier  = Modifier
                                    .width(360.dp)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Строка состояния ──────────────────────────────────────────
            ContactsStatusBar(listState = state.listState)
        }

        // ── Feedback-баннер (поверх контента) ─────────────────────────────
        state.actionFeedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.VARNING else AdbBannerType.SUCCESS,
                onDismiss = { component.onDismissFeedback() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }

        // ── Модальное окно длительной операции ────────────────────────────
        state.operationState?.let { operation ->
            OperationProgressDialog(
                operation = operation,
                onCancel = { component.onCancelOperation() },
            )
        }
    }

    // ── Диалоги ───────────────────────────────────────────────────────────────
    state.addForm?.let { form ->
        AddContactDialog(
            form = form,
            availableAccounts = state.availableAccounts,
            component = component,
        )
    }

    state.pendingDeleteContact?.let { contact ->
        ConfirmDeleteDialog(
            contact         = contact,
            isActionRunning = state.isActionRunning,
            onConfirm       = { component.onConfirmDelete() },
            onDismiss       = { component.onCancelDelete() },
        )
    }
}

// ── Toolbar ───────────────────────────────────────────────────────────────────

/**
 * Панель инструментов экрана контактов.
 */
@Composable
private fun ContactsToolbar(
    listState: ContactsListState,
    searchQuery: String,
    accounts: List<ContactAccount>,
    selectedAccount: ContactAccount,
    hasSelectedContact: Boolean,
    selectedContact: Contact?,
    component: ContactsComponent,
) {
    val isLoading = listState is ContactsListState.Loading

    // Флаги для dropdown-меню
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Обновить
        TextButton(onClick = { component.onRefresh() }, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
            }
            Text(text = "Обновить", modifier = Modifier.padding(start = 6.dp))
        }

        // Добавить контакт
        TextButton(onClick = { component.onShowAddForm() }) {
            Icon(Icons.Filled.PersonAdd, contentDescription = "Добавить контакт")
            Text(text = "Добавить", modifier = Modifier.padding(start = 6.dp))
        }

        Box {
            TextButton(onClick = { accountMenuExpanded = true }) {
                Icon(Icons.Filled.ManageAccounts, contentDescription = "Аккаунт")
                Text(
                    text = "Аккаунт: ${selectedAccount.uiLabel()}",
                    modifier = Modifier.padding(start = 6.dp).width(220.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(
                expanded = accountMenuExpanded,
                onDismissRequest = { accountMenuExpanded = false },
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.uiLabel()) },
                        onClick = {
                            accountMenuExpanded = false
                            component.onSelectTargetAccount(account)
                        },
                    )
                }
            }
        }

        // Меню экспорта
        Box {
            TextButton(onClick = { exportMenuExpanded = true }) {
                Icon(Icons.Outlined.Download, contentDescription = "Экспорт")
                Text(text = "Экспорт", modifier = Modifier.padding(start = 6.dp))
            }
            DropdownMenu(
                expanded         = exportMenuExpanded,
                onDismissRequest = { exportMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text    = { Text("Все контакты → JSON") },
                    onClick = {
                        exportMenuExpanded = false
                        val path = showSaveFileDialog("contacts.json", "json", "JSON-файл")
                        if (path != null) component.onExportAllToJson(path)
                    },
                )
                DropdownMenuItem(
                    text    = { Text("Все контакты → VCF") },
                    onClick = {
                        exportMenuExpanded = false
                        val path = showSaveFileDialog("contacts.vcf", "vcf", "vCard-файл")
                        if (path != null) component.onExportAllToVcf(path)
                    },
                )
                if (hasSelectedContact && selectedContact != null) {
                    DropdownMenuItem(
                        text    = { Text("Выбранный → JSON") },
                        onClick = {
                            exportMenuExpanded = false
                            val path = showSaveFileDialog(
                                "${selectedContact.displayName}.json", "json", "JSON-файл",
                            )
                            if (path != null) component.onExportContactToJson(selectedContact, path)
                        },
                    )
                    DropdownMenuItem(
                        text    = { Text("Выбранный → VCF") },
                        onClick = {
                            exportMenuExpanded = false
                            val path = showSaveFileDialog(
                                "${selectedContact.displayName}.vcf", "vcf", "vCard-файл",
                            )
                            if (path != null) component.onExportContactToVcf(selectedContact, path)
                        },
                    )
                }
            }
        }

        // Меню импорта
        Box {
            TextButton(onClick = { importMenuExpanded = true }) {
                Icon(Icons.Outlined.Upload, contentDescription = "Импорт")
                Text(text = "Импорт", modifier = Modifier.padding(start = 6.dp))
            }
            DropdownMenu(
                expanded         = importMenuExpanded,
                onDismissRequest = { importMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text    = { Text("Из JSON") },
                    onClick = {
                        importMenuExpanded = false
                        val path = showOpenFileDialog("Выберите JSON-файл", "json")
                        if (path != null) component.onImportFromJson(path)
                    },
                )
                DropdownMenuItem(
                    text    = { Text("Из VCF") },
                    onClick = {
                        importMenuExpanded = false
                        val path = showOpenFileDialog("Выберите VCF-файл", "vcf")
                        if (path != null) component.onImportFromVcf(path)
                    },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Поиск
        OutlinedTextField(
            value           = searchQuery,
            onValueChange   = { component.onSearchChanged(it) },
            placeholder     = { Text("Поиск по имени, телефону, email") },
            modifier        = Modifier.width(280.dp),
            singleLine      = true,
            textStyle       = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ── Список контактов ──────────────────────────────────────────────────────────

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    selectedContactId: Long?,
    onSelectContact: (Contact) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = contacts, key = { it.id }) { contact ->
            ContactRow(
                contact    = contact,
                isSelected = contact.id == selectedContactId,
                onClick    = { onSelectContact(contact) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        }
    }
}

/**
 * Строка контакта в списке.
 */
@Composable
private fun ContactRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Filled.Person,
            contentDescription = null,
            modifier           = Modifier.size(36.dp).padding(4.dp),
            tint               = MaterialTheme.colorScheme.primary,
        )

        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text       = contact.displayName.ifEmpty { "Без имени" },
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (contact.phones.isNotEmpty()) {
                Text(
                    text       = contact.phones.first().value,
                    style      = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.primaryEmail.isNotEmpty()) {
                Text(
                    text  = contact.primaryEmail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Бейдж аккаунта
        if (contact.accountType.isNotEmpty()) {
            Text(
                text  = contact.accountType,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ── Строка состояния ──────────────────────────────────────────────────────────

@Composable
private fun ContactsStatusBar(
    listState: ContactsListState,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val statusText = when (listState) {
            is ContactsListState.NoDevice -> "Устройство не подключено"
            is ContactsListState.Loading  -> "Загрузка контактов..."
            is ContactsListState.Error    -> "Ошибка загрузки"
            is ContactsListState.Success  ->
                "${listState.contacts.size} контакт(ов)"
        }
        Text(
            text  = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Диалог подтверждения удаления ────────────────────────────────────────────

@Composable
private fun ConfirmDeleteDialog(
    contact: Contact,
    isActionRunning: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isActionRunning) onDismiss() },
        title            = { Text("Удалить контакт?") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Контакт «${contact.displayName}» будет удалён с устройства. Это действие нельзя отменить.")
                if (isActionRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text     = "Удаление...",
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = !isActionRunning,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isActionRunning) {
                Text("Отмена")
            }
        },
    )
}

@Composable
private fun OperationProgressDialog(
    operation: ContactsOperationState,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(operation.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (operation.isIndeterminate) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    val current = operation.currentStep ?: 0
                    val total = operation.totalSteps ?: 1
                    LinearProgressIndicator(
                        progress = { (current.toFloat() / total.coerceAtLeast(1).toFloat()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "$current / $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = operation.status,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        operation.logs.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCancel) {
                Text("Отмена")
            }
        },
    )
}
