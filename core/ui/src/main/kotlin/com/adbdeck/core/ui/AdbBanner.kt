package com.adbdeck.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Семантический тип баннера.
 */
enum class AdbBannerType {
    /** Информационное сообщение. */
    INFO,

    /** Успешное завершение действия. */
    SUCCESS,

    /** Предупреждение, не блокирующее работу. */
    WARNING,

    /** Ошибка или критическое состояние. */
    VARNING,
}

/**
 * Вариант элемента закрытия для баннера.
 */
enum class AdbBannerDismissStyle {
    /** Кнопка закрытия в виде иконки "X". */
    ICON,

    /** Текстовая кнопка закрытия ("ОК"). */
    TEXT,
}

private data class AdbBannerVisuals(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
)

/**
 * Унифицированный баннер приложения.
 *
 * Один компонент покрывает:
 * - обратную связь после действий (успех/ошибка),
 * - информационные сообщения,
 * - предупреждения.
 *
 * @param message Текст сообщения.
 * @param type Тип баннера. По умолчанию [AdbBannerType.INFO].
 * @param modifier Modifier внешнего контейнера.
 * @param onDismiss Callback закрытия. Если `null`, элемент закрытия не рисуется.
 * @param dismissStyle Вид элемента закрытия.
 * @param dismissText Подпись текстовой кнопки закрытия.
 */
@Composable
fun AdbBanner(
    message: String,
    type: AdbBannerType = AdbBannerType.INFO,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    dismissStyle: AdbBannerDismissStyle = AdbBannerDismissStyle.ICON,
    dismissText: String = "ОК",
) {
    val visuals = bannerVisuals(type = type)

    Surface(
        modifier = modifier,
        color = visuals.containerColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = null,
                tint = visuals.contentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = visuals.contentColor,
                modifier = Modifier.weight(1f),
            )

            if (onDismiss != null) {
                BannerDismissAction(
                    style = dismissStyle,
                    text = dismissText,
                    tint = visuals.contentColor,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun BannerDismissAction(
    style: AdbBannerDismissStyle,
    text: String,
    tint: Color,
    onClick: () -> Unit,
) {
    when (style) {
        AdbBannerDismissStyle.ICON -> {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Закрыть",
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        AdbBannerDismissStyle.TEXT -> {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .defaultMinSize(minWidth = 24.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun bannerVisuals(type: AdbBannerType): AdbBannerVisuals {
    return when (type) {
        AdbBannerType.INFO -> AdbBannerVisuals(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Outlined.Info,
        )

        AdbBannerType.SUCCESS -> AdbBannerVisuals(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Outlined.CheckCircle,
        )

        AdbBannerType.WARNING -> AdbBannerVisuals(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Outlined.WarningAmber,
        )

        AdbBannerType.VARNING -> AdbBannerVisuals(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Outlined.Error,
        )
    }
}

@Composable
private fun AdbBannerPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AdbBanner(
                    message = "Информация о подключении устройства",
                    type = AdbBannerType.INFO,
                    onDismiss = {},
                )
                AdbBanner(
                    message = "Операция успешно выполнена",
                    type = AdbBannerType.SUCCESS,
                    onDismiss = {},
                )
                AdbBanner(
                    message = "Низкий заряд устройства, возможны задержки",
                    type = AdbBannerType.WARNING,
                    onDismiss = {},
                    dismissStyle = AdbBannerDismissStyle.TEXT,
                )
                AdbBanner(
                    message = "Не удалось выполнить adb shell",
                    type = AdbBannerType.VARNING,
                    onDismiss = {},
                )
                AdbBanner(
                    message = "Баннер без dismiss-элемента",
                    type = AdbBannerType.INFO,
                )
            }
        }
    }
}

@Preview
@Composable
fun AdbBannerLightPreview() {
    AdbBannerPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
fun AdbBannerDarkPreview() {
    AdbBannerPreviewContent(isDarkTheme = true)
}
