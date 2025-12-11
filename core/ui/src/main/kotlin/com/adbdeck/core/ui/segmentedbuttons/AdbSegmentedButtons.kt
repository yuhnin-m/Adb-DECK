package com.adbdeck.core.ui.segmentedbuttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Универсальный single-choice segmented-контрол.
 *
 * Используется там, где может быть выбран только один вариант
 * (например, `Compact/Full`).
 *
 * @param options Набор опций.
 * @param selectedValue Текущее выбранное значение.
 * @param onValueSelected Callback выбора опции.
 * @param modifier Modifier контейнера.
 * @param size Размер сегментов.
 * @param colors Цветовая схема компонента.
 */
@Composable
fun <T> AdbSingleSegmentedButtons(
    options: List<AdbSegmentedOption<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    size: AdbSegmentedButtonSize = AdbSegmentedButtonSize.MEDIUM,
    colors: AdbSegmentedButtonColors = AdbSegmentedButtonDefaults.colors(),
) {
    if (options.isEmpty()) return

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            SegmentedItem(
                label = option.label,
                selected = option.value == selectedValue,
                enabled = option.enabled,
                indicatorColor = option.indicatorColor,
                contentDescription = option.contentDescription ?: option.label,
                onClick = { onValueSelected(option.value) },
                shape = segmentShape(index = index, count = options.size),
                size = size,
                colors = colors,
            )
        }
    }
}

/**
 * Универсальный multi-choice segmented-контрол.
 *
 * Используется там, где одновременно может быть выбрано несколько опций
 * (например, `Date/Time/ms/Colors`).
 *
 * @param options Набор опций.
 * @param selectedValues Множество выбранных значений.
 * @param onValueToggle Callback изменения состояния сегмента.
 * @param modifier Modifier контейнера.
 * @param size Размер сегментов.
 * @param colors Цветовая схема компонента.
 */
@Composable
fun <T> AdbMultiSegmentedButtons(
    options: List<AdbSegmentedOption<T>>,
    selectedValues: Set<T>,
    onValueToggle: (value: T, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: AdbSegmentedButtonSize = AdbSegmentedButtonSize.MEDIUM,
    colors: AdbSegmentedButtonColors = AdbSegmentedButtonDefaults.colors(),
) {
    if (options.isEmpty()) return

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = option.value in selectedValues
            SegmentedItem(
                label = option.label,
                selected = selected,
                enabled = option.enabled,
                indicatorColor = option.indicatorColor,
                contentDescription = option.contentDescription ?: option.label,
                onClick = { onValueToggle(option.value, !selected) },
                shape = segmentShape(index = index, count = options.size),
                size = size,
                colors = colors,
            )
        }
    }
}

@Composable
private fun SegmentedItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    indicatorColor: androidx.compose.ui.graphics.Color?,
    contentDescription: String,
    onClick: () -> Unit,
    shape: Shape,
    size: AdbSegmentedButtonSize,
    colors: AdbSegmentedButtonColors,
) {
    val containerColor = if (selected) colors.activeContainerColor else colors.inactiveContainerColor
    val contentColor = when {
        !enabled -> colors.disabledContentColor
        selected -> colors.activeContentColor
        else -> colors.inactiveContentColor
    }

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor),
        modifier = Modifier
            .height(size.height)
            .defaultMinSize(minWidth = size.minWidth)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = size.horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                indicatorColor?.let { color ->
                    Box(
                        modifier = Modifier
                            .size(size.indicatorSize)
                            .background(color = color, shape = CircleShape),
                    )
                }

                Text(
                    text = label,
                    style = segmentedTextStyle(size = size),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun segmentedTextStyle(size: AdbSegmentedButtonSize): TextStyle {
    return when (size) {
        AdbSegmentedButtonSize.LARGE -> MaterialTheme.typography.labelLarge
        AdbSegmentedButtonSize.MEDIUM -> MaterialTheme.typography.labelMedium
        AdbSegmentedButtonSize.SMALL -> MaterialTheme.typography.labelSmall
        AdbSegmentedButtonSize.XSMALL -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    }
}

private fun segmentShape(index: Int, count: Int): Shape {
    val radius = 8.dp
    if (count <= 1) return RoundedCornerShape(radius)
    return when (index) {
        0 -> RoundedCornerShape(topStart = radius, bottomStart = radius)
        count - 1 -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
        else -> RectangleShape
    }
}

@Composable
private fun SegmentedPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    size = AdbSegmentedButtonSize.LARGE,
                )

                AdbSingleSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption("all", "All"),
                        AdbSegmentedOption("d", "D", indicatorColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                        AdbSegmentedOption("i", "I", indicatorColor = androidx.compose.ui.graphics.Color(0xFF2196F3)),
                        AdbSegmentedOption("w", "W", indicatorColor = androidx.compose.ui.graphics.Color(0xFFFF9800)),
                        AdbSegmentedOption("e", "E", indicatorColor = androidx.compose.ui.graphics.Color(0xFFF44336)),
                    ),
                    selectedValue = "w",
                    onValueSelected = {},
                    size = AdbSegmentedButtonSize.XSMALL,
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
                    size = AdbSegmentedButtonSize.MEDIUM,
                )

                AdbSingleSegmentedButtons(
                    options = listOf(
                        AdbSegmentedOption("l", "Large"),
                        AdbSegmentedOption("m", "Medium"),
                        AdbSegmentedOption("s", "Small"),
                        AdbSegmentedOption("x", "XSmall"),
                    ),
                    selectedValue = "s",
                    onValueSelected = {},
                    size = AdbSegmentedButtonSize.SMALL,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SegmentedLightPreview() {
    SegmentedPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun SegmentedDarkPreview() {
    SegmentedPreviewContent(isDarkTheme = true)
}
