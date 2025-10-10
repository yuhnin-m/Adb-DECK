package com.adbdeck.feature.settings.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.feature.settings.SettingsComponent

/**
 * Экран настроек приложения ADB Deck.
 *
 * Секции:
 * - ADB — путь к исполняемому файлу adb + проверка.
 * - Тема — выбор светлой/тёмной/системной темы.
 * - Logcat — настройки отображения и производительности потокового лога.
 *
 * @param component Компонент настроек.
 */
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingLarge),
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(Dimensions.paddingLarge))
        HorizontalDivider()
        Spacer(Modifier.height(Dimensions.paddingLarge))

        // ── Секция ADB ──────────────────────────────────────────
        SettingsSection(title = "ADB") {
            OutlinedTextField(
                value = state.adbPath,
                onValueChange = component::onAdbPathChanged,
                label = { Text("Путь к adb") },
                placeholder = { Text("adb") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = "Оставьте 'adb', если исполняемый файл находится в PATH системы.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            Spacer(Modifier.height(Dimensions.paddingMedium))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                OutlinedButton(
                    onClick = component::onCheckAdb,
                    enabled = !state.isCheckingAdb,
                ) {
                    if (state.isCheckingAdb) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(16.dp)
                                .height(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(Dimensions.paddingSmall))
                        Text("Проверяется…")
                    } else {
                        Text("Проверить ADB")
                    }
                }

                Button(onClick = component::onSave) {
                    if (state.isSaved) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(Modifier.width(Dimensions.paddingXSmall))
                        Text("Сохранено")
                    } else {
                        Text("Сохранить")
                    }
                }
            }

            if (state.adbCheckResult.isNotBlank()) {
                Spacer(Modifier.height(Dimensions.paddingSmall))
                val isSuccess = state.adbCheckResult.startsWith("✓")
                Text(
                    text = state.adbCheckResult,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSuccess) AdbDeckGreen else MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(Dimensions.paddingLarge))
        HorizontalDivider()
        Spacer(Modifier.height(Dimensions.paddingLarge))

        // ── Секция темы ──────────────────────────────────────────
        SettingsSection(title = "Тема") {
            ThemeSelector(
                currentTheme = state.currentTheme,
                onThemeSelected = component::onThemeChanged,
            )
        }

        Spacer(Modifier.height(Dimensions.paddingLarge))
        HorizontalDivider()
        Spacer(Modifier.height(Dimensions.paddingLarge))

        // ── Секция Logcat ────────────────────────────────────────
        SettingsSection(title = "Logcat") {
            Text(
                text = "Настройки применяются при следующем запуске захвата лога.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimensions.paddingMedium))

            // Режим отображения
            SettingsRow(label = "Компактный режим") {
                Switch(
                    checked = state.logcatCompactMode,
                    onCheckedChange = component::onLogcatCompactModeChanged,
                )
            }

            SettingsRow(label = "Показывать дату (MM-DD)") {
                Switch(
                    checked = state.logcatShowDate,
                    onCheckedChange = component::onLogcatShowDateChanged,
                )
            }

            SettingsRow(label = "Показывать время (HH:MM:SS)") {
                Switch(
                    checked = state.logcatShowTime,
                    onCheckedChange = component::onLogcatShowTimeChanged,
                )
            }

            SettingsRow(label = "Показывать миллисекунды") {
                Switch(
                    checked = state.logcatShowMillis,
                    onCheckedChange = component::onLogcatShowMillisChanged,
                )
            }

            SettingsRow(label = "Цветовая подсветка уровней") {
                Switch(
                    checked = state.logcatColoredLevels,
                    onCheckedChange = component::onLogcatColoredLevelsChanged,
                )
            }

            SettingsRow(label = "Автоскролл по умолчанию") {
                Switch(
                    checked = state.logcatAutoScroll,
                    onCheckedChange = component::onLogcatAutoScrollChanged,
                )
            }

            Spacer(Modifier.height(Dimensions.paddingMedium))

            // Размер буфера
            MaxBufferLinesField(
                value = state.logcatMaxBufferedLines,
                onValueChange = component::onLogcatMaxBufferedLinesChanged,
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Обёртка для именованной секции настроек.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimensions.paddingMedium))
        content()
    }
}

/**
 * Строка настройки: метка слева, элемент управления справа.
 */
@Composable
private fun SettingsRow(
    label: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        control()
    }
}

/**
 * Поле для ввода максимального числа строк в буфере logcat.
 */
@Composable
private fun MaxBufferLinesField(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    // Локальное текстовое состояние для плавного ввода
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.toIntOrNull()?.let { parsed -> onValueChange(parsed) }
        },
        label = { Text("Макс. строк в буфере") },
        supportingText = {
            Text(
                text = "Минимум 100. Старые строки удаляются (FIFO) при переполнении.",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Выбор темы через набор FilterChip.
 */
@Composable
private fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val themes = listOf(
        AppTheme.LIGHT to "Светлая",
        AppTheme.DARK to "Тёмная",
        AppTheme.SYSTEM to "Системная",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        themes.forEach { (theme, label) ->
            FilterChip(
                selected = currentTheme == theme,
                onClick = { onThemeSelected(theme) },
                label = { Text(label) },
                leadingIcon = if (currentTheme == theme) {
                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}
