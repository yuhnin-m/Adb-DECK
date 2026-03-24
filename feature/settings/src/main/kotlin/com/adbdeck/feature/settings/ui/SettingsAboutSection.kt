package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_about_developer_label
import adbdeck.feature.settings.generated.resources.settings_about_developer_value
import adbdeck.feature.settings.generated.resources.settings_about_issues_label
import adbdeck.feature.settings.generated.resources.settings_about_issues_value
import adbdeck.feature.settings.generated.resources.settings_about_license_label
import adbdeck.feature.settings.generated.resources.settings_about_license_value
import adbdeck.feature.settings.generated.resources.settings_about_platforms_label
import adbdeck.feature.settings.generated.resources.settings_about_platforms_value
import adbdeck.feature.settings.generated.resources.settings_about_project_label
import adbdeck.feature.settings.generated.resources.settings_about_project_value
import adbdeck.feature.settings.generated.resources.settings_about_repository_label
import adbdeck.feature.settings.generated.resources.settings_about_repository_value
import adbdeck.feature.settings.generated.resources.settings_about_updates_label
import adbdeck.feature.settings.generated.resources.settings_action_checking
import adbdeck.feature.settings.generated.resources.settings_action_check_updates
import adbdeck.feature.settings.generated.resources.settings_section_about
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.settings.SettingsComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

private data class AboutSectionUiState(
    val isCheckingUpdates: Boolean,
)

@Composable
internal fun AboutSectionHost(component: SettingsComponent) {
    val initial = remember(component) {
        AboutSectionUiState(
            isCheckingUpdates = component.state.value.isCheckingUpdates,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { AboutSectionUiState(isCheckingUpdates = it.isCheckingUpdates) }
            .distinctUntilChanged()
    }.collectAsState(initial = initial)

    AboutSection(
        state = uiState,
        onCheckForUpdates = component::onCheckForUpdates,
    )
}

@Composable
private fun AboutSection(
    state: AboutSectionUiState,
    onCheckForUpdates: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_about)

    val projectLabel = stringResource(Res.string.settings_about_project_label)
    val projectValue = stringResource(Res.string.settings_about_project_value)
    val repositoryLabel = stringResource(Res.string.settings_about_repository_label)
    val repositoryValue = stringResource(Res.string.settings_about_repository_value)
    val issuesLabel = stringResource(Res.string.settings_about_issues_label)
    val issuesValue = stringResource(Res.string.settings_about_issues_value)
    val developerLabel = stringResource(Res.string.settings_about_developer_label)
    val developerValue = stringResource(Res.string.settings_about_developer_value)
    val licenseLabel = stringResource(Res.string.settings_about_license_label)
    val licenseValue = stringResource(Res.string.settings_about_license_value)
    val platformsLabel = stringResource(Res.string.settings_about_platforms_label)
    val platformsValue = stringResource(Res.string.settings_about_platforms_value)
    val updatesLabel = stringResource(Res.string.settings_about_updates_label)
    val checkUpdatesLabel = if (state.isCheckingUpdates) {
        stringResource(Res.string.settings_action_checking)
    } else {
        stringResource(Res.string.settings_action_check_updates)
    }

    val aboutContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    val aboutBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.primary,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = aboutContainerColor,
        border = BorderStroke(1.dp, aboutBorderColor),
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AboutInfoRow(
            label = projectLabel,
            value = projectValue,
        )
        HorizontalDivider()
        AboutInfoRow(
            label = repositoryLabel,
            value = repositoryValue,
            url = repositoryValue,
        )
        HorizontalDivider()
        AboutInfoRow(
            label = issuesLabel,
            value = issuesValue,
            url = issuesValue,
        )
        HorizontalDivider()
        AboutInfoRow(
            label = developerLabel,
            value = developerValue,
        )
        HorizontalDivider()
        AboutInfoRow(
            label = licenseLabel,
            value = licenseValue,
        )
        HorizontalDivider()
        AboutInfoRow(
            label = platformsLabel,
            value = platformsValue,
        )
        HorizontalDivider()
        AboutUpdatesRow(
            label = updatesLabel,
            actionLabel = checkUpdatesLabel,
            checking = state.isCheckingUpdates,
            onCheckForUpdates = onCheckForUpdates,
        )
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String,
    url: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.34f),
        )

        val valueModifier = Modifier.weight(0.66f)
        if (url.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = valueModifier,
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = valueModifier.clickable { openExternalUrl(url) },
            )
        }
    }
}

@Composable
private fun AboutUpdatesRow(
    label: String,
    actionLabel: String,
    checking: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.34f),
        )

        AdbOutlinedButton(
            onClick = onCheckForUpdates,
            text = actionLabel,
            loading = checking,
            enabled = !checking,
            type = AdbButtonType.NEUTRAL,
            modifier = Modifier.weight(0.66f),
            fullWidth = true,
        )
    }
}

private fun openExternalUrl(url: String) {
    if (!Desktop.isDesktopSupported()) return
    val desktop = runCatching { Desktop.getDesktop() }.getOrNull() ?: return
    if (!desktop.isSupported(Desktop.Action.BROWSE)) return
    runCatching { desktop.browse(URI(url)) }
}
