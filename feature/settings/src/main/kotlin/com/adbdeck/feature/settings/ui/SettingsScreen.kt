package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_action_save
import adbdeck.feature.settings.generated.resources.settings_action_saving
import adbdeck.feature.settings.generated.resources.settings_save_hint_pending
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.feature.settings.SettingsComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

private data class SaveActionUiState(
    val isSaving: Boolean,
    val hasPendingChanges: Boolean,
)

@Composable
fun SettingsScreen(component: SettingsComponent) {
    var openedHelpTool by remember { mutableStateOf<HelpTool?>(null) }
    val initialLanguage = remember(component) { component.state.value.currentLanguage }
    val currentLanguage by remember(component) {
        component.state.map { it.currentLanguage }.distinctUntilChanged()
    }.collectAsState(initial = initialLanguage)

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimensions.paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
                ) {
                    AppearanceSectionHost(component = component)
                    ToolsSectionHost(
                        component = component,
                        onOpenGuide = { openedHelpTool = it },
                    )
                    AboutSectionHost(component = component)
                    SaveActionHost(component = component)
                }

                openedHelpTool?.let { tool ->
                    VerticalDivider()
                    HelpSidebarPanel(
                        tool = tool,
                        currentLanguage = currentLanguage,
                        onClose = { openedHelpTool = null },
                        modifier = Modifier
                            .width(Dimensions.sidebarWidth * 2)
                            .fillMaxHeight(),
                    )
                }
            }

            SaveFeedbackHost(
                component = component,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Dimensions.paddingDefault),
            )
        }
    }
}

@Composable
private fun SaveFeedbackHost(
    component: SettingsComponent,
    modifier: Modifier = Modifier,
) {
    val initialFeedback = remember(component) { component.state.value.saveFeedback }
    val feedback by remember(component) {
        component.state.map { it.saveFeedback }.distinctUntilChanged()
    }.collectAsState(initial = initialFeedback)

    feedback?.let { banner ->
        AdbBanner(
            message = banner.message,
            type = if (banner.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
            onDismiss = component::onDismissFeedback,
            modifier = modifier,
        )
    }
}

@Composable
private fun SaveActionHost(component: SettingsComponent) {
    val initial = remember(component) {
        SaveActionUiState(
            isSaving = component.state.value.isSaving,
            hasPendingChanges = component.state.value.hasPendingChanges,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { SaveActionUiState(isSaving = it.isSaving, hasPendingChanges = it.hasPendingChanges) }
            .distinctUntilChanged()
    }.collectAsState(initial = initial)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        if (uiState.hasPendingChanges) {
            Text(
                text = stringResource(Res.string.settings_save_hint_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }

        AdbFilledButton(
            onClick = component::onSave,
            text = if (uiState.isSaving) {
                stringResource(Res.string.settings_action_saving)
            } else {
                stringResource(Res.string.settings_action_save)
            },
            modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
            loading = uiState.isSaving,
            enabled = uiState.hasPendingChanges && !uiState.isSaving,
            size = AdbButtonSize.LARGE,
            fullWidth = true,
        )
    }
}
