package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_action_adb_install_guide
import adbdeck.feature.settings.generated.resources.settings_action_bundletool_install_guide
import adbdeck.feature.settings.generated.resources.settings_action_scrcpy_install_guide
import adbdeck.feature.settings.generated.resources.settings_help_load_failed
import adbdeck.feature.settings.generated.resources.settings_help_loading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import java.awt.Desktop
import java.util.Locale
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.event.HyperlinkEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

private sealed interface HelpContentState {
    data object Loading : HelpContentState
    data class Loaded(val html: String) : HelpContentState
    data object Error : HelpContentState
}

@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun HelpSidebarPanel(
    tool: HelpTool,
    currentLanguage: AppLanguage,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var contentState by remember(tool, currentLanguage) {
        mutableStateOf<HelpContentState>(HelpContentState.Loading)
    }

    val title = when (tool) {
        HelpTool.ADB -> stringResource(Res.string.settings_action_adb_install_guide)
        HelpTool.BUNDLETOOL -> stringResource(Res.string.settings_action_bundletool_install_guide)
        HelpTool.SCRCPY -> stringResource(Res.string.settings_action_scrcpy_install_guide)
    }
    val loadingText = stringResource(Res.string.settings_help_loading)
    val errorText = stringResource(Res.string.settings_help_load_failed)

    LaunchedEffect(tool, currentLanguage) {
        contentState = runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                val path = resolveHelpResourcePath(tool, currentLanguage)
                Res.readBytes(path).decodeToString()
            }
        }.fold(
            onSuccess = { HelpContentState.Loaded(it) },
            onFailure = { HelpContentState.Error },
        )
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingDefault,
                        vertical = Dimensions.paddingSmall,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = contentState) {
                    HelpContentState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimensions.iconSizeSmall),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HelpContentState.Error -> Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimensions.paddingLarge),
                    )

                    is HelpContentState.Loaded -> SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            createHelpScrollPane(state.html)
                        },
                        update = { pane ->
                            updateHelpScrollPane(pane, state.html)
                        },
                    )
                }
            }
        }
    }
}

private fun resolveHelpResourcePath(tool: HelpTool, language: AppLanguage): String {
    val isRussian = when (language) {
        AppLanguage.RUSSIAN -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language.startsWith("ru", ignoreCase = true)
    }
    return when (tool) {
        HelpTool.ADB -> if (isRussian) "files/adb_help_ru.html" else "files/adb_help_en.html"
        HelpTool.BUNDLETOOL -> if (isRussian) "files/bundletool_help_ru.html" else "files/bundletool_help_en.html"
        HelpTool.SCRCPY -> if (isRussian) "files/scrcpy_help_ru.html" else "files/scrcpy_help_en.html"
    }
}

private fun createHelpScrollPane(html: String): JScrollPane {
    val editor = JEditorPane("text/html", html).apply {
        isEditable = false
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                openHelpHyperlink(event)
            }
        }
        caretPosition = 0
    }
    return JScrollPane(editor).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
}

private fun updateHelpScrollPane(scrollPane: JScrollPane, html: String) {
    val editor = scrollPane.viewport.view as? JEditorPane ?: return
    if (editor.text != html) {
        editor.text = html
        editor.caretPosition = 0
    }
}

private fun openHelpHyperlink(event: HyperlinkEvent) {
    val url = event.url ?: return
    if (!Desktop.isDesktopSupported()) return
    val desktop = try {
        Desktop.getDesktop()
    } catch (_: Exception) {
        return
    }
    if (!desktop.isSupported(Desktop.Action.BROWSE)) return
    try {
        desktop.browse(url.toURI())
    } catch (_: Exception) {
        Unit
    }
}
