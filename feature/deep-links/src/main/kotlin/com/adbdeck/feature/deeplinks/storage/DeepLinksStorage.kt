package com.adbdeck.feature.deeplinks.storage

import com.adbdeck.core.adb.api.intents.DeepLinkParams
import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.IntentExtra
import com.adbdeck.core.adb.api.intents.IntentParams
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.feature.deeplinks.IntentTemplate
import com.adbdeck.feature.deeplinks.LaunchHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// Сериализуемые модели хранилища
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
internal data class StorageExtra(
    val key: String,
    val type: String,
    val value: String,
)

@Serializable
internal data class StorageHistoryEntry(
    val id: String,
    val mode: String,
    val dlUri: String? = null,
    val dlAction: String? = null,
    val dlPackage: String? = null,
    val dlComponent: String? = null,
    val dlCategory: String? = null,
    val itAction: String? = null,
    val itDataUri: String? = null,
    val itPackage: String? = null,
    val itComponent: String? = null,
    val itCategories: List<String> = emptyList(),
    val itFlags: String? = null,
    val itExtras: List<StorageExtra> = emptyList(),
    val launchedAt: Long,
    val commandPreview: String,
    val isSuccess: Boolean,
)

@Serializable
internal data class StorageTemplate(
    val id: String,
    val name: String,
    val mode: String,
    val dlUri: String? = null,
    val dlAction: String? = null,
    val dlPackage: String? = null,
    val dlComponent: String? = null,
    val dlCategory: String? = null,
    val itAction: String? = null,
    val itDataUri: String? = null,
    val itPackage: String? = null,
    val itComponent: String? = null,
    val itCategories: List<String> = emptyList(),
    val itFlags: String? = null,
    val itExtras: List<StorageExtra> = emptyList(),
    val createdAt: Long,
)

@Serializable
internal data class DeepLinksStorageData(
    val history: List<StorageHistoryEntry> = emptyList(),
    val templates: List<StorageTemplate> = emptyList(),
)

// ──────────────────────────────────────────────────────────────────────────────
// Сервис хранилища
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Персистентное хранилище истории и шаблонов Deep Links / Intents.
 *
 * Данные сохраняются в `~/.adbdeck/deep-links.json` в формате JSON.
 * Все операции выполняются в [Dispatchers.IO].
 */
class DeepLinksStorage {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val storageFile = File(System.getProperty("user.home"), ".adbdeck/deep-links.json")

    /**
     * Загрузить данные из файла.
     * Возвращает пустой [DeepLinksStorageData] при первом запуске или ошибке чтения.
     */
    suspend fun load(): Pair<List<LaunchHistoryEntry>, List<IntentTemplate>> =
        withContext(Dispatchers.IO) {
            if (!storageFile.exists()) return@withContext Pair(emptyList(), emptyList())
            runCatching {
                val data = json.decodeFromString<DeepLinksStorageData>(storageFile.readText())
                Pair(
                    data.history.map { it.toDomain() },
                    data.templates.map { it.toDomain() },
                )
            }.getOrDefault(Pair(emptyList(), emptyList()))
        }

    /**
     * Сохранить историю и шаблоны в файл.
     */
    suspend fun save(history: List<LaunchHistoryEntry>, templates: List<IntentTemplate>) =
        withContext(Dispatchers.IO) {
            runCatching {
                storageFile.parentFile?.mkdirs()
                val data = DeepLinksStorageData(
                    history = history.map { it.toStorage() },
                    templates = templates.map { it.toStorage() },
                )
                storageFile.writeText(json.encodeToString(data))
            }
        }
}

// ──────────────────────────────────────────────────────────────────────────────
// Конверторы: domain ↔ storage
// ──────────────────────────────────────────────────────────────────────────────

