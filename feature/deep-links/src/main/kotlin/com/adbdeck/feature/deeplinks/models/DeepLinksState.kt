package com.adbdeck.feature.deeplinks.models

import com.adbdeck.core.adb.api.intents.DeepLinkParams
import com.adbdeck.core.adb.api.intents.IntentExtra
import com.adbdeck.core.adb.api.intents.IntentParams
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.adb.api.intents.LaunchResult

// ──────────────────────────────────────────────────────────────────────────────
// Вспомогательные типы
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Вкладка правой панели экрана Deep Links / Intents.
 */
enum class DeepLinksTab {
    /** Предпросмотр команды и результат последнего запуска. */
    COMMAND_RESULT,

    /** История последних запусков. */
    HISTORY,

    /** Сохранённые шаблоны конфигураций. */
    TEMPLATES,
}

/**
 * Запись в истории запусков.
 *
 * @param id             Уникальный идентификатор записи.
 * @param mode           Режим запуска ([LaunchMode]).
 * @param deepLinkParams Параметры deep link (заполнен если `mode == DEEP_LINK`).
 * @param intentParams   Параметры intent (заполнен если `mode == INTENT`).
 * @param launchedAt     Временная метка запуска (Unix ms).
 * @param commandPreview Строковое представление выполненной команды.
 * @param isSuccess      `true` если команда завершилась без ошибок.
 */
data class LaunchHistoryEntry(
    val id: String,
    val mode: LaunchMode,
    val deepLinkParams: DeepLinkParams? = null,
    val intentParams: IntentParams? = null,
    val launchedAt: Long,
    val commandPreview: String,
    val isSuccess: Boolean,
)

/**
 * Сохранённый шаблон конфигурации запуска.
 *
 * @param id             Уникальный идентификатор шаблона.
 * @param name           Пользовательское название шаблона.
 * @param mode           Режим запуска ([LaunchMode]).
 * @param deepLinkParams Параметры deep link (если `mode == DEEP_LINK`).
 * @param intentParams   Параметры intent (если `mode == INTENT`).
 * @param createdAt      Временная метка создания (Unix ms).
 */
data class IntentTemplate(
    val id: String,
    val name: String,
    val mode: LaunchMode,
    val deepLinkParams: DeepLinkParams? = null,
    val intentParams: IntentParams? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Краткое feedback-сообщение для баннера.
 *
 * @param message Текст сообщения.
 * @param isError `true`, если сообщение об ошибке.
 */
data class DeepLinksFeedback(
    val message: String,
    val isError: Boolean = false,
)

// ──────────────────────────────────────────────────────────────────────────────
// Агрегированное состояние экрана
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Полное состояние экрана Deep Links / Intents.
 *
 * @param mode                  Активный режим запуска.
 * @param dlUri                 URI для deep link.
 * @param dlAction              Action для deep link.
 * @param dlPackage             Package для deep link.
 * @param dlComponent           Component для deep link.
 * @param dlCategory            Category для deep link.
 * @param itAction              Action для intent.
 * @param itDataUri             Data URI для intent.
 * @param itPackage             Package для intent.
 * @param itComponent           Component для intent.
 * @param itCategories          Список categories для intent.
 * @param itFlags               Flags для intent (hex-строка, по умолчанию `0x10000000` / `NEW_TASK`).
 * @param itSelectedFlags       Выбранные флаги intent (для диалога Pick flags).
 * @param itFlagsValidationMessage Ошибка валидации/парсинга флагов (если есть).
 * @param itExtras              Typed extras для intent.
 * @param commandPreview        Текущий предпросмотр команды.
 * @param isLaunching           `true` во время выполнения запуска.
 * @param lastResult            Результат последнего запуска.
 * @param rightTab              Активная вкладка правой панели.
 * @param history               История запусков (макс. 50 записей).
 * @param templates             Сохранённые шаблоны.
 * @param isSaveTemplateDialogOpen Диалог сохранения шаблона открыт.
 * @param saveTemplateName      Имя в диалоге сохранения шаблона.
 * @param isIntentFlagsDialogOpen Диалог выбора флагов открыт.
 * @param activeDeviceId        ID активного ADB-устройства.
 * @param packageSuggestions    Подсказки установленных пакетов активного устройства.
 * @param isPackageSuggestionsLoading `true` во время загрузки подсказок пакетов.
 * @param feedback              Временное сообщение для баннера.
 */
data class DeepLinksState(
    val mode: LaunchMode = LaunchMode.DEEP_LINK,

    // Форма Deep Link
    val dlUri: String = "",
    val dlAction: String = "android.intent.action.VIEW",
    val dlPackage: String = "",
    val dlComponent: String = "",
    val dlCategory: String = "",

    // Форма Intent
    val itAction: String = "",
    val itDataUri: String = "",
    val itPackage: String = "",
    val itComponent: String = "",
    val itCategories: List<String> = emptyList(),
    val itFlags: String = DEFAULT_INTENT_FLAGS_MASK,
    val itSelectedFlags: Set<String> = DefaultIntentFlagsSelection,
    val itFlagsValidationMessage: String? = null,
    val itExtras: List<IntentExtra> = emptyList(),

    // Предпросмотр и результат
    val commandPreview: String = "",
    val isLaunching: Boolean = false,
    val lastResult: LaunchResult? = null,

    // Правая панель
    val rightTab: DeepLinksTab = DeepLinksTab.COMMAND_RESULT,

    // История и шаблоны
    val history: List<LaunchHistoryEntry> = emptyList(),
    val templates: List<IntentTemplate> = emptyList(),

    // Диалог сохранения шаблона
    val isSaveTemplateDialogOpen: Boolean = false,
    val saveTemplateName: String = "",
    val isIntentFlagsDialogOpen: Boolean = false,

    // Активное устройство
    val activeDeviceId: String? = null,
    val packageSuggestions: List<String> = emptyList(),
    val isPackageSuggestionsLoading: Boolean = false,

    // Feedback
    val feedback: DeepLinksFeedback? = null,
)
