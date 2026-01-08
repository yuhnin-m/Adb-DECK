package com.adbdeck.feature.quicktoggles.service

import com.adbdeck.feature.quicktoggles.QuickToggleId
import com.adbdeck.feature.quicktoggles.QuickToggleState

/**
 * Значение одного параметра анимаций.
 *
 * @param key Ключ параметра в `settings global`.
 * @param value Текущее числовое значение (`null` = unset/default).
 * @param error Ошибка чтения параметра, если есть.
 */
data class AnimationScaleValue(
    val key: String,
    val value: Float?,
    val error: String? = null,
)

/**
 * Снимок статусов quick-toggle элементов.
 *
 * @param states Состояния toggle-элементов.
 * @param readErrors Ошибки чтения статусов по toggle-идентификатору (best-effort).
 * @param animationValues Значения параметров Animations.
 */
data class QuickToggleStatusSnapshot(
    val states: Map<QuickToggleId, QuickToggleState>,
    val readErrors: Map<QuickToggleId, String>,
    val animationValues: Map<QuickToggleId, List<AnimationScaleValue>> = emptyMap(),
)

/**
 * Результат чтения статуса одного toggle-элемента.
 *
 * @param state Текущее состояние (`ON/OFF/CUSTOM/UNKNOWN`).
 * @param error Ошибка чтения, если статус не удалось определить надежно.
 * @param animationValues Значения параметров Animations (для `ANIMATIONS`).
 */
data class QuickToggleReadResult(
    val state: QuickToggleState,
    val error: String? = null,
    val animationValues: List<AnimationScaleValue> = emptyList(),
)

/**
 * Сервис quick-toggle операций.
 */
interface QuickTogglesService {

    /** Прочитать статусы всех toggle-элементов. */
    suspend fun readStatuses(
        deviceId: String,
        adbPath: String,
    ): QuickToggleStatusSnapshot

    /** Прочитать статус конкретного toggle-элемента. */
    suspend fun readStatus(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
    ): QuickToggleReadResult

    /** Применить целевое состояние toggle-элемента. */
    suspend fun setToggle(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
        targetState: QuickToggleState,
    ): Result<Unit>

    /** Прочитать значения всех трех animation-scale параметров. */
    suspend fun readAnimationScales(
        deviceId: String,
        adbPath: String,
    ): List<AnimationScaleValue>

    /** Установить значение конкретного animation-scale параметра. */
    suspend fun setAnimationScale(
        deviceId: String,
        adbPath: String,
        key: String,
        value: Float,
    ): Result<Unit>

    /** Открыть системные настройки устройства для соответствующего toggle. */
    suspend fun openSettings(
        deviceId: String,
        adbPath: String,
        toggleId: QuickToggleId,
    ): Result<Unit>
}
