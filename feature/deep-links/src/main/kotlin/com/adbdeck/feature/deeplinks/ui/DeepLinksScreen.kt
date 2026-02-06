package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.layout.Box
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
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.models.toFormUiState
import com.adbdeck.feature.deeplinks.models.toRightPanelUiState
import com.adbdeck.feature.deeplinks.models.toStatusBarUiState

/**
 * Главный экран Deep Links / Intents.
 *
 * Макет:
 * - Левая панель (420dp): переключатель режима + форма + кнопка запуска
 * - Правая панель: три вкладки — Команда/Результат, История, Шаблоны
 * - Строка состояния снизу
 * - Диалог сохранения шаблона (если открыт)
 *
 * @param component Компонент экрана.
 */
@Composable
fun DeepLinksScreen(component: DeepLinksComponent) {
    val state by component.state.collectAsState()
    val formUiState = remember(
        state.mode,
        state.dlUri,
        state.dlAction,
        state.dlPackage,
        state.dlComponent,
        state.dlCategory,
        state.itAction,
        state.itDataUri,
        state.itPackage,
        state.itComponent,
        state.itCategories,
        state.itFlags,
        state.itSelectedFlags,
        state.itFlagsValidationMessage,
        state.itExtras,
        state.activeDeviceId,
        state.isLaunching,
        state.packageSuggestions,
        state.isPackageSuggestionsLoading,
        state.isIntentFlagsDialogOpen,
    ) {
        state.toFormUiState()
    }
    val rightPanelUiState = remember(
        state.rightTab,
        state.commandPreview,
        state.isLaunching,
        state.lastResult,
        state.history,
        state.templates,
    ) {
        state.toRightPanelUiState()
    }
    val statusBarUiState = remember(
        state.activeDeviceId,
        state.history.size,
        state.templates.size,
    ) {
        state.toStatusBarUiState()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                DeepLinksFormPanel(
                    state = formUiState,
                    component = component,
                    modifier = Modifier.width(420.dp).fillMaxHeight(),
                )

                VerticalDivider()

                DeepLinksRightPanel(
                    state = rightPanelUiState,
                    component = component,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            HorizontalDivider()
            DeepLinksStatusBar(state = statusBarUiState)
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

    if (state.isSaveTemplateDialogOpen) {
        SaveTemplateDialog(
            name = state.saveTemplateName,
            onNameChanged = { component.onSaveTemplateNameChanged(it) },
            onConfirm = { component.onConfirmSaveTemplate() },
            onDismiss = { component.onDismissSaveTemplateDialog() },
        )
    }

    if (state.isIntentFlagsDialogOpen) {
        IntentFlagsDialog(
            initialSelectedFlagKeys = state.itSelectedFlags,
            onApply = component::onApplyIntentFlagsSelection,
            onDismiss = component::onDismissIntentFlagsDialog,
        )
    }
}
