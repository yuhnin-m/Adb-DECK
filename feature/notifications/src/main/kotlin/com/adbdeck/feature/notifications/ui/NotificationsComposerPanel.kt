package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_composer_enable_content_intent
import adbdeck.feature.notifications.generated.resources.notifications_composer_enable_deep_link
import adbdeck.feature.notifications.generated.resources.notifications_composer_enable_icon
import adbdeck.feature.notifications.generated.resources.notifications_composer_enable_large_icon
import adbdeck.feature.notifications.generated.resources.notifications_composer_enable_picture
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_big_text
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_content_intent
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_conversation
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_deep_link
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_icon
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_inbox_lines
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_large_icon
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_messages
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_picture_spec
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_style
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_tag
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_text
import adbdeck.feature.notifications.generated.resources.notifications_composer_field_title
import adbdeck.feature.notifications.generated.resources.notifications_composer_fill_saved
import adbdeck.feature.notifications.generated.resources.notifications_composer_fill_selected
import adbdeck.feature.notifications.generated.resources.notifications_composer_hint_deep_link
import adbdeck.feature.notifications.generated.resources.notifications_composer_hint_inbox_lines
import adbdeck.feature.notifications.generated.resources.notifications_composer_hint_intent
import adbdeck.feature.notifications.generated.resources.notifications_composer_hint_messages
import adbdeck.feature.notifications.generated.resources.notifications_composer_host_image_encode_failed
import adbdeck.feature.notifications.generated.resources.notifications_composer_host_image_file_not_found
import adbdeck.feature.notifications.generated.resources.notifications_composer_host_image_selected
import adbdeck.feature.notifications.generated.resources.notifications_composer_no_device
import adbdeck.feature.notifications.generated.resources.notifications_composer_pick_host_image
import adbdeck.feature.notifications.generated.resources.notifications_composer_saved_placeholder
import adbdeck.feature.notifications.generated.resources.notifications_composer_section_base
import adbdeck.feature.notifications.generated.resources.notifications_composer_section_intent
import adbdeck.feature.notifications.generated.resources.notifications_composer_section_source
import adbdeck.feature.notifications.generated.resources.notifications_composer_section_style
import adbdeck.feature.notifications.generated.resources.notifications_composer_section_visual
import adbdeck.feature.notifications.generated.resources.notifications_composer_reset_all
import adbdeck.feature.notifications.generated.resources.notifications_composer_send
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_bigpicture
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_bigtext
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_error_inbox_required
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_error_messaging_format
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_error_messaging_required
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_error_picture_required
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_inbox
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_media
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_messaging
import adbdeck.feature.notifications.generated.resources.notifications_composer_style_none
import adbdeck.feature.notifications.generated.resources.notifications_composer_subtitle_device
import adbdeck.feature.notifications.generated.resources.notifications_composer_switch_verbose
import adbdeck.feature.notifications.generated.resources.notifications_composer_title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.adb.api.notifications.NotificationPostMessage
import com.adbdeck.core.adb.api.notifications.NotificationPostRequest
import com.adbdeck.core.adb.api.notifications.NotificationPostStyle
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.notifications.NotificationsState
import com.adbdeck.feature.notifications.SavedNotification
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.Base64

/**
 * Правая дочерняя панель-конструктор тестового уведомления.
 *
 * Содержит расширенный набор настроек `cmd notification post` и инструменты автозаполнения:
 * - prefill из выбранного/сохранённого уведомления;
 * - выбор изображения с ФС хоста с преобразованием в `data:base64` iconspec;
 * - отдельные поля для intent/deep link.
 *
 * Примечание: `cmd notification post` не поддерживает action-кнопки.
 * Для открытия приложения при тапе используйте поле content intent или deep link.
 */
