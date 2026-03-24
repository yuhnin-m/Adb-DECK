package com.adbdeck.app.update

/**
 * Фазы UI-состояния обновления приложения.
 */
enum class AppUpdatePhase {
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    INSTALLING,
    RESTARTING,
    ERROR,
}

/**
 * UI-модель обновления приложения.
 *
 * @param visible Показывать диалог обновления.
 * @param blocking Блокировать закрытие/сворачивание диалога.
 * @param phase Текущая фаза обновления.
 * @param currentVersion Текущая версия приложения.
 * @param targetVersion Целевая версия обновления.
 * @param progress Прогресс в диапазоне `0f..1f` (если доступен).
 * @param details Дополнительный статус или сообщение ошибки.
 * @param changelog Текст изменений для пользователя.
 * @param canInstallNow Показывать кнопку установки "сейчас".
 * @param canCancel Показывать кнопку отмены текущего обновления.
 * @param canDismiss Разрешать скрытие диалога.
 */
data class AppUpdateUiState(
    val visible: Boolean = false,
    val blocking: Boolean = false,
    val phase: AppUpdatePhase = AppUpdatePhase.CHECKING,
    val currentVersion: String? = null,
    val targetVersion: String? = null,
    val progress: Float? = null,
    val details: String? = null,
    val changelog: String = "",
    val canInstallNow: Boolean = false,
    val canCancel: Boolean = false,
    val canDismiss: Boolean = false,
) {
    companion object {
        val Hidden: AppUpdateUiState = AppUpdateUiState(visible = false)
    }
}
