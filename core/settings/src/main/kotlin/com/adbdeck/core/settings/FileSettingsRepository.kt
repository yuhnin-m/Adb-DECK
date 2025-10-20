package com.adbdeck.core.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Реализация [SettingsRepository], хранящая настройки в JSON-файле на диске.
 *
 * Файл создается автоматически при первом сохранении.
 * При ошибке чтения используются настройки по умолчанию [AppSettings].
 *
 * @param settingsFile Путь к JSON-файлу настроек.
 */
class FileSettingsRepository(private val settingsFile: File) : SettingsRepository {

    /** Mutex для безопасного конкурентного доступа к файлу. */
    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settingsFlow = MutableStateFlow(loadSettingsFromDisk())

    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    override fun getSettings(): AppSettings = _settingsFlow.value

    override suspend fun saveSettings(settings: AppSettings) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                settingsFile.parentFile?.mkdirs()
                settingsFile.writeText(json.encodeToString(settings))
            }
            _settingsFlow.value = settings
        }
    }

    /** Читает настройки с диска. При ошибке возвращает значения по умолчанию. */
    private fun loadSettingsFromDisk(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                json.decodeFromString<AppSettings>(settingsFile.readText())
            } else {
                AppSettings()
            }
        } catch (_: Exception) {
            AppSettings()
        }
    }
}
