package com.adbdeck.core.i18n

import adbdeck.core.i18n.generated.resources.Res
import adbdeck.core.i18n.generated.resources.common_action_clear
import adbdeck.core.i18n.generated.resources.common_action_close_settings
import adbdeck.core.i18n.generated.resources.common_action_settings
import adbdeck.core.i18n.generated.resources.common_action_start
import adbdeck.core.i18n.generated.resources.common_action_stop
import adbdeck.core.i18n.generated.resources.common_placeholder_search
import org.jetbrains.compose.resources.StringResource

/**
 * Общие строковые ресурсы приложения.
 *
 * Нужны для унификации повторяющихся подписей между feature-модулями.
 */
object AdbCommonStringRes {

    /** Действие запуска процесса. */
    val actionStart: StringResource
        get() = Res.string.common_action_start

    /** Действие остановки процесса. */
    val actionStop: StringResource
        get() = Res.string.common_action_stop

    /** Действие очистки данных/состояния. */
    val actionClear: StringResource
        get() = Res.string.common_action_clear

    /** Подпись кнопки открытия настроек. */
    val actionSettings: StringResource
        get() = Res.string.common_action_settings

    /** Подпись кнопки закрытия панели настроек. */
    val actionCloseSettings: StringResource
        get() = Res.string.common_action_close_settings

    /** Универсальный placeholder для поля поиска. */
    val placeholderSearch: StringResource
        get() = Res.string.common_placeholder_search
}
