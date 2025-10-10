package com.adbdeck.core.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт репозитория настроек приложения.
 *
 * Читает и сохраняет [AppSettings]. Изменения настроек транслируются
 * через [settingsFlow], на который можно подписаться из UI.
 */
interface SettingsRepository {

    /**
     * Поток текущих настроек приложения.
     * Эмитит новое значение каждый раз, когда настройки сохраняются.
     */
    val settingsFlow: StateFlow<AppSettings>

    /**
     * Возвращает текущие настройки синхронно (последнее сохранённое значение).
     */
    fun getSettings(): AppSettings

    /**
     * Сохраняет новые настройки и обновляет [settingsFlow].
     *
     * @param settings Новый объект настроек.
     */
    suspend fun saveSettings(settings: AppSettings)
}