@Composable
internal fun NotificationsComposerPanel(
    modifier: Modifier,
    state: NotificationsState,
    onDismissRequest: () -> Unit,
    onSend: (NotificationPostRequest) -> Unit,
) {
    val initialRecord = remember(state.selectedRecord, state.savedNotifications) {
        state.selectedRecord ?: state.savedNotifications.firstOrNull()?.record
    }

    var tag by remember { mutableStateOf(defaultTag(initialRecord)) }
    var title by remember { mutableStateOf(initialRecord?.title.orEmpty()) }
    var text by remember { mutableStateOf(defaultText(initialRecord)) }
    var iconSpec by remember { mutableStateOf("") }
    var largeIconSpec by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(NotificationPostStyle.NONE) }
    var bigText by remember { mutableStateOf(initialRecord?.bigText.orEmpty()) }
    var pictureSpec by remember { mutableStateOf("") }
    var inboxLinesRaw by remember { mutableStateOf("") }
    var messagingConversationTitle by remember { mutableStateOf("") }
    var messagingMessagesRaw by remember { mutableStateOf("") }
    var contentIntentSpec by remember { mutableStateOf("") }
    var deepLink by remember { mutableStateOf("") }
    var verbose by remember { mutableStateOf(false) }
    var selectedSavedId by remember { mutableStateOf<String?>(null) }

    var isIconEnabled by remember { mutableStateOf(false) }
    var isLargeIconEnabled by remember { mutableStateOf(false) }
    var isPictureEnabled by remember { mutableStateOf(false) }
    var isContentIntentEnabled by remember { mutableStateOf(false) }
    var isDeepLinkEnabled by remember { mutableStateOf(false) }

    var imageStatusMessage by remember { mutableStateOf<String?>(null) }
    var isPickingIcon by remember { mutableStateOf(false) }
    var isPickingLargeIcon by remember { mutableStateOf(false) }
    var isPickingPicture by remember { mutableStateOf(false) }

    val savedOptions = remember(state.savedNotifications) {
        state.savedNotifications.map { saved ->
            AdbDropdownOption(
                value = saved.id,
                label = buildSavedOptionLabel(saved),
            )
        }
    }
    val selectedSavedRecord = remember(selectedSavedId, state.savedNotifications) {
        state.savedNotifications.firstOrNull { it.id == selectedSavedId }?.record
    }

    val styleOptions = rememberStyleOptions()

    val parsedInboxLines = remember(inboxLinesRaw) { parsePipeSeparatedValues(inboxLinesRaw) }
    val parsedMessagingMessages = remember(messagingMessagesRaw) {
        parseMessagingMessages(messagingMessagesRaw)
    }

    val styleValidationError = remember(
        style,
        pictureSpec,
        isPictureEnabled,
        parsedInboxLines,
        parsedMessagingMessages,
        messagingMessagesRaw,
    ) {
        when {
            style == NotificationPostStyle.BIGPICTURE && (!isPictureEnabled || pictureSpec.isBlank()) ->
                Res.string.notifications_composer_style_error_picture_required

            style == NotificationPostStyle.INBOX && parsedInboxLines.isEmpty() ->
                Res.string.notifications_composer_style_error_inbox_required

            style == NotificationPostStyle.MESSAGING && parsedMessagingMessages.isEmpty() ->
                Res.string.notifications_composer_style_error_messaging_required

            style == NotificationPostStyle.MESSAGING &&
                messagingMessagesRaw.isNotBlank() &&
                parsedMessagingMessages.size < parsePipeSeparatedValues(messagingMessagesRaw).size ->
                Res.string.notifications_composer_style_error_messaging_format

            else -> null
        }
    }

    val hasDevice = state.activeDeviceId != null
    val canSend = hasDevice &&
        tag.isNotBlank() &&
        text.isNotBlank() &&
        styleValidationError == null
    val pickDialogTitleText = stringResource(Res.string.notifications_composer_pick_host_image)
    val hostImageSelectedPattern = stringResource(
        Res.string.notifications_composer_host_image_selected,
        IMAGE_PATH_PLACEHOLDER,
    )
    val hostImageNotFoundPattern = stringResource(
        Res.string.notifications_composer_host_image_file_not_found,
        IMAGE_PATH_PLACEHOLDER,
    )
    val hostImageEncodeFailedPattern = stringResource(
        Res.string.notifications_composer_host_image_encode_failed,
        IMAGE_PATH_PLACEHOLDER,
    )

    fun prefillFromRecord(record: NotificationRecord) {
        val prefilled = prefillComposerFromRecord(record)
        tag = prefilled.tag
        title = prefilled.title
        text = prefilled.text
        bigText = prefilled.bigText
    }

    fun resetAllFields() {
        val defaultRecord = initialRecord
        tag = defaultTag(defaultRecord)
        title = defaultRecord?.title.orEmpty()
        text = defaultText(defaultRecord)
        iconSpec = ""
        largeIconSpec = ""
        style = NotificationPostStyle.NONE
        bigText = defaultRecord?.bigText.orEmpty()
        pictureSpec = ""
        inboxLinesRaw = ""
        messagingConversationTitle = ""
        messagingMessagesRaw = ""
        contentIntentSpec = ""
        deepLink = ""
        verbose = false
        selectedSavedId = null
        imageStatusMessage = null

        isIconEnabled = false
        isLargeIconEnabled = false
        isPictureEnabled = false
        isContentIntentEnabled = false
        isDeepLinkEnabled = false
    }

    fun pickImageTo(
        onValueReady: (String) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        if (!hasDevice) return

        setLoading(true)
        imageStatusMessage = null
        try {
            val selectedPath = showNotificationOpenImageFileDialog(pickDialogTitleText)

            if (selectedPath.isNullOrBlank()) {
                return
            }

            val encodedResult = encodeImageAsDataUri(path = selectedPath)
            encodedResult.onSuccess { spec ->
                onValueReady(spec)
                imageStatusMessage = hostImageSelectedPattern.replace(IMAGE_PATH_PLACEHOLDER, selectedPath)
            }.onFailure { error ->
                imageStatusMessage = if (error.message?.contains("not found", ignoreCase = true) == true) {
                    hostImageNotFoundPattern.replace(IMAGE_PATH_PLACEHOLDER, selectedPath)
                } else {
                    hostImageEncodeFailedPattern.replace(IMAGE_PATH_PLACEHOLDER, selectedPath)
                }
            }
        } finally {
            setLoading(false)
        }
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.notifications_composer_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (hasDevice) {
                    Text(
                        text = stringResource(
                            Res.string.notifications_composer_subtitle_device,
                            state.activeDeviceId.orEmpty(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.notifications_composer_no_device),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            AdbOutlinedButton(
                onClick = ::resetAllFields,
                text = stringResource(Res.string.notifications_composer_reset_all),
                size = AdbButtonSize.SMALL,
            )
            AdbPlainButton(
                onClick = onDismissRequest,
                leadingIcon = Icons.Outlined.Close,
                type = AdbButtonType.NEUTRAL,
                contentDescription = stringResource(AdbCommonStringRes.actionClose),
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.paddingSmall)
                .padding(bottom = Dimensions.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbSectionCard(
                title = stringResource(Res.string.notifications_composer_section_source),
                titleTextStyle = MaterialTheme.typography.labelMedium,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdbOutlinedButton(
                        onClick = { state.selectedRecord?.let(::prefillFromRecord) },
                        text = stringResource(Res.string.notifications_composer_fill_selected),
                        enabled = state.selectedRecord != null,
                        size = AdbButtonSize.SMALL,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdbOutlinedDropdownTextField(
                        options = savedOptions,
                        selectedValue = selectedSavedId,
                        onValueSelected = { selectedSavedId = it },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(Res.string.notifications_composer_saved_placeholder),
                        size = AdbTextFieldSize.SMALL,
                        enabled = savedOptions.isNotEmpty(),
                    )
                    AdbOutlinedButton(
                        onClick = { selectedSavedRecord?.let(::prefillFromRecord) },
                        text = stringResource(Res.string.notifications_composer_fill_saved),
                        enabled = selectedSavedRecord != null,
                        size = AdbButtonSize.SMALL,
                    )
                }
            }

            AdbSectionCard(
                title = stringResource(Res.string.notifications_composer_section_base),
                titleTextStyle = MaterialTheme.typography.labelMedium,
            ) {
                AdbOutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    placeholder = stringResource(Res.string.notifications_composer_field_tag),
                    size = AdbTextFieldSize.MEDIUM,
                )
                AdbOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(Res.string.notifications_composer_field_title),
                    size = AdbTextFieldSize.MEDIUM,
                )
                AdbOutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = stringResource(Res.string.notifications_composer_field_text),
                    size = AdbTextFieldSize.MEDIUM,
                )
            }

            AdbSectionCard(
                title = stringResource(Res.string.notifications_composer_section_visual),
                titleTextStyle = MaterialTheme.typography.labelMedium,
            ) {
                OptionalToggleRow(
                    label = stringResource(Res.string.notifications_composer_enable_icon),
                    checked = isIconEnabled,
                    onCheckedChange = { isIconEnabled = it },
                )
                if (isIconEnabled) {
                    AdbOutlinedTextField(
                        value = iconSpec,
                        onValueChange = { iconSpec = it },
                        placeholder = stringResource(Res.string.notifications_composer_field_icon),
                        size = AdbTextFieldSize.MEDIUM,
                        leadingIcon = Icons.Outlined.Image,
                    )
                    AdbOutlinedButton(
                        onClick = {
                            pickImageTo(
                                onValueReady = { iconSpec = it },
                                setLoading = { isPickingIcon = it },
                            )
                        },
                        text = stringResource(Res.string.notifications_composer_pick_host_image),
                        leadingIcon = Icons.Outlined.Upload,
                        loading = isPickingIcon,
                        enabled = hasDevice,
                        size = AdbButtonSize.SMALL,
                    )
                }

                OptionalToggleRow(
                    label = stringResource(Res.string.notifications_composer_enable_large_icon),
                    checked = isLargeIconEnabled,
                    onCheckedChange = { isLargeIconEnabled = it },
                )
                if (isLargeIconEnabled) {
                    AdbOutlinedTextField(
                        value = largeIconSpec,
                        onValueChange = { largeIconSpec = it },
                        placeholder = stringResource(Res.string.notifications_composer_field_large_icon),
                        size = AdbTextFieldSize.MEDIUM,
                        leadingIcon = Icons.Outlined.Image,
                    )
                    AdbOutlinedButton(
                        onClick = {
                            pickImageTo(
                                onValueReady = { largeIconSpec = it },
                                setLoading = { isPickingLargeIcon = it },
                            )
                        },
                        text = stringResource(Res.string.notifications_composer_pick_host_image),
                        leadingIcon = Icons.Outlined.Upload,
                        loading = isPickingLargeIcon,
                        enabled = hasDevice,
                        size = AdbButtonSize.SMALL,
                    )
                }

                imageStatusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AdbSectionCard(
                title = stringResource(Res.string.notifications_composer_section_style),
                titleTextStyle = MaterialTheme.typography.labelMedium,
            ) {
                AdbOutlinedDropdownTextField(
                    options = styleOptions,
                    selectedValue = style,
                    onValueSelected = { style = it },
                    placeholder = stringResource(Res.string.notifications_composer_field_style),
                    size = AdbTextFieldSize.MEDIUM,
                )

                when (style) {
                    NotificationPostStyle.NONE,
                    NotificationPostStyle.MEDIA -> Unit

                    NotificationPostStyle.BIGTEXT -> {
                        AdbOutlinedTextField(
                            value = bigText,
                            onValueChange = { bigText = it },
                            placeholder = stringResource(Res.string.notifications_composer_field_big_text),
                            size = AdbTextFieldSize.MEDIUM,
                        )
                    }

                    NotificationPostStyle.BIGPICTURE -> {
                        OptionalToggleRow(
                            label = stringResource(Res.string.notifications_composer_enable_picture),
                            checked = isPictureEnabled,
                            onCheckedChange = { isPictureEnabled = it },
                        )
                        if (isPictureEnabled) {
                            AdbOutlinedTextField(
                                value = pictureSpec,
                                onValueChange = { pictureSpec = it },
                                placeholder = stringResource(Res.string.notifications_composer_field_picture_spec),
                                size = AdbTextFieldSize.MEDIUM,
                                leadingIcon = Icons.Outlined.Image,
                            )
                            AdbOutlinedButton(
                                onClick = {
                                    pickImageTo(
                                        onValueReady = { pictureSpec = it },
                                        setLoading = { isPickingPicture = it },
                                    )
                                },
                                text = stringResource(Res.string.notifications_composer_pick_host_image),
                                leadingIcon = Icons.Outlined.Upload,
                                loading = isPickingPicture,
                                enabled = hasDevice,
                                size = AdbButtonSize.SMALL,
                            )
                        }
                    }

                    NotificationPostStyle.INBOX -> {
                        AdbOutlinedTextField(
                            value = inboxLinesRaw,
                            onValueChange = { inboxLinesRaw = it },
                            placeholder = stringResource(Res.string.notifications_composer_field_inbox_lines),
                            supportingText = stringResource(Res.string.notifications_composer_hint_inbox_lines),
                            size = AdbTextFieldSize.MEDIUM,
                        )
                    }

                    NotificationPostStyle.MESSAGING -> {
                        AdbOutlinedTextField(
                            value = messagingConversationTitle,
                            onValueChange = { messagingConversationTitle = it },
                            placeholder = stringResource(Res.string.notifications_composer_field_conversation),
                            size = AdbTextFieldSize.MEDIUM,
                        )
                        AdbOutlinedTextField(
                            value = messagingMessagesRaw,
                            onValueChange = { messagingMessagesRaw = it },
                            placeholder = stringResource(Res.string.notifications_composer_field_messages),
                            supportingText = stringResource(Res.string.notifications_composer_hint_messages),
                            size = AdbTextFieldSize.MEDIUM,
                        )
                    }
                }

                styleValidationError?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            AdbSectionCard(
                title = stringResource(Res.string.notifications_composer_section_intent),
                titleTextStyle = MaterialTheme.typography.labelMedium,
            ) {
                OptionalToggleRow(
                    label = stringResource(Res.string.notifications_composer_enable_content_intent),
                    checked = isContentIntentEnabled,
                    onCheckedChange = { isContentIntentEnabled = it },
                )
                if (isContentIntentEnabled) {
                    AdbOutlinedTextField(
                        value = contentIntentSpec,
                        onValueChange = { contentIntentSpec = it },
                        placeholder = stringResource(Res.string.notifications_composer_field_content_intent),
                        supportingText = stringResource(Res.string.notifications_composer_hint_intent),
                        size = AdbTextFieldSize.MEDIUM,
                    )
                }

                OptionalToggleRow(
                    label = stringResource(Res.string.notifications_composer_enable_deep_link),
                    checked = isDeepLinkEnabled,
                    onCheckedChange = { isDeepLinkEnabled = it },
                )
                if (isDeepLinkEnabled) {
                    AdbOutlinedTextField(
                        value = deepLink,
                        onValueChange = { deepLink = it },
                        placeholder = stringResource(Res.string.notifications_composer_field_deep_link),
                        supportingText = stringResource(Res.string.notifications_composer_hint_deep_link),
                        size = AdbTextFieldSize.MEDIUM,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.notifications_composer_switch_verbose),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Switch(
                        checked = verbose,
                        onCheckedChange = { verbose = it },
                    )
                }
            }

            AdbFilledButton(
                onClick = {
                    val resolvedIntentSpec = resolveIntentSpec(
                        explicitIntentSpec = if (isContentIntentEnabled) contentIntentSpec else "",
                        deepLink = if (isDeepLinkEnabled) deepLink else "",
                    )

                    val request = NotificationPostRequest(
                        tag = tag,
                        text = text,
                        title = title.ifBlank { null },
                        verbose = verbose,
                        iconSpec = if (isIconEnabled) iconSpec.ifBlank { null } else null,
                        largeIconSpec = if (isLargeIconEnabled) largeIconSpec.ifBlank { null } else null,
                        style = style,
                        bigText = bigText.ifBlank { null },
                        pictureSpec = if (isPictureEnabled) pictureSpec.ifBlank { null } else null,
                        inboxLines = parsedInboxLines,
                        messagingConversationTitle = messagingConversationTitle.ifBlank { null },
                        messagingMessages = parsedMessagingMessages,
                        contentIntentSpec = resolvedIntentSpec,
                    )
                    onSend(request)
                },
                text = stringResource(Res.string.notifications_composer_send),
                enabled = canSend,
                loading = state.isPostingNotification,
                type = AdbButtonType.SUCCESS,
                size = AdbButtonSize.MEDIUM,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun rememberStyleOptions(): List<AdbDropdownOption<NotificationPostStyle>> {
    val noneLabel = stringResource(Res.string.notifications_composer_style_none)
    val bigTextLabel = stringResource(Res.string.notifications_composer_style_bigtext)
    val bigPictureLabel = stringResource(Res.string.notifications_composer_style_bigpicture)
    val inboxLabel = stringResource(Res.string.notifications_composer_style_inbox)
    val messagingLabel = stringResource(Res.string.notifications_composer_style_messaging)
    val mediaLabel = stringResource(Res.string.notifications_composer_style_media)

    return remember(
        noneLabel,
        bigTextLabel,
        bigPictureLabel,
        inboxLabel,
        messagingLabel,
        mediaLabel,
    ) {
        listOf(
            AdbDropdownOption(NotificationPostStyle.NONE, noneLabel),
            AdbDropdownOption(NotificationPostStyle.BIGTEXT, bigTextLabel),
            AdbDropdownOption(NotificationPostStyle.BIGPICTURE, bigPictureLabel),
            AdbDropdownOption(NotificationPostStyle.INBOX, inboxLabel),
            AdbDropdownOption(NotificationPostStyle.MESSAGING, messagingLabel),
            AdbDropdownOption(NotificationPostStyle.MEDIA, mediaLabel),
        )
    }
}

private data class ComposerPrefill(
    val tag: String,
    val title: String,
    val text: String,
    val bigText: String,
)

private fun prefillComposerFromRecord(record: NotificationRecord): ComposerPrefill {
    return ComposerPrefill(
        tag = defaultTag(record),
        title = record.title.orEmpty(),
        text = defaultText(record),
        bigText = record.bigText.orEmpty(),
    )
}

private fun defaultTag(record: NotificationRecord?): String {
    val fromRecord = record?.tag?.takeIf { it.isNotBlank() }
    if (fromRecord != null) return fromRecord
    return "adbdeck_test"
}

private fun defaultText(record: NotificationRecord?): String =
    record?.text
        ?.takeIf { it.isNotBlank() }
        ?: record?.title
            ?.takeIf { it.isNotBlank() }
            ?: "Test notification"

private fun parsePipeSeparatedValues(raw: String): List<String> =
    raw.split("|")
        .asSequence()
        .map { token -> token.trim() }
        .filter { token -> token.isNotEmpty() }
        .toList()

private fun parseMessagingMessages(raw: String): List<NotificationPostMessage> =
    parsePipeSeparatedValues(raw)
        .mapNotNull { token ->
            val separatorIndex = token.indexOf(':')
            if (separatorIndex <= 0 || separatorIndex >= token.lastIndex) {
                return@mapNotNull null
            }
            val who = token.substring(0, separatorIndex).trim()
            val text = token.substring(separatorIndex + 1).trim()
            if (who.isEmpty() || text.isEmpty()) {
                null
            } else {
                NotificationPostMessage(who = who, text = text)
            }
        }

private fun resolveIntentSpec(
    explicitIntentSpec: String,
    deepLink: String,
): String? {
    explicitIntentSpec.trim().takeIf { it.isNotBlank() }?.let { return it }

    deepLink.trim().takeIf { it.isNotBlank() }?.let { url ->
        return "activity -a android.intent.action.VIEW -d \"$url\""
    }

    return null
}

private fun buildSavedOptionLabel(saved: SavedNotification): String {
    val suffix = saved.record.title
        ?.takeIf { it.isNotBlank() }
        ?.let { title -> " • $title" }
        .orEmpty()
    return "${saved.record.packageName}$suffix (${formatNotificationFullTime(saved.savedAt)})"
}

private fun encodeImageAsDataUri(path: String): Result<String> = runCatching {
    val sourceFile = File(path)
    require(sourceFile.isFile) { "Host image file not found." }
    val bytes = sourceFile.readBytes()
    require(bytes.isNotEmpty()) { "Host image file is empty." }
    val encoded = Base64.getEncoder().encodeToString(bytes)
    "data:base64,$encoded"
}

@Composable
private fun OptionalToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private const val IMAGE_PATH_PLACEHOLDER: String = "{path}"
