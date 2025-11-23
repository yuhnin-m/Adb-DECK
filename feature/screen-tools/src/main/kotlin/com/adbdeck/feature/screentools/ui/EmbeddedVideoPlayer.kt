package com.adbdeck.feature.screentools.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import java.io.File
import javax.swing.JComponent

/**
 * Встроенный video player на базе JavaFX MediaView.
 */
@Composable
fun EmbeddedVideoPlayer(
    videoPath: String?,
    modifier: Modifier = Modifier,
    showPlaybackControls: Boolean = true,
    showStatus: Boolean = true,
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val playerResult = remember {
        runCatching {
            FxVideoPlayer { statusMessage = it }
        }
    }
    val player = playerResult.getOrNull()

    if (player == null) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Text(
                text = "Встроенный плеер недоступен в текущем окружении",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
        return
    }

    DisposableEffect(Unit) {
        onDispose { player.dispose() }
    }

    LaunchedEffect(videoPath) {
        player.load(videoPath)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            SwingPanel(
                factory = { player.component() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (showPlaybackControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { player.play() },
                    enabled = !videoPath.isNullOrBlank(),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }

                OutlinedButton(
                    onClick = { player.pause() },
                    enabled = !videoPath.isNullOrBlank(),
                ) {
                    Icon(Icons.Outlined.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pause")
                }

                OutlinedButton(
                    onClick = { player.stop() },
                    enabled = !videoPath.isNullOrBlank(),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            }
        }

        if (showStatus) {
            statusMessage?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/**
 * Локальный адаптер JavaFX-плеера для Compose.
 */
private class FxVideoPlayer(
    private val onStatus: (String?) -> Unit,
) {
    private val panel: JFXPanel = JFXPanel()
    private var mediaPlayer: MediaPlayer? = null

    init {
        Platform.runLater {
            panel.scene = Scene(StackPane().apply {
                style = "-fx-background-color: #111111;"
            })
        }
    }

    fun component(): JComponent = panel

    fun load(path: String?) {
        if (path.isNullOrBlank()) {
            onStatus("Видео не выбрано")
            Platform.runLater {
                releasePlayer()
                panel.scene = Scene(StackPane().apply { style = "-fx-background-color: #111111;" })
            }
            return
        }

        val file = File(path)
        if (!file.isFile) {
            onStatus("Файл видео не найден: $path")
            Platform.runLater {
                releasePlayer()
                panel.scene = Scene(StackPane().apply { style = "-fx-background-color: #111111;" })
            }
            return
        }

        Platform.runLater {
            releasePlayer()

            val media = Media(file.toURI().toString())
            val player = MediaPlayer(media)
            val mediaView = MediaView(player).apply {
                isPreserveRatio = true
                isSmooth = true
            }

            val root = StackPane(mediaView).apply {
                style = "-fx-background-color: #111111;"
            }
            val scene = Scene(root)

            // Подгоняем размер video-view под фактический размер контейнера без обрезки.
            mediaView.fitWidthProperty().bind(scene.widthProperty())
            mediaView.fitHeightProperty().bind(scene.heightProperty())

            player.setOnReady {
                val seconds = player.totalDuration?.toSeconds()?.toLong() ?: 0L
                onStatus("Видео загружено (${seconds}s)")
            }
            player.setOnError {
                onStatus(player.error?.message ?: "Ошибка MediaPlayer")
            }
            media.setOnError {
                onStatus(media.error?.message ?: "Ошибка Media")
            }

            panel.scene = scene
            mediaPlayer = player
        }
    }

    fun play() {
        Platform.runLater {
            mediaPlayer?.play()
            onStatus("Воспроизведение")
        }
    }

    fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
            onStatus("Пауза")
        }
    }

    fun stop() {
        Platform.runLater {
            mediaPlayer?.stop()
            onStatus("Остановлено")
        }
    }

    fun dispose() {
        Platform.runLater {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.dispose()
        mediaPlayer = null
    }
}
