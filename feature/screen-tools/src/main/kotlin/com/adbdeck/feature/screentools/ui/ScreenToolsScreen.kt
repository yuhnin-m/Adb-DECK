package com.adbdeck.feature.screentools.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.feature.screentools.ScreenToolsComponent
import com.adbdeck.feature.screentools.ScreenToolsState
import com.adbdeck.feature.screentools.ScreenToolsStatus
import com.adbdeck.feature.screentools.ScreenToolsTab
import com.adbdeck.feature.screentools.ScreenshotQualityPreset
import com.adbdeck.feature.screentools.ScreenshotSectionState
import com.adbdeck.feature.screentools.ScreenrecordQualityPreset
import com.adbdeck.feature.screentools.ScreenrecordSectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import javax.swing.JFileChooser

/**
 * Экран feature Screen Tools.
 */
@Composable
fun ScreenToolsScreen(component: ScreenToolsComponent) {
    val state by component.state.collectAsState()

    // Прогреваем JavaFX заранее, чтобы не ловить фриз при первом открытии вкладки Screenrecord.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            JavaFxBootstrap.prewarm()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            Tab(
                selected = state.selectedTab == ScreenToolsTab.SCREENSHOT,
                onClick = { component.onSelectTab(ScreenToolsTab.SCREENSHOT) },
                text = { Text("Screenshot") },
            )
            Tab(
                selected = state.selectedTab == ScreenToolsTab.SCREENRECORD,
                onClick = { component.onSelectTab(ScreenToolsTab.SCREENRECORD) },
                text = { Text("Screenrecord") },
            )
        }

        AdbBanner(
            message = state.deviceMessage,
            type = if (state.isDeviceReady) AdbBannerType.SUCCESS else AdbBannerType.WARNING,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )

        HorizontalDivider()

        when (state.selectedTab) {
            ScreenToolsTab.SCREENSHOT -> ScreenshotTab(
                state = state.screenshot,
                isDeviceReady = state.isDeviceReady,
                onOutputDirectoryChanged = component::onScreenshotOutputDirectoryChanged,
                onQualityChanged = component::onScreenshotQualityChanged,
                onTakeScreenshot = component::onTakeScreenshot,
                onCopyToClipboard = component::onCopyLastScreenshotToClipboard,
                onOpenFile = component::onOpenLastScreenshotFile,
                onOpenFolder = component::onOpenScreenshotFolder,
            )

            ScreenToolsTab.SCREENRECORD -> ScreenrecordTab(
                state = state.screenrecord,
                isDeviceReady = state.isDeviceReady,
                onOutputDirectoryChanged = component::onScreenrecordOutputDirectoryChanged,
                onQualityChanged = component::onScreenrecordQualityChanged,
                onStartRecording = component::onStartRecording,
                onStopRecording = component::onStopRecording,
                onOpenFile = component::onOpenLastVideoFile,
                onOpenFolder = component::onOpenVideoFolder,
            )
        }

        state.feedback?.let {
            HorizontalDivider()
            AdbBanner(
                message = it.message,
                type = if (it.isError) AdbBannerType.VARNING else AdbBannerType.SUCCESS,
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
private fun ScreenshotTab(
    state: ScreenshotSectionState,
    isDeviceReady: Boolean,
    onOutputDirectoryChanged: (String) -> Unit,
    onQualityChanged: (ScreenshotQualityPreset) -> Unit,
    onTakeScreenshot: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    val previewBitmap = rememberScreenshotBitmap(state.lastFilePath)
    val canUseResult = state.lastFilePath?.let { File(it).isFile } == true

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutputDirectorySection(
                title = "Screenshot output directory",
                directory = state.outputDirectory,
                onDirectoryChanged = onOutputDirectoryChanged,
                onOpenFolder = onOpenFolder,
            )

            QualitySection(
                title = "Качество screenshot",
                selectedLabel = state.quality.title,
                options = ScreenshotQualityPreset.entries,
                optionLabel = { it.title },
                onSelect = onQualityChanged,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onTakeScreenshot,
                    enabled = isDeviceReady && !state.isCapturing,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Take Screenshot")
                }

                OutlinedButton(
                    onClick = onCopyToClipboard,
                    enabled = canUseResult,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }

                OutlinedButton(
                    onClick = onOpenFile,
                    enabled = canUseResult,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open File")
                }

                OutlinedButton(
                    onClick = onOpenFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Folder")
                }
            }

            OperationStatusCard(
                title = "Статус screenshot",
                status = state.status,
                isRunning = state.isCapturing,
            )

            state.lastFilePath?.let { path ->
                Text(
                    text = "Последний файл: $path",
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
            Box(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Последний скриншот",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    EmptyView(
                        message = "Здесь появится preview последнего скриншота",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenrecordTab(
    state: ScreenrecordSectionState,
    isDeviceReady: Boolean,
    onOutputDirectoryChanged: (String) -> Unit,
    onQualityChanged: (ScreenrecordQualityPreset) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    val canOpenVideo = state.lastFilePath?.let { File(it).isFile } == true

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutputDirectorySection(
                title = "Screenrecord output directory",
                directory = state.outputDirectory,
                onDirectoryChanged = onOutputDirectoryChanged,
                onOpenFolder = onOpenFolder,
            )

            QualitySection(
                title = "Качество видеозаписи",
                selectedLabel = state.quality.title,
                options = ScreenrecordQualityPreset.entries,
                optionLabel = { "${it.title} • ${it.bitRateMbps} Mbps" },
                onSelect = onQualityChanged,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onStartRecording,
                    enabled = isDeviceReady && !state.isRecording && !state.isStarting && !state.isStopping,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Recording")
                }

                OutlinedButton(
                    onClick = onStopRecording,
                    enabled = state.isRecording && !state.isStopping,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Recording")
                }

                OutlinedButton(
                    onClick = onOpenFile,
                    enabled = canOpenVideo,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open File")
                }

                OutlinedButton(
                    onClick = onOpenFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Folder")
                }
            }

            if (state.isRecording || state.isStopping) {
                Text(
                    text = "Время записи: ${formatElapsed(state.elapsedSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OperationStatusCard(
                title = "Статус screenrecord",
                status = state.status,
                isRunning = state.isStarting || state.isRecording || state.isStopping,
            )

            state.lastFilePath?.let { path ->
                Text(
                    text = "Последний файл: $path",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        EmbeddedVideoPlayer(
            videoPath = state.lastFilePath,
            modifier = Modifier.weight(1f).fillMaxSize(),
            showPlaybackControls = true,
            showStatus = true,
        )
    }
}

@Composable
private fun <T> QualitySection(
    title: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                ) {
                    Text(selectedLabel)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(optionLabel(option)) },
                            onClick = {
                                expanded = false
                                onSelect(option)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputDirectorySection(
    title: String,
    directory: String,
    onDirectoryChanged: (String) -> Unit,
    onOpenFolder: () -> Unit,
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )

            OutlinedTextField(
                value = directory,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Путь") },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val selected = showDirectoryDialog(directory)
                        if (selected != null) {
                            onDirectoryChanged(selected)
                        }
                    },
                ) {
                    Text("Выбрать…")
                }

                OutlinedButton(onClick = onOpenFolder) {
                    Text("Открыть папку")
                }
            }
        }
    }
}

@Composable
private fun OperationStatusCard(
    title: String,
    status: ScreenToolsStatus,
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
}

@Composable
private fun rememberScreenshotBitmap(path: String?): ImageBitmap? {
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

/** Форматирует длительность записи как HH:MM:SS. */
private fun formatElapsed(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return "%02d:%02d:%02d".format(hours, minutes, secs)
}

/**
 * Диалог выбора директории.
 */
private fun showDirectoryDialog(initialPath: String): String? {
    val chooser = JFileChooser(File(initialPath).takeIf { it.exists() } ?: File(System.getProperty("user.home"))).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        dialogTitle = "Выберите директорию"
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
