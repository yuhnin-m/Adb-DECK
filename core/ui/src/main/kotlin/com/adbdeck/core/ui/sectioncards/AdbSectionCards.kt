package com.adbdeck.core.ui.sectioncards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Универсальная секционная карточка с заголовком и произвольным слотом контента.
 *
 * Компонент нужен для единообразного оформления блоков настроек/информации:
 * - заголовок (обязательный);
 * - подзаголовок (опционально);
 * - зона действий в правой части заголовка (опционально);
 * - контейнер для любого composable-контента.
 *
 * @param title Текст заголовка секции.
 * @param modifier Внешний [Modifier].
 * @param subtitle Дополнительный текст под заголовком.
 * @param titleUppercase Приводить заголовок к upper-case.
 * @param titleColor Цвет заголовка.
 * @param titleTextStyle Стиль заголовка секции.
 * @param subtitleTextStyle Стиль подзаголовка секции.
 * @param containerColor Цвет контейнера секции.
 * @param border Граница контейнера.
 * @param shape Форма контейнера. По умолчанию берется из темы.
 * @param tonalElevation Тоновое поднятие контейнера.
 * @param contentPadding Внутренние отступы контейнера.
 * @param contentSpacing Вертикальный шаг между элементами внутри секции.
 * @param headerTrailing Дополнительный контент справа от заголовка (например, кнопки).
 * @param content Слот произвольного содержимого секции.
 */
@Composable
fun AdbSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleUppercase: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    titleTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    subtitleTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    shape: Shape = MaterialTheme.shapes.small,
    tonalElevation: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimensions.paddingMedium,
        vertical = Dimensions.paddingSmall,
    ),
    contentSpacing: Dp = Dimensions.paddingXSmall,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedTitle = if (titleUppercase) title.uppercase() else title

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolvedTitle,
                    style = titleTextStyle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = subtitleTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (headerTrailing != null) {
                Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
                headerTrailing()
            }
        }

        Surface(
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
                content = content,
            )
        }
    }
}

@Preview
@Composable
private fun AdbSectionCardLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingDefault),
        ) {
            AdbSectionCard(
                title = "Подключение",
                subtitle = "Базовые параметры устройства",
                titleUppercase = true,
            ) {
                Text(text = "Device ID: emulator-5554")
                Text(text = "Состояние: device")
            }
        }
    }
}

@Preview
@Composable
private fun AdbSectionCardDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingDefault),
        ) {
            AdbSectionCard(
                title = "Опасные действия",
                subtitle = "Операции, требующие подтверждения",
                titleColor = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                ),
            ) {
                Text(text = "Recovery Mode")
                Text(text = "Bootloader / Fastboot")
            }
        }
    }
}
