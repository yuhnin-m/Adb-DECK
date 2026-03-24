package com.adbdeck.feature.update

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт app-level компонента обновления приложения.
 *
 * Компонент отвечает за:
 * - принятие решения, нужно ли показывать диалог обновления;
 * - обработку действий пользователя в диалоге;
 * - хранение единого [state], которое читает UI.
 */
interface AppUpdateComponent {

    /** Текущее состояние UI диалога обновления. */
    val state: StateFlow<AppUpdateUiState>

    /** Инициализация проверки обновления при старте приложения. */
    fun onStart()

    /**
     * Явная ручная проверка обновления.
     *
     * @return `true`, если найдено обновление и показан update-диалог; иначе `false`.
     * @throws IllegalStateException если проверка не удалась.
     */
    suspend fun checkForUpdatesNow(): Boolean

    /** Пользователь нажал "Install now". */
    fun onInstallUpdateNow()

    /** Пользователь открыл диалог доступного обновления из баннера/настроек. */
    fun onOpenUpdateDialog()

    /** Пользователь нажал "Cancel update". */
    fun onCancelUpdate()

    /** Пользователь скрыл диалог обновления. */
    fun onDismissUpdate()
}
