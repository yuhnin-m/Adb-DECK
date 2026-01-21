package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_action_pause
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_action_play
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_action_stop
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_error_media
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_error_player
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_file_not_found
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_loaded
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_no_video
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_paused
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_playing
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_status_stopped
import adbdeck.feature.screen_tools.generated.resources.screen_tools_player_unavailable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.Locale
import javax.swing.JComponent
import javax.swing.SwingUtilities

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
    val strings = rememberPlayerStatusStrings()

    val playerResult = remember(strings) {
        runCatching {
            FxVideoPlayer(
                strings = strings,
                onStatus = { statusMessage = it },
            )
        }
    }
    val player = playerResult.getOrNull()

    if (player == null) {
        Surface(
            modifier = modifier,
            color = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = stringResource(Res.string.screen_tools_player_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimensions.paddingMedium),
            )
        }
        return
    }

    DisposableEffect(player) {
        onDispose { player.dispose() }
    }

    // Повторно загружаем файл и для нового инстанса плеера,
    // чтобы после переинициализации не остаться с пустой сценой.
    LaunchedEffect(player, videoPath) {
        player.load(videoPath)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.small,
        ) {
            SwingPanel(
                factory = { player.component() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (showPlaybackControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                AdbFilledButton(
                    onClick = { player.play() },
                    enabled = !videoPath.isNullOrBlank(),
                    text = stringResource(Res.string.screen_tools_player_action_play),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = { player.pause() },
                    enabled = !videoPath.isNullOrBlank(),
                    text = stringResource(Res.string.screen_tools_player_action_pause),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = { player.stop() },
                    enabled = !videoPath.isNullOrBlank(),
                    text = stringResource(Res.string.screen_tools_player_action_stop),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )
            }
        }

        if (showStatus) {
            statusMessage?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = AdbTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimensions.paddingXSmall),
                )
            }
        }
    }
}

@Composable
private fun rememberPlayerStatusStrings(): PlayerStatusStrings {
    val noVideo = stringResource(Res.string.screen_tools_player_status_no_video)
    val fileNotFoundFormat = stringResource(
        Res.string.screen_tools_player_status_file_not_found,
        "%1\$s",
    )
    val loadedFormat = stringResource(
        Res.string.screen_tools_player_status_loaded,
        "%1\$s",
    )
    val playerError = stringResource(Res.string.screen_tools_player_status_error_player)
    val mediaError = stringResource(Res.string.screen_tools_player_status_error_media)
    val playing = stringResource(Res.string.screen_tools_player_status_playing)
    val paused = stringResource(Res.string.screen_tools_player_status_paused)
    val stopped = stringResource(Res.string.screen_tools_player_status_stopped)

    return remember(
        noVideo,
        fileNotFoundFormat,
        loadedFormat,
        playerError,
        mediaError,
        playing,
        paused,
        stopped,
    ) {
        PlayerStatusStrings(
            noVideo = noVideo,
            fileNotFoundFormat = fileNotFoundFormat,
            loadedFormat = loadedFormat,
            playerError = playerError,
            mediaError = mediaError,
            playing = playing,
            paused = paused,
            stopped = stopped,
        )
    }
}

/**
 * Набор локализованных текстов для статусов плеера.
 */
private data class PlayerStatusStrings(
    val noVideo: String,
    val fileNotFoundFormat: String,
    val loadedFormat: String,
    val playerError: String,
    val mediaError: String,
    val playing: String,
    val paused: String,
    val stopped: String,
) {
    fun fileNotFound(path: String): String = String.format(Locale.ROOT, fileNotFoundFormat, path)
    fun loaded(seconds: Long): String = String.format(Locale.ROOT, loadedFormat, seconds.toString())
}

/**
 * Локальный адаптер JavaFX-плеера для Compose.
 */
private class FxVideoPlayer(
    private val strings: PlayerStatusStrings,
    private val onStatus: (String?) -> Unit,
) {
    private companion object {
        private const val FX_BACKGROUND_STYLE = "-fx-background-color: #111111;"
    }

    private val panel: JFXPanel = JFXPanel()
    private var mediaPlayer: MediaPlayer? = null

    init {
        Platform.runLater {
            panel.scene = Scene(StackPane().apply {
                style = FX_BACKGROUND_STYLE
            })
        }
    }

    fun component(): JComponent = panel

    fun load(path: String?) {
        if (path.isNullOrBlank()) {
            postStatus(strings.noVideo)
            Platform.runLater {
                releasePlayer()
                panel.scene = Scene(StackPane().apply { style = FX_BACKGROUND_STYLE })
            }
            return
        }

        val file = File(path)
        if (!file.isFile) {
            postStatus(strings.fileNotFound(path))
            Platform.runLater {
                releasePlayer()
                panel.scene = Scene(StackPane().apply { style = FX_BACKGROUND_STYLE })
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
                style = FX_BACKGROUND_STYLE
            }
            val scene = Scene(root)

            // Подгоняем размер video-view под фактический размер контейнера без обрезки.
            mediaView.fitWidthProperty().bind(scene.widthProperty())
            mediaView.fitHeightProperty().bind(scene.heightProperty())

            player.setOnReady {
                val seconds = player.totalDuration?.toSeconds()?.toLong() ?: 0L
                postStatus(strings.loaded(seconds))
            }
            player.setOnError {
                postStatus(player.error?.message ?: strings.playerError)
            }
            media.setOnError {
                postStatus(media.error?.message ?: strings.mediaError)
            }

            panel.scene = scene
            mediaPlayer = player
        }
    }

    fun play() {
        Platform.runLater {
            mediaPlayer?.play()
            postStatus(strings.playing)
        }
    }

    fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
            postStatus(strings.paused)
        }
    }

    fun stop() {
        Platform.runLater {
            mediaPlayer?.stop()
            postStatus(strings.stopped)
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

    /**
     * Публикует статус на EDT-поток, чтобы безопасно менять Compose state.
     */
    private fun postStatus(message: String?) {
        if (SwingUtilities.isEventDispatchThread()) {
            onStatus(message)
        } else {
            SwingUtilities.invokeLater { onStatus(message) }
        }
    }
}
