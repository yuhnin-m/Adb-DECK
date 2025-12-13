package com.adbdeck.core.ui.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbDeckBlue
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.AdbDeckRed

/**
 * Универсальная кнопка с заливкой.
 *
 * Имеет единый API с [AdbOutlinedButton] и [AdbPlainButton], чтобы можно было
 * переключать стиль без изменения остального кода.
 *
 * @param onClick Callback клика.
 * @param text Текст кнопки. Если `null`, кнопка может быть icon-only.
 * @param modifier Modifier контейнера.
 * @param type Цветовой тип (`NEUTRAL/DANGER/SUCCESS`) с фиксированной палитрой.
 * @param size Размер кнопки.
 * @param cornerRadius Радиус скругления.
 * @param enabled Доступность кнопки.
 * @param loading Показывать встроенный loader. При `true` клик блокируется.
 * @param leadingIcon Иконка слева от текста.
 * @param trailingIcon Иконка справа от текста.
 * @param contentDescription Текст для accessibility (актуально для icon-only).
 * @param fullWidth Растягивать кнопку на всю ширину контейнера.
 */
@Composable
fun AdbFilledButton(
    onClick: () -> Unit,
    text: String? = null,
    modifier: Modifier = Modifier,
    type: AdbButtonType = AdbButtonType.NEUTRAL,
    size: AdbButtonSize = AdbButtonSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentDescription: String? = null,
    fullWidth: Boolean = false,
) {
    AdbBaseButton(
        style = AdbButtonStyle.FILLED,
        onClick = onClick,
        text = text,
        modifier = modifier,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentDescription = contentDescription,
        fullWidth = fullWidth,
    )
}

/**
 * Универсальная кнопка с обводкой.
 *
 * API идентичен [AdbFilledButton] и [AdbPlainButton].
 */
@Composable
fun AdbOutlinedButton(
    onClick: () -> Unit,
    text: String? = null,
    modifier: Modifier = Modifier,
    type: AdbButtonType = AdbButtonType.NEUTRAL,
    size: AdbButtonSize = AdbButtonSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentDescription: String? = null,
    fullWidth: Boolean = false,
) {
    AdbBaseButton(
        style = AdbButtonStyle.OUTLINED,
        onClick = onClick,
        text = text,
        modifier = modifier,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentDescription = contentDescription,
        fullWidth = fullWidth,
    )
}

/**
 * Универсальная «плоская» кнопка без заливки и обводки.
 *
 * API идентичен [AdbFilledButton] и [AdbOutlinedButton].
 */
@Composable
fun AdbPlainButton(
    onClick: () -> Unit,
    text: String? = null,
    modifier: Modifier = Modifier,
    type: AdbButtonType = AdbButtonType.NEUTRAL,
    size: AdbButtonSize = AdbButtonSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentDescription: String? = null,
    fullWidth: Boolean = false,
) {
    AdbBaseButton(
        style = AdbButtonStyle.PLAIN,
        onClick = onClick,
        text = text,
        modifier = modifier,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentDescription = contentDescription,
        fullWidth = fullWidth,
    )
}

@Composable
private fun AdbBaseButton(
    style: AdbButtonStyle,
    onClick: () -> Unit,
    text: String?,
    modifier: Modifier,
    type: AdbButtonType,
    size: AdbButtonSize,
    cornerRadius: AdbCornerRadius,
    enabled: Boolean,
    loading: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    contentDescription: String?,
    fullWidth: Boolean,
) {
    val resolvedColors = resolveButtonColors(style = style, type = type)
    val isClickable = enabled && !loading
    val hasText = !text.isNullOrBlank()
    val isIconOnly = !hasText

    val containerColor = if (enabled) resolvedColors.containerColor else resolvedColors.disabledContainerColor
    val contentColor = if (enabled) resolvedColors.contentColor else resolvedColors.disabledContentColor
    val borderColor = if (enabled) resolvedColors.borderColor else resolvedColors.disabledBorderColor
    val border = if (borderColor.alpha > 0f) BorderStroke(1.dp, borderColor) else null

    val widthModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    val contentWidthModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    val minWidth = if (isIconOnly) size.height else size.minWidth
    val horizontalPadding = if (isIconOnly) 0.dp else size.horizontalPadding

    val semanticsModifier = if (contentDescription.isNullOrBlank()) {
        Modifier
    } else {
        Modifier.semantics { this.contentDescription = contentDescription }
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius.value),
        color = containerColor,
        contentColor = contentColor,
        border = border,
        modifier = modifier
            .then(widthModifier)
            .defaultMinSize(minWidth = minWidth, minHeight = size.height)
            .height(size.height)
            .clip(RoundedCornerShape(cornerRadius.value))
            .clickable(enabled = isClickable, onClick = onClick)
            .then(semanticsModifier),
    ) {
        Row(
            modifier = contentWidthModifier
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        color = resolvedColors.loaderColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(size.loaderSize),
                    )
                    if (hasText) {
                        Box(modifier = Modifier.size(size.contentSpacing))
                        Text(
                            text = text.orEmpty(),
                            style = buttonTextStyle(size),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                else -> {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(size.iconSize),
                        )
                    }

                    if (hasText) {
                        if (leadingIcon != null) {
                            Box(modifier = Modifier.size(size.contentSpacing))
                        }
                        Text(
                            text = text.orEmpty(),
                            style = buttonTextStyle(size),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (trailingIcon != null) {
                        if (hasText) {
                            Box(modifier = Modifier.size(size.contentSpacing))
                        }
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(size.iconSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveButtonColors(
    style: AdbButtonStyle,
    type: AdbButtonType,
): AdbButtonResolvedColors {
    val baseColor = when (type) {
        AdbButtonType.NEUTRAL -> AdbDeckBlue
        AdbButtonType.DANGER -> AdbDeckRed
        AdbButtonType.SUCCESS -> AdbDeckGreen
    }

    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val disabledContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    return when (style) {
        AdbButtonStyle.FILLED -> AdbButtonResolvedColors(
            containerColor = baseColor,
            contentColor = Color.White,
            borderColor = Color.Transparent,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
            disabledBorderColor = Color.Transparent,
            loaderColor = Color.White,
        )

        AdbButtonStyle.OUTLINED -> AdbButtonResolvedColors(
            containerColor = Color.Transparent,
            contentColor = baseColor,
            borderColor = baseColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContent,
            disabledBorderColor = disabledBorder,
            loaderColor = baseColor,
        )

        AdbButtonStyle.PLAIN -> AdbButtonResolvedColors(
            containerColor = Color.Transparent,
            contentColor = baseColor,
            borderColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContent,
            disabledBorderColor = Color.Transparent,
            loaderColor = baseColor,
        )
    }
}

@Composable
private fun buttonTextStyle(size: AdbButtonSize): TextStyle {
    return when (size) {
        AdbButtonSize.LARGE -> MaterialTheme.typography.labelLarge
        AdbButtonSize.MEDIUM -> MaterialTheme.typography.labelMedium
        AdbButtonSize.SMALL -> MaterialTheme.typography.labelSmall
        AdbButtonSize.XSMALL -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    }
}
