package com.adbdeck.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Опция для сегмент-контрола.
 *
 * @param value Идентификатор/значение опции.
 * @param label Текст, отображаемый в сегменте.
 * @param enabled Доступность сегмента.
 * @param indicatorColor Опциональный цвет точки-индикатора слева от подписи.
 * @param contentDescription Текст для accessibility. Если `null`, используется [label].
 */
@Immutable
data class AdbSegmentedOption<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
    val indicatorColor: Color? = null,
    val contentDescription: String? = null,
)

/**
 * Универсальный single-choice segmented control.
 *
 * Используется там, где можно выбрать только один вариант:
 * режим отображения, активная вкладка фильтра и т.д.
 *
 * @param options Набор опций.
 * @param selectedValue Текущее выбранное значение.
 * @param onValueSelected Callback выбора опции.
 * @param modifier Modifier контейнера.
 * @param showSelectionIcon Показывать ли встроенную check-иконку выбранного сегмента.
 */
@Composable
fun <T> AdbSingleSegmentedButtons(
    options: List<AdbSegmentedOption<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    showSelectionIcon: Boolean = false,
) {
    if (options.isEmpty()) return

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val selected = option.value == selectedValue
            SegmentedButton(
                selected = selected,
                onClick = { onValueSelected(option.value) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                enabled = option.enabled,
                icon = if (showSelectionIcon) {
                    { SegmentedButtonDefaults.Icon(selected) }
                } else {
                    {}
                },
                modifier = Modifier.semantics {
                    contentDescription = option.contentDescription ?: option.label
                },
                label = {
                    SegmentLabel(option = option)
                },
            )
        }
    }
}

/**
 * Универсальный multi-choice segmented control.
 *
 * Используется там, где одновременно может быть выбрано несколько опций:
 * например флаги отображения.
 *
 * @param options Набор опций.
 * @param selectedValues Множество выбранных значений.
 * @param onValueToggle Callback изменения состояния сегмента.
 * @param modifier Modifier контейнера.
 * @param showSelectionIcon Показывать ли встроенную check-иконку выбранного сегмента.
 */
@Composable
fun <T> AdbMultiSegmentedButtons(
    options: List<AdbSegmentedOption<T>>,
    selectedValues: Set<T>,
    onValueToggle: (value: T, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showSelectionIcon: Boolean = false,
) {
    if (options.isEmpty()) return

    MultiChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val checked = option.value in selectedValues
            SegmentedButton(
                checked = checked,
                onCheckedChange = { isChecked -> onValueToggle(option.value, isChecked) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                enabled = option.enabled,
                icon = if (showSelectionIcon) {
                    { SegmentedButtonDefaults.Icon(checked) }
                } else {
                    {}
                },
                modifier = Modifier.semantics {
                    contentDescription = option.contentDescription ?: option.label
                },
                label = {
                    SegmentLabel(option = option)
                },
            )
        }
    }
}

@Composable
private fun <T> SegmentLabel(option: AdbSegmentedOption<T>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        option.indicatorColor?.let { color ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = color, shape = CircleShape),
            )
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SegmentedPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                AdbSingleSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption("compact", "Compact"),
                        AdbSegmentedOption("full", "Full"),
                    ),
                    selectedValue = "compact",
                    onValueSelected = {},
                )

                AdbSingleSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption("all", "All"),
                        AdbSegmentedOption("debug", "D", indicatorColor = Color(0xFF4CAF50)),
                        AdbSegmentedOption("info", "I", indicatorColor = Color(0xFF2196F3)),
                        AdbSegmentedOption("warn", "W", indicatorColor = Color(0xFFFF9800)),
                        AdbSegmentedOption("error", "E", indicatorColor = Color(0xFFF44336)),
                    ),
                    selectedValue = "warn",
                    onValueSelected = {},
                )

                AdbMultiSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption("date", "Date"),
                        AdbSegmentedOption("time", "Time"),
                        AdbSegmentedOption("ms", "ms"),
                        AdbSegmentedOption("colors", "Colors"),
                    ),
                    selectedValues = setOf("date", "colors"),
                    onValueToggle = { _, _ -> },
                )
            }
        }
    }
}

@Preview
@Composable
fun SegmentedLightPreview() {
    SegmentedPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
fun SegmentedDarkPreview() {
    SegmentedPreviewContent(isDarkTheme = true)
}
