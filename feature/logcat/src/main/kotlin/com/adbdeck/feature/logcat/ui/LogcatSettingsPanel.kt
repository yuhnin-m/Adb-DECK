package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import adbdeck.feature.logcat.generated.resources.Res
import adbdeck.feature.logcat.generated.resources.*
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatFontFamily
import com.adbdeck.feature.logcat.LogcatState
import org.jetbrains.compose.resources.stringResource

/**
 * Дочерняя панель настроек Logcat.
 *
 * Секции:
 * - формат вывода;
 * - шрифт;
 * - временные метки.
 */
@Composable
internal fun LogcatSettingsPanel(
    state: LogcatState,
    component: LogcatComponent,
    modifier: Modifier = Modifier,
) {
    val blockSpacing = Dimensions.paddingMedium

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(blockSpacing),
        ) {
            Text(
                text = stringResource(Res.string.logcat_settings_title),
                style = MaterialTheme.typography.titleMedium,
            )

            LogcatSettingsSection(
                title = stringResource(Res.string.logcat_settings_output_section_title),
            ) {
                Text(
                    text = stringResource(Res.string.logcat_settings_output_format_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AdbSingleSegmentedButtons(
                        options = listOf(
                            AdbSegmentedOption(
                                value = LogcatDisplayMode.COMPACT,
                                label = stringResource(Res.string.logcat_settings_mode_compact),
                            ),
                            AdbSegmentedOption(
                                value = LogcatDisplayMode.FULL,
                                label = stringResource(Res.string.logcat_settings_mode_full),
                            ),
                        ),
                        selectedValue = state.displayMode,
                        onValueSelected = component::onDisplayModeChanged,
                        size = AdbSegmentedButtonSize.MEDIUM,
                    )
                }

                LabeledSwitchRow(
                    label = stringResource(Res.string.logcat_settings_use_colors_label),
                    checked = state.coloredLevels,
                    onCheckedChange = { component.onToggleColoredLevels() },
                )
                LabeledSwitchRow(
                    label = stringResource(Res.string.logcat_settings_smooth_stream_label),
                    checked = state.smoothStreamAnimation,
                    onCheckedChange = component::onSmoothStreamAnimationChanged,
                )
            }

            LogcatSettingsSection(
                title = stringResource(Res.string.logcat_settings_font_section_title),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AdbSingleSegmentedButtons(
                        options = listOf(
                            AdbSegmentedOption(
                                value = LogcatFontFamily.MONOSPACE,
                                label = stringResource(Res.string.logcat_settings_font_mono),
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.SANS_SERIF,
                                label = stringResource(Res.string.logcat_settings_font_sans),
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.SERIF,
                                label = stringResource(Res.string.logcat_settings_font_serif),
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.DEFAULT,
                                label = stringResource(Res.string.logcat_settings_font_system),
                            ),
                        ),
                        selectedValue = state.fontFamily,
                        onValueSelected = component::onFontFamilyChanged,
                        size = AdbSegmentedButtonSize.MEDIUM,
                    )
                }

                FontSizeSelector(
                    fontSizeSp = state.fontSizeSp,
                    onDecrease = { component.onFontSizeChanged(state.fontSizeSp - 1) },
                    onIncrease = { component.onFontSizeChanged(state.fontSizeSp + 1) },
                )
            }

            LogcatSettingsSection(
                title = stringResource(Res.string.logcat_settings_timestamps_section_title),
            ) {
                LabeledSwitchRow(
                    label = stringResource(Res.string.logcat_settings_show_date_label),
                    checked = state.showDate,
                    onCheckedChange = { component.onToggleShowDate() },
                )
                LabeledSwitchRow(
                    label = stringResource(Res.string.logcat_settings_show_time_label),
                    checked = state.showTime,
                    onCheckedChange = { component.onToggleShowTime() },
                )
                LabeledSwitchRow(
                    label = stringResource(Res.string.logcat_settings_show_millis_label),
                    checked = state.showMillis,
                    onCheckedChange = { component.onToggleShowMillis() },
                )
            }
        }
    }
}

/**
 * Блок секции в панели настроек Logcat.
 */
@Composable
private fun LogcatSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
                content = content,
            )
        }
    }
}

/**
 * Строка с подписью и переключателем.
 */
@Composable
private fun LabeledSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Контрол выбора размера шрифта с явными кнопками `+/-`.
 */
@Composable
private fun FontSizeSelector(
    fontSizeSp: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = stringResource(Res.string.logcat_settings_font_size_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))

        AdbOutlinedButton(
            onClick = onDecrease,
            type = AdbButtonType.NEUTRAL,
            size = AdbButtonSize.LARGE,
            cornerRadius = AdbCornerRadius.MEDIUM,
            enabled = fontSizeSp > 8,
            leadingIcon = Icons.Outlined.Remove,
            contentDescription = stringResource(Res.string.logcat_settings_font_decrease_content_desc),
            modifier = Modifier.size(40.dp),
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = fontSizeSp.toString(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        AdbOutlinedButton(
            onClick = onIncrease,
            type = AdbButtonType.NEUTRAL,
            size = AdbButtonSize.LARGE,
            cornerRadius = AdbCornerRadius.MEDIUM,
            enabled = fontSizeSp < 24,
            leadingIcon = Icons.Outlined.Add,
            contentDescription = stringResource(Res.string.logcat_settings_font_increase_content_desc),
            modifier = Modifier.size(40.dp),
        )
    }
}
