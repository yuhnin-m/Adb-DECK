package com.adbdeck.feature.screentools.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.screentools.ScreenToolsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.util.Locale

internal val ScreenToolsControlsPanelWidth = 420.dp

/**
 * Универсальный блок выбора output-директории.
 */
@Composable
internal fun OutputDirectorySection(
    title: String,
    directory: String,
    pathLabel: String,
    chooseLabel: String,
    openFolderLabel: String,
    onChooseDirectory: () -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdbSectionCard(
        title = title,
        modifier = modifier,
    ) {
        Text(
            text = pathLabel,
            style = MaterialTheme.typography.labelSmall,
            color = AdbTheme.colorScheme.onSurfaceVariant,
        )
        AdbOutlinedTextField(
            value = directory,
            onValueChange = {},
            readOnly = true,
            size = AdbTextFieldSize.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbOutlinedButton(
                onClick = onChooseDirectory,
                text = chooseLabel,
                size = AdbButtonSize.MEDIUM,
                type = AdbButtonType.NEUTRAL,
            )
            AdbOutlinedButton(
                onClick = onOpenFolder,
                text = openFolderLabel,
                size = AdbButtonSize.MEDIUM,
                type = AdbButtonType.NEUTRAL,
            )
        }
    }
}

/**
 * Универсальный блок выбора quality preset.
 */
@Composable
internal fun <T> QualitySection(
    title: String,
    selectedValue: T,
    options: List<AdbDropdownOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    AdbSectionCard(
        title = title,
        modifier = modifier,
    ) {
        AdbOutlinedDropdownTextField(
            options = options,
            selectedValue = selectedValue,
            onValueSelected = onSelect,
            size = AdbTextFieldSize.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Универсальная карточка статуса длительной операции.
 */
@Composable
internal fun StatusSection(
    title: String,
    status: ScreenToolsStatus,
    fallbackMessage: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val resolvedMessage = status.message.ifBlank { fallbackMessage }
    val containerColor = if (status.isError) {
        AdbTheme.colorScheme.errorContainer
    } else {
        AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val contentColor = if (status.isError) {
        AdbTheme.colorScheme.onErrorContainer
    } else {
        AdbTheme.colorScheme.onSurface
    }

    AdbSectionCard(
        title = title,
        containerColor = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = resolvedMessage,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
        when {
            status.progress != null -> {
                LinearProgressIndicator(
                    progress = { status.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            isRunning -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Возвращает bitmap превью скриншота по пути файла.
 */
@Composable
internal fun rememberScreenshotBitmap(path: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            val file = path?.let(::File)
            if (file == null || !file.isFile) {
                null
            } else {
                runCatching {
                    SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

/**
 * Проверяет существование файла на хосте в IO-диспетчере.
 */
@Composable
internal fun rememberHostFileExists(path: String?): Boolean {
    val exists by produceState(initialValue = false, key1 = path) {
        value = withContext(Dispatchers.IO) {
            path?.let { File(it).isFile } == true
        }
    }
    return exists
}

/** Форматирует длительность записи как HH:MM:SS. */
internal fun formatElapsed(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs)
}

/**
 * Текст "последний файл" в стиле monospace с безопасным ellipsis.
 */
@Composable
internal fun LastFilePathLabel(
    pathText: String,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.bodySmall
    val monoStyle = remember(baseStyle) {
        baseStyle.copy(fontFamily = FontFamily.Monospace)
    }
    Text(
        text = pathText,
        style = monoStyle,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
