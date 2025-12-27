package com.adbdeck.feature.apkinstall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.feature.apkinstall.ApkInstallComponent
import com.adbdeck.feature.apkinstall.ApkInstallState
import com.adbdeck.feature.apkinstall.ApkInstallStatus
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
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

/**
 * Экран установки APK на активное устройство.
 */
@Composable
fun ApkInstallScreen(component: ApkInstallComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AdbBanner(
            message = state.deviceMessage,
            type = if (state.isDeviceReady) AdbBannerType.SUCCESS else AdbBannerType.WARNING,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )

        HorizontalDivider()

        ApkInstallBody(
            state = state,
            onApkPathChanged = component::onApkPathChanged,
            onInstallApk = component::onInstallApk,
            onClearLog = component::onClearLog,
        )

        state.feedback?.let {
            HorizontalDivider()
            AdbBanner(
                message = it.message,
                type = if (it.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ApkInstallBody(
    state: ApkInstallState,
    onApkPathChanged: (String) -> Unit,
    onInstallApk: () -> Unit,
    onClearLog: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "APK file",
                        style = MaterialTheme.typography.titleSmall,
                    )

                    OutlinedTextField(
                        value = state.apkPath,
                        onValueChange = onApkPathChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Путь к .apk") },
                        enabled = !state.isInstalling,
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val selected = showApkFileDialog(state.apkPath)
                                if (selected != null) {
                                    onApkPathChanged(selected)
                                }
                            },
                            enabled = !state.isInstalling,
                        ) {
                            Text("Выбрать APK…")
                        }

                        Button(
                            onClick = onInstallApk,
                            enabled = state.isDeviceReady && !state.isInstalling && state.apkPath.isNotBlank(),
                        ) {
                            Text("Install APK")
                        }
                    }
                }
            }

            ApkDropZone(
                enabled = state.isDeviceReady && !state.isInstalling,
                onApkDropped = onApkPathChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            OperationStatusCard(
                title = "Статус установки APK",
                status = state.status,
                isRunning = state.isInstalling,
            )

            state.lastInstalledApkPath?.let { path ->
                Text(
                    text = "Последний установленный APK: $path",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Лог установки",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onClearLog,
                        enabled = state.logLines.isNotEmpty() && !state.isInstalling,
                    ) {
                        Text("Очистить")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.logLines.isEmpty()) {
                            EmptyView(
                                message = "Здесь появится вывод adb install и детали ошибок",
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            state.logLines.forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationStatusCard(
    title: String,
    status: ApkInstallStatus,
    isRunning: Boolean,
) {
    val containerColor = if (status.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val contentColor = if (status.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )

            when {
                status.progress != null -> {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { status.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                isRunning -> {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkDropZone(
    enabled: Boolean,
    onApkDropped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnApkDropped by rememberUpdatedState(onApkDropped)
    val panelBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val panelBorderColor = MaterialTheme.colorScheme.outlineVariant
    val panelTextColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Drag & Drop",
                style = MaterialTheme.typography.titleSmall,
            )

            androidx.compose.ui.awt.SwingPanel(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                factory = {
                    JPanel(BorderLayout()).apply {
                        val label = JLabel("", SwingConstants.CENTER)
                        isOpaque = true
                        background = panelBackground.toAwtColor()
                        label.isOpaque = false
                        label.foreground = panelTextColor.toAwtColor()
                        add(label, BorderLayout.CENTER)
                        border = BorderFactory.createDashedBorder(
                            panelBorderColor.toAwtColor(),
                            1f,
                            6f,
                            3f,
                            true,
                        )

                        dropTarget = object : DropTarget() {
                            override fun dragEnter(event: DropTargetDragEvent) {
                                handleDrag(event)
                            }

                            override fun dragOver(event: DropTargetDragEvent) {
                                handleDrag(event)
                            }

                            override fun dropActionChanged(event: DropTargetDragEvent) {
                                handleDrag(event)
                            }

                            override fun drop(event: DropTargetDropEvent) {
                                val dropEnabled = getClientProperty("apk.drop.enabled") as? Boolean ?: false
                                if (!dropEnabled) {
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

                                @Suppress("UNCHECKED_CAST")
                                val callback = getClientProperty("apk.drop.callback") as? ((String) -> Unit)
                                callback?.invoke(droppedPath)
                                event.dropComplete(true)
                            }

                            private fun handleDrag(event: DropTargetDragEvent) {
                                val dropEnabled = getClientProperty("apk.drop.enabled") as? Boolean ?: false
                                if (!dropEnabled || !hasSupportedTransferFlavor(event.currentDataFlavors)) {
                                    event.rejectDrag()
                                    return
                                }
                                event.acceptDrag(DnDConstants.ACTION_COPY)
                            }
                        }
                    }
                },
                update = { panel ->
                    panel.putClientProperty("apk.drop.enabled", enabled)
                    panel.putClientProperty("apk.drop.callback", currentOnApkDropped)
                    panel.background = panelBackground.toAwtColor()
                    panel.border = BorderFactory.createDashedBorder(
                        panelBorderColor.toAwtColor(),
                        1f,
                        6f,
                        3f,
                        true,
                    )
                    (panel.components.firstOrNull() as? JLabel)?.text = if (enabled) {
                        "Перетащите сюда .apk файл"
                    } else {
                        "Выберите активное устройство для установки"
                    }
                    (panel.components.firstOrNull() as? JLabel)?.foreground = panelTextColor.toAwtColor()
                },
            )
        }
    }
}

/** Извлекает путь к первому .apk из drop-события. */
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
        .mapNotNull { line ->
            parseDroppedLineToFile(line)
        }
        .toList()
}

/** Возвращает поддерживаемый текстовый payload из dnd-transfer. */
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

/** Пытается распарсить строку из dnd как file-uri или как абсолютный путь. */
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

/** Проверяет, поддерживает ли transfer хотя бы один ожидаемый drag flavor. */
private fun hasSupportedTransferFlavor(transferable: Transferable): Boolean =
    hasSupportedTransferFlavor(transferable.transferDataFlavors)

/** Проверяет список dnd flavor-ов на наличие поддерживаемых. */
private fun hasSupportedTransferFlavor(flavors: Array<DataFlavor>): Boolean =
    flavors.any { flavor ->
        flavor == DataFlavor.javaFileListFlavor ||
            flavor == DataFlavor.stringFlavor ||
            (flavor.primaryType.equals("text", ignoreCase = true) &&
                flavor.subType.equals("uri-list", ignoreCase = true))
    }

/** Конвертирует Compose-цвет в AWT-цвет (для SwingPanel). */
private fun androidx.compose.ui.graphics.Color.toAwtColor(): AwtColor =
    AwtColor(
        (red * 255f).roundToInt().coerceIn(0, 255),
        (green * 255f).roundToInt().coerceIn(0, 255),
        (blue * 255f).roundToInt().coerceIn(0, 255),
        (alpha * 255f).roundToInt().coerceIn(0, 255),
    )

/** Диалог выбора APK-файла. */
private fun showApkFileDialog(initialPath: String): String? {
    val initialFile = File(initialPath).takeIf { it.exists() }
    val initialDir = when {
        initialFile == null -> File(System.getProperty("user.home"))
        initialFile.isDirectory -> initialFile
        else -> initialFile.parentFile ?: File(System.getProperty("user.home"))
    }

    val chooser = JFileChooser(initialDir).apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = false
        fileFilter = FileNameExtensionFilter("Android Package (*.apk)", "apk")
        dialogTitle = "Выберите APK-файл"
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