private fun LaunchHistoryEntry.toStorage() = StorageHistoryEntry(
    id             = id,
    mode           = mode.name,
    dlUri          = deepLinkParams?.uri,
    dlAction       = deepLinkParams?.action,
    dlPackage      = deepLinkParams?.packageName,
    dlComponent    = deepLinkParams?.component,
    dlCategory     = deepLinkParams?.category,
    itAction       = intentParams?.action,
    itDataUri      = intentParams?.dataUri,
    itPackage      = intentParams?.packageName,
    itComponent    = intentParams?.component,
    itCategories   = intentParams?.categories ?: emptyList(),
    itFlags        = intentParams?.flags,
    itExtras       = intentParams?.extras?.map { StorageExtra(it.key, it.type.name, it.value) } ?: emptyList(),
    launchedAt     = launchedAt,
    commandPreview = commandPreview,
    isSuccess      = isSuccess,
)

private fun StorageHistoryEntry.toDomain(): LaunchHistoryEntry {
    val parsedMode = runCatching { LaunchMode.valueOf(mode) }.getOrDefault(LaunchMode.DEEP_LINK)
    return LaunchHistoryEntry(
        id   = id,
        mode = parsedMode,
        deepLinkParams = if (parsedMode == LaunchMode.DEEP_LINK) DeepLinkParams(
            uri         = dlUri ?: "",
            action      = dlAction ?: "android.intent.action.VIEW",
            packageName = dlPackage ?: "",
            component   = dlComponent ?: "",
            category    = dlCategory ?: "",
        ) else null,
        intentParams = if (parsedMode == LaunchMode.INTENT) IntentParams(
            action      = itAction ?: "",
            dataUri     = itDataUri ?: "",
            packageName = itPackage ?: "",
            component   = itComponent ?: "",
            categories  = itCategories,
            flags       = itFlags ?: "",
            extras      = itExtras.map { extra ->
                IntentExtra(
                    key   = extra.key,
                    type  = runCatching { ExtraType.valueOf(extra.type) }.getOrDefault(ExtraType.STRING),
                    value = extra.value,
                )
            },
        ) else null,
        launchedAt     = launchedAt,
        commandPreview = commandPreview,
        isSuccess      = isSuccess,
    )
}

private fun IntentTemplate.toStorage() = StorageTemplate(
    id          = id,
    name        = name,
    mode        = mode.name,
    dlUri       = deepLinkParams?.uri,
    dlAction    = deepLinkParams?.action,
    dlPackage   = deepLinkParams?.packageName,
    dlComponent = deepLinkParams?.component,
    dlCategory  = deepLinkParams?.category,
    itAction    = intentParams?.action,
    itDataUri   = intentParams?.dataUri,
    itPackage   = intentParams?.packageName,
    itComponent = intentParams?.component,
    itCategories = intentParams?.categories ?: emptyList(),
    itFlags     = intentParams?.flags,
    itExtras    = intentParams?.extras?.map { StorageExtra(it.key, it.type.name, it.value) } ?: emptyList(),
    createdAt   = createdAt,
)

private fun StorageTemplate.toDomain(): IntentTemplate {
    val parsedMode = runCatching { LaunchMode.valueOf(mode) }.getOrDefault(LaunchMode.DEEP_LINK)
    return IntentTemplate(
        id   = id,
        name = name,
        mode = parsedMode,
        deepLinkParams = if (parsedMode == LaunchMode.DEEP_LINK) DeepLinkParams(
            uri         = dlUri ?: "",
            action      = dlAction ?: "android.intent.action.VIEW",
            packageName = dlPackage ?: "",
            component   = dlComponent ?: "",
            category    = dlCategory ?: "",
        ) else null,
        intentParams = if (parsedMode == LaunchMode.INTENT) IntentParams(
            action      = itAction ?: "",
            dataUri     = itDataUri ?: "",
            packageName = itPackage ?: "",
            component   = itComponent ?: "",
            categories  = itCategories,
            flags       = itFlags ?: "",
            extras      = itExtras.map { extra ->
                IntentExtra(
                    key   = extra.key,
                    type  = runCatching { ExtraType.valueOf(extra.type) }.getOrDefault(ExtraType.STRING),
                    value = extra.value,
                )
            },
        ) else null,
        createdAt = createdAt,
    )
}
