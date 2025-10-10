package com.adbdeck.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.dashboard.DashboardState

/**
 * Экран Dashboard — стартовый экран с плитками быстрых действий.
 *
 * @param component Компонент Dashboard, содержащий состояние и обработчики.
 */
@Composable
fun DashboardScreen(component: DashboardComponent) {
    val state by component.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingLarge),
    ) {
        // ── Заголовок ───────────────────────────────────────────
        Text(
            text = "ADB Deck",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Dimensions.paddingXSmall))
        Text(
            text = "Управление Android-устройствами через ADB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimensions.paddingXLarge))

        // ── Плитки быстрых действий ─────────────────────────────
        Text(
            text = "Быстрые действия",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimensions.paddingMedium))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ActionTile(
                icon = Icons.Outlined.DevicesOther,
                title = "Устройства",
                subtitle = state.deviceCount?.let { "$it подключено" } ?: "Список устройств",
                onClick = component::onOpenDevices,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.Terminal,
                title = "Logcat",
                subtitle = "Просмотр логов",
                onClick = component::onOpenLogcat,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.Settings,
                title = "Настройки",
                subtitle = "Конфигурация ADB",
                onClick = component::onOpenSettings,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Dimensions.paddingMedium))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ActionTile(
                icon = Icons.Outlined.Refresh,
                title = "Обновить устройства",
                subtitle = if (state.isRefreshingDevices) "Обновляется…" else "Запустить adb devices",
                onClick = component::onRefreshDevices,
                isLoading = state.isRefreshingDevices,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Outlined.Checklist,
                title = "Проверить ADB",
                subtitle = if (state.isCheckingAdb) "Проверяется…" else "adb version",
                onClick = component::onCheckAdb,
                isLoading = state.isCheckingAdb,
                modifier = Modifier.weight(1f),
            )
            // Пустой столбец для выравнивания сетки
            Spacer(modifier = Modifier.weight(1f))
        }

        // ── Результат проверки adb ──────────────────────────────
        if (state.adbStatusText.isNotBlank()) {
            Spacer(Modifier.height(Dimensions.paddingLarge))
            AdbStatusCard(statusText = state.adbStatusText)
        }
    }
}

/**
 * Кликабельная плитка быстрого действия.
 *
 * @param icon      Иконка действия.
 * @param title     Заголовок плитки.
 * @param subtitle  Подпись или описание.
 * @param onClick   Обработчик нажатия.
 * @param isLoading Если `true` — показывается индикатор загрузки.
 * @param modifier  Модификатор карточки.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.paddingDefault),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.iconSizeCard),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.iconSizeCard),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Карточка с результатом проверки доступности adb.
 *
 * @param statusText Текст результата проверки.
 */
@Composable
private fun AdbStatusCard(statusText: String) {
    val isSuccess = statusText.startsWith("✓")
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSuccess) {
                AdbDeckGreen.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(Dimensions.paddingSmall))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSuccess) {
                    AdbDeckGreen
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
