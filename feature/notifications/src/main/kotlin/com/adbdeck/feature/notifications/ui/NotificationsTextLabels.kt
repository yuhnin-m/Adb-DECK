package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_importance_default
import adbdeck.feature.notifications.generated.resources.notifications_importance_high
import adbdeck.feature.notifications.generated.resources.notifications_importance_low
import adbdeck.feature.notifications.generated.resources.notifications_importance_min
import adbdeck.feature.notifications.generated.resources.notifications_importance_none
import adbdeck.feature.notifications.generated.resources.notifications_importance_unspecified
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun notificationImportanceLabel(importance: Int): String = when (importance) {
    5 -> stringResource(Res.string.notifications_importance_high)
    4 -> stringResource(Res.string.notifications_importance_default)
    3 -> stringResource(Res.string.notifications_importance_low)
    2 -> stringResource(Res.string.notifications_importance_min)
    1 -> stringResource(Res.string.notifications_importance_none)
    0 -> stringResource(Res.string.notifications_importance_unspecified)
    else -> importance.toString()
}
