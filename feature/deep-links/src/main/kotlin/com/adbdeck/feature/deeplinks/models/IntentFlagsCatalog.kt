package com.adbdeck.feature.deeplinks.models

import androidx.compose.runtime.Immutable

@Immutable
internal enum class IntentFlagGroup {
    COMMON,
    ADVANCED,
    RISKY,
    URI_PERMISSIONS,
}

@Immutable
internal enum class IntentFlagBadge {
    SAFE,
    ADVANCED,
    RISKY,
}

@Immutable
internal data class IntentFlagDefinition(
    val key: String,
    val label: String,
    val value: Int,
    val description: String,
    val group: IntentFlagGroup,
    val badge: IntentFlagBadge,
)

@Immutable
internal data class IntentFlagPreset(
    val id: String,
    val title: String,
    val description: String,
    val flagKeys: Set<String>,
)

internal const val FLAG_ACTIVITY_NEW_TASK = "FLAG_ACTIVITY_NEW_TASK"
internal const val FLAG_ACTIVITY_CLEAR_TOP = "FLAG_ACTIVITY_CLEAR_TOP"
internal const val FLAG_ACTIVITY_SINGLE_TOP = "FLAG_ACTIVITY_SINGLE_TOP"
internal const val FLAG_ACTIVITY_REORDER_TO_FRONT = "FLAG_ACTIVITY_REORDER_TO_FRONT"
internal const val FLAG_ACTIVITY_NO_HISTORY = "FLAG_ACTIVITY_NO_HISTORY"
internal const val FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS"
internal const val FLAG_ACTIVITY_CLEAR_TASK = "FLAG_ACTIVITY_CLEAR_TASK"
internal const val FLAG_ACTIVITY_MULTIPLE_TASK = "FLAG_ACTIVITY_MULTIPLE_TASK"
internal const val FLAG_ACTIVITY_TASK_ON_HOME = "FLAG_ACTIVITY_TASK_ON_HOME"
internal const val FLAG_GRANT_READ_URI_PERMISSION = "FLAG_GRANT_READ_URI_PERMISSION"
internal const val FLAG_GRANT_WRITE_URI_PERMISSION = "FLAG_GRANT_WRITE_URI_PERMISSION"
internal const val FLAG_GRANT_PERSISTABLE_URI_PERMISSION = "FLAG_GRANT_PERSISTABLE_URI_PERMISSION"
internal const val DEFAULT_INTENT_FLAGS_MASK = "0x10000000"
internal val DefaultIntentFlagsSelection: Set<String> = setOf(FLAG_ACTIVITY_NEW_TASK)

