package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.designsystem.AdbDeckAmber
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.AdbDeckRed
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState

/**
 * Экран списка подключённых ADB-устройств.
 *
 * Отображает состояние загрузки, список устройств, пустое состояние или ошибку.
 *
 * @param component Компонент, управляющий состоянием экрана.
 */
@Composable
fun DevicesScreen(component: DevicesComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Заголовок + кнопка обновления ───────────────────────
        DevicesToolbar(onRefresh = component::onRefresh)
        HorizontalDivider()

        // ── Контент в зависимости от состояния ──────────────────
        when (val s = state) {
            is DevicesState.Loading -> LoadingView(message = "Получение списка устройств…")
            is DevicesState.Empty -> EmptyView(message = "Нет подключённых устройств.\nПодключите устройство по USB или запустите эмулятор.")
            is DevicesState.Error -> ErrorView(message = s.message, onRetry = component::onRefresh)
            is DevicesState.Success -> DevicesList(devices = s.devices)
        }
    }
}

/**
 * Панель инструментов экрана устройств с заголовком и кнопкой обновления.
 */
@Composable
private fun DevicesToolbar(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.topBarHeight)
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Устройства",
            style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Обновить список устройств",
                modifier = Modifier.size(Dimensions.iconSizeNav),
            )
        }
    }
}

/**
 * Прокручиваемый список устройств.
 *
 * @param devices Список устройств для отображения.
 */
@Composable
private fun DevicesList(devices: List<AdbDevice>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        items(devices, key = { it.deviceId }) { device ->
            DeviceItem(device = device)
        }
    }
}

/**
 * Карточка одного устройства.
 *
 * @param device Модель устройства для отображения.
 */
@Composable
private fun DeviceItem(device: AdbDevice) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeCard),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Dimensions.paddingDefault))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceId,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (device.info.isNotBlank()) {
                    Text(
                        text = device.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(Dimensions.paddingDefault))
            DeviceStateBadge(state = device.state)
        }
    }
}

/**
 * Цветной бейдж, отображающий состояние устройства.
 *
 * @param state Состояние устройства.
 */
@Composable
private fun DeviceStateBadge(state: DeviceState) {
    val (color, label) = when (state) {
        DeviceState.DEVICE -> Pair(AdbDeckGreen, "device")
        DeviceState.OFFLINE -> Pair(AdbDeckRed, "offline")
        DeviceState.UNAUTHORIZED -> Pair(AdbDeckAmber, "unauthorized")
        DeviceState.UNKNOWN -> Pair(MaterialTheme.colorScheme.outline, state.rawValue)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = Dimensions.paddingSmall,
                vertical = Dimensions.paddingXSmall,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
