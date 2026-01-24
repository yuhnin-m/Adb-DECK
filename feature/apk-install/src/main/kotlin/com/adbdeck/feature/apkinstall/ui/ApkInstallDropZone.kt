package com.adbdeck.feature.apkinstall.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import java.awt.BorderLayout
import java.awt.Color as AwtColor
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.roundToInt

/**
 * Drop-зона для APK-файла.
 */
@Composable
internal fun ApkInstallDropZone(
    enabled: Boolean,
    enabledText: String,
    disabledText: String,
    onApkDropped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onApkDroppedState = rememberUpdatedState(onApkDropped)
    val panelBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f).toAwtColor()
    val panelBorderColor = MaterialTheme.colorScheme.outlineVariant.toAwtColor()
    val panelTextColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.toAwtColor()
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f).toAwtColor()
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            ApkDropTargetPanel().apply {
                onDropPath = { droppedPath ->
                    onApkDroppedState.value.invoke(droppedPath)
                }
            }
        },
        update = { panel ->
            panel.onDropPath = { droppedPath ->
                onApkDroppedState.value.invoke(droppedPath)
            }
            panel.render(
                enabled = enabled,
                enabledText = enabledText,
                disabledText = disabledText,
                backgroundColor = panelBackground,
                borderColor = panelBorderColor,
                textColor = panelTextColor,
            )
        },
    )
}

/**
 * JPanel-реализация drop-области APK.
 */
private class ApkDropTargetPanel : JPanel(BorderLayout()) {

    var onDropPath: ((String) -> Unit)? = null

    private val label = JLabel("", SwingConstants.CENTER)

    private var renderedEnabled: Boolean? = null
    private var renderedEnabledText: String = ""
    private var renderedDisabledText: String = ""
    private var renderedBackground: AwtColor? = null
    private var renderedTextColor: AwtColor? = null
    private var renderedBorderColor: AwtColor? = null

    init {
        isOpaque = true
        label.isOpaque = false
        add(label, BorderLayout.CENTER)

        dropTarget = object : DropTarget() {
            override fun dragEnter(event: DropTargetDragEvent) = handleDrag(event)
            override fun dragOver(event: DropTargetDragEvent) = handleDrag(event)
            override fun dropActionChanged(event: DropTargetDragEvent) = handleDrag(event)

            override fun drop(event: DropTargetDropEvent) {
                val enabledNow = renderedEnabled == true
                if (!enabledNow) {
                    event.rejectDrop()
                    return
                }

                val transferable = event.transferable
                if (transferable == null || !hasSupportedTransferFlavor(transferable)) {
                    event.rejectDrop()
                    return
                }

                event.acceptDrop(DnDConstants.ACTION_COPY)
                val droppedPath = extractDroppedApkPath(transferable)
                if (droppedPath == null) {
                    event.dropComplete(false)
                    return
                }

                onDropPath?.invoke(droppedPath)
                event.dropComplete(true)
            }

            private fun handleDrag(event: DropTargetDragEvent) {
                val enabledNow = renderedEnabled == true
                if (!enabledNow || !hasSupportedTransferFlavor(event.currentDataFlavors)) {
                    event.rejectDrag()
                    return
                }
                event.acceptDrag(DnDConstants.ACTION_COPY)
            }
        }
    }

    /**
     * Обновляет Swing-состояние только при реальном изменении параметров.
     *
     * Это снижает лишнюю работу в AWT при частых Compose-рекомпозициях.
     */
    fun render(
        enabled: Boolean,
        enabledText: String,
        disabledText: String,
        backgroundColor: AwtColor,
        borderColor: AwtColor,
        textColor: AwtColor,
    ) {
        if (renderedEnabled != enabled ||
            renderedEnabledText != enabledText ||
            renderedDisabledText != disabledText
        ) {
            renderedEnabled = enabled
            renderedEnabledText = enabledText
            renderedDisabledText = disabledText
            label.text = if (enabled) enabledText else disabledText
        }

        if (renderedBackground != backgroundColor) {
            renderedBackground = backgroundColor
            background = backgroundColor
        }

        if (renderedTextColor != textColor) {
            renderedTextColor = textColor
            label.foreground = textColor
        }

        if (renderedBorderColor != borderColor) {
            renderedBorderColor = borderColor
            border = BorderFactory.createDashedBorder(
                borderColor,
                1f,
                6f,
                3f,
                true,
            )
        }
    }
}

/** Извлекает путь к первому `.apk` из drop-transfer. */
private fun extractDroppedApkPath(transferable: Transferable): String? {
    val files = extractDroppedFiles(transferable)
    return files.firstOrNull { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }?.absolutePath
}

/** Извлекает файлы из drop-transfer (java file list + uri-list fallback). */
private fun extractDroppedFiles(transferable: Transferable): List<File> {
    val fromFileList = runCatching {
        if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            emptyList()
        } else {
            @Suppress("UNCHECKED_CAST")
            (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<Any?>)
                .orEmpty()
                .mapNotNull { it as? File }
        }
    }.getOrDefault(emptyList())
    if (fromFileList.isNotEmpty()) return fromFileList

    val rawUriList = readDroppedText(transferable)
    return rawUriList.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull(::parseDroppedLineToFile)
        .toList()
}

/** Возвращает поддерживаемый текстовый payload из DnD-transfer. */
private fun readDroppedText(transferable: Transferable): String {
    val uriFlavor = runCatching {
        DataFlavor("text/uri-list;class=java.lang.String")
    }.getOrNull()

    val fromUriList = uriFlavor?.let { flavor ->
        runCatching {
            if (transferable.isDataFlavorSupported(flavor)) {
                transferable.getTransferData(flavor) as? String
            } else {
                null
            }
        }.getOrNull()
    }
    if (!fromUriList.isNullOrBlank()) return fromUriList

    return runCatching {
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    }.getOrNull().orEmpty()
}

/** Пытается распарсить строку из drop-transfer как file-uri или абсолютный путь. */
private fun parseDroppedLineToFile(line: String): File? {
    val byUri = runCatching { URI(line) }.getOrNull()
    if (byUri != null && byUri.scheme?.equals("file", ignoreCase = true) == true) {
        return runCatching { File(byUri) }.getOrNull()
    }

    val plainPath = line.removePrefix("file://").trim()
    if (plainPath.isEmpty()) return null
    val asFile = File(plainPath)
    return asFile.takeIf { it.isAbsolute }
}

/** Проверяет, поддерживает ли transfer хотя бы один ожидаемый drop flavor. */
private fun hasSupportedTransferFlavor(transferable: Transferable): Boolean =
    hasSupportedTransferFlavor(transferable.transferDataFlavors)

/** Проверяет список DnD flavor-ов на наличие поддерживаемых. */
private fun hasSupportedTransferFlavor(flavors: Array<DataFlavor>): Boolean =
    flavors.any { flavor ->
        flavor == DataFlavor.javaFileListFlavor ||
            flavor == DataFlavor.stringFlavor ||
            (flavor.primaryType.equals("text", ignoreCase = true) &&
                flavor.subType.equals("uri-list", ignoreCase = true))
    }

/** Конвертирует Compose-цвет в AWT-цвет (для SwingPanel). */
private fun Color.toAwtColor(): AwtColor =
    AwtColor(
        (red * 255f).roundToInt().coerceIn(0, 255),
        (green * 255f).roundToInt().coerceIn(0, 255),
        (blue * 255f).roundToInt().coerceIn(0, 255),
        (alpha * 255f).roundToInt().coerceIn(0, 255),
    )