internal val IntentFlagDefinitions: List<IntentFlagDefinition> = listOf(
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_NEW_TASK,
        label = "FLAG_ACTIVITY_NEW_TASK",
        value = 0x10000000,
        description = "Запуск в новой task. Back stack будет отличаться от обычного.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_CLEAR_TOP,
        label = "FLAG_ACTIVITY_CLEAR_TOP",
        value = 0x04000000,
        description = "Если activity уже есть в стеке, удаляет все над ней и поднимает ее наверх.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_SINGLE_TOP,
        label = "FLAG_ACTIVITY_SINGLE_TOP",
        value = 0x20000000,
        description = "Не создавать новый экземпляр, если target уже наверху стека.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_REORDER_TO_FRONT,
        label = "FLAG_ACTIVITY_REORDER_TO_FRONT",
        value = 0x00020000,
        description = "Перемещает существующую activity в верх стека без пересоздания.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_NO_HISTORY,
        label = "FLAG_ACTIVITY_NO_HISTORY",
        value = 0x40000000,
        description = "Activity не останется в history после ухода с нее.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        label = "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS",
        value = 0x00800000,
        description = "Task не будет показана в Recents.",
        group = IntentFlagGroup.COMMON,
        badge = IntentFlagBadge.SAFE,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_TASK_ON_HOME,
        label = "FLAG_ACTIVITY_TASK_ON_HOME",
        value = 0x00004000,
        description = "Помещает task поверх Home. Использовать осознанно.",
        group = IntentFlagGroup.ADVANCED,
        badge = IntentFlagBadge.ADVANCED,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_CLEAR_TASK,
        label = "FLAG_ACTIVITY_CLEAR_TASK",
        value = 0x00008000,
        description = "Очищает текущую task перед запуском. Может резко поменять back stack.",
        group = IntentFlagGroup.RISKY,
        badge = IntentFlagBadge.RISKY,
    ),
    IntentFlagDefinition(
        key = FLAG_ACTIVITY_MULTIPLE_TASK,
        label = "FLAG_ACTIVITY_MULTIPLE_TASK",
        value = 0x08000000,
        description = "Разрешает несколько task для одной activity. Может спутать навигацию QA.",
        group = IntentFlagGroup.RISKY,
        badge = IntentFlagBadge.RISKY,
    ),
    IntentFlagDefinition(
        key = FLAG_GRANT_READ_URI_PERMISSION,
        label = "FLAG_GRANT_READ_URI_PERMISSION",
        value = 0x00000001,
        description = "Выдать временный доступ на чтение URI.",
        group = IntentFlagGroup.URI_PERMISSIONS,
        badge = IntentFlagBadge.ADVANCED,
    ),
    IntentFlagDefinition(
        key = FLAG_GRANT_WRITE_URI_PERMISSION,
        label = "FLAG_GRANT_WRITE_URI_PERMISSION",
        value = 0x00000002,
        description = "Выдать временный доступ на запись URI.",
        group = IntentFlagGroup.URI_PERMISSIONS,
        badge = IntentFlagBadge.ADVANCED,
    ),
    IntentFlagDefinition(
        key = FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        label = "FLAG_GRANT_PERSISTABLE_URI_PERMISSION",
        value = 0x00000040,
        description = "Разрешить persistable URI permission через takePersistableUriPermission.",
        group = IntentFlagGroup.URI_PERMISSIONS,
        badge = IntentFlagBadge.ADVANCED,
    ),
)

internal val IntentFlagDefinitionsByKey: Map<String, IntentFlagDefinition> =
    IntentFlagDefinitions.associateBy { definition -> definition.key }

internal val IntentFlagPresets: List<IntentFlagPreset> = listOf(
    IntentFlagPreset(
        id = "default",
        title = "Default",
        description = "Без флагов.",
        flagKeys = emptySet(),
    ),
    IntentFlagPreset(
        id = "new_task",
        title = "New task",
        description = "Запуск в новой task.",
        flagKeys = setOf(FLAG_ACTIVITY_NEW_TASK),
    ),
    IntentFlagPreset(
        id = "clear_top",
        title = "Clear top",
        description = "Вернуть существующий экран наверх стека.",
        flagKeys = setOf(FLAG_ACTIVITY_CLEAR_TOP, FLAG_ACTIVITY_SINGLE_TOP),
    ),
    IntentFlagPreset(
        id = "cold_start",
        title = "Cold start",
        description = "Новая task с очисткой предыдущего стека.",
        flagKeys = setOf(FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TASK),
    ),
)

internal fun computeIntentFlagsMask(selectedFlagKeys: Set<String>): Int =
    selectedFlagKeys.fold(0) { mask, key ->
        val flagValue = IntentFlagDefinitionsByKey[key]?.value ?: 0
        mask or flagValue
    }

internal fun deriveSelectedIntentFlags(mask: Int): Set<String> =
    IntentFlagDefinitions.asSequence()
        .filter { definition -> (mask and definition.value) == definition.value }
        .map { definition -> definition.key }
        .toSet()

internal fun formatIntentFlagsMask(mask: Int): String = "0x${mask.toString(16).uppercase().padStart(8, '0')}"

internal fun parseIntentFlagsMask(rawValue: String): Result<Int> {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) {
        return Result.success(0)
    }

    return runCatching {
        val value = if (trimmed.startsWith("0x", ignoreCase = true)) {
            val hexPart = trimmed.substring(2).replace("_", "")
            require(hexPart.isNotEmpty()) {
                "После 0x нужно указать hex-значение маски"
            }
            hexPart.toLong(radix = 16)
        } else {
            trimmed.replace("_", "").toLong()
        }

        require(value >= 0) {
            "Маска не может быть отрицательной"
        }
        require(value <= Int.MAX_VALUE.toLong()) {
            "Маска должна помещаться в Int"
        }

        value.toInt()
    }
}
