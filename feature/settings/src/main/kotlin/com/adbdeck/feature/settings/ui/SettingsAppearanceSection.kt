package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_language_description
import adbdeck.feature.settings.generated.resources.settings_language_english
import adbdeck.feature.settings.generated.resources.settings_language_russian
import adbdeck.feature.settings.generated.resources.settings_language_system
import adbdeck.feature.settings.generated.resources.settings_language_title_bilingual
import adbdeck.feature.settings.generated.resources.settings_section_appearance
import adbdeck.feature.settings.generated.resources.settings_section_theme
import adbdeck.feature.settings.generated.resources.settings_theme_dark
import adbdeck.feature.settings.generated.resources.settings_theme_description
import adbdeck.feature.settings.generated.resources.settings_theme_light
import adbdeck.feature.settings.generated.resources.settings_theme_system
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.settings.SettingsComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

private data class AppearanceSectionUiState(
    val currentTheme: AppTheme,
    val currentLanguage: AppLanguage,
)

@Composable
internal fun AppearanceSectionHost(component: SettingsComponent) {
    val initial = remember(component) {
        AppearanceSectionUiState(
            currentTheme = component.state.value.currentTheme,
            currentLanguage = component.state.value.currentLanguage,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { AppearanceSectionUiState(currentTheme = it.currentTheme, currentLanguage = it.currentLanguage) }
            .distinctUntilChanged()
    }.collectAsState(initial = initial)

    AppearanceSection(
        state = uiState,
        onThemeChanged = component::onThemeChanged,
        onLanguageChanged = component::onLanguageChanged,
    )
}

@Composable
private fun AppearanceSection(
    state: AppearanceSectionUiState,
    onThemeChanged: (AppTheme) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_appearance)

    val languageTitle = stringResource(Res.string.settings_language_title_bilingual)
    val languageDesc = stringResource(Res.string.settings_language_description)
    val languageSystem = stringResource(Res.string.settings_language_system)
    val languageEn = stringResource(Res.string.settings_language_english)
    val languageRu = stringResource(Res.string.settings_language_russian)

    val themeTitle = stringResource(Res.string.settings_section_theme)
    val themeDesc = stringResource(Res.string.settings_theme_description)
    val themeSystem = stringResource(Res.string.settings_theme_system)
    val themeLight = stringResource(Res.string.settings_theme_light)
    val themeDark = stringResource(Res.string.settings_theme_dark)

    val languageOptions = remember(languageSystem, languageEn, languageRu) {
        listOf(
            AdbDropdownOption(value = AppLanguage.SYSTEM, label = languageSystem),
            AdbDropdownOption(value = AppLanguage.ENGLISH, label = languageEn),
            AdbDropdownOption(value = AppLanguage.RUSSIAN, label = languageRu),
        )
    }

    val themeOptions = remember(themeSystem, themeLight, themeDark) {
        listOf(
            AdbSegmentedOption(value = AppTheme.SYSTEM, label = themeSystem),
            AdbSegmentedOption(value = AppTheme.LIGHT, label = themeLight),
            AdbSegmentedOption(value = AppTheme.DARK, label = themeDark),
        )
    }

    val appearanceContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    val appearanceBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.primary,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = appearanceContainerColor,
        border = BorderStroke(1.dp, appearanceBorderColor),
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SectionSettingRow(
            title = languageTitle,
            description = languageDesc,
            modifier = Modifier.padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
            control = {
                AdbOutlinedDropdownTextField(
                    options = languageOptions,
                    selectedValue = state.currentLanguage,
                    onValueSelected = onLanguageChanged,
                    modifier = Modifier.width(160.dp),
                    size = AdbTextFieldSize.MEDIUM,
                )
            },
        )

        HorizontalDivider()

        SectionSettingRow(
            title = themeTitle,
            description = themeDesc,
            modifier = Modifier.padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
            control = {
                AdbSingleSegmentedButtons(
                    options = themeOptions,
                    selectedValue = state.currentTheme,
                    onValueSelected = onThemeChanged,
                    size = AdbSegmentedButtonSize.MEDIUM,
                )
            },
        )
    }
}

/**
 * Строка настройки внешнего вида: заголовок (жирный) + описание слева, контрол справа.
 */
@Composable
private fun SectionSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        control()
    }
}
