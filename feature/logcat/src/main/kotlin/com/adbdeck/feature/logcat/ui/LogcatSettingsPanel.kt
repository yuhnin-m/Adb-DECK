package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatFontFamily
import com.adbdeck.feature.logcat.LogcatState

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
                text = "Настройки Logcat",
                style = MaterialTheme.typography.titleMedium,
            )

            LogcatSettingsSection(title = "Формат вывода") {
                Text(
                    text = "Формат логов",
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
                                label = "Compact",
                            ),
                            AdbSegmentedOption(
                                value = LogcatDisplayMode.FULL,
                                label = "Full",
                            ),
                        ),
                        selectedValue = state.displayMode,
                        onValueSelected = component::onDisplayModeChanged,
                        size = AdbSegmentedButtonSize.MEDIUM,
                    )
                }

                LabeledSwitchRow(
                    label = "Использовать цвета",
                    checked = state.coloredLevels,
                    onCheckedChange = component::onToggleColoredLevels,
                )
            }

            LogcatSettingsSection(title = "Шрифт") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AdbSingleSegmentedButtons(
                        options = listOf(
                            AdbSegmentedOption(
                                value = LogcatFontFamily.MONOSPACE,
                                label = "Mono",
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.SANS_SERIF,
                                label = "Sans",
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.SERIF,
                                label = "Serif",
                            ),
                            AdbSegmentedOption(
                                value = LogcatFontFamily.DEFAULT,
                                label = "System",
                            ),
                        ),
                        selectedValue = state.fontFamily,
                        onValueSelected = component::onFontFamilyChanged,
                        size = AdbSegmentedButtonSize.SMALL,
                    )
                }

                FontSizeSelector(
                    fontSizeSp = state.fontSizeSp,
                    onDecrease = { component.onFontSizeChanged(state.fontSizeSp - 1) },
                    onIncrease = { component.onFontSizeChanged(state.fontSizeSp + 1) },
                )
            }

            LogcatSettingsSection(title = "Временные метки") {
                LabeledSwitchRow(
                    label = "Отображать дату",
                    checked = state.showDate,
                    onCheckedChange = component::onToggleShowDate,
                )
                LabeledSwitchRow(
                    label = "Отображать время",
                    checked = state.showTime,
                    onCheckedChange = component::onToggleShowTime,
                )
                LabeledSwitchRow(
                    label = "Отображать ms",
                    checked = state.showMillis,
                    onCheckedChange = component::onToggleShowMillis,
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
    onCheckedChange: () -> Unit,
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
            onCheckedChange = { onCheckedChange() },
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
            text = "Размер шрифта",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onDecrease,
            enabled = fontSizeSp > 8,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Уменьшить шрифт",
                modifier = Modifier.size(18.dp),
            )
        }

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

        OutlinedButton(
            onClick = onIncrease,
            enabled = fontSizeSp < 24,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Увеличить шрифт",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
