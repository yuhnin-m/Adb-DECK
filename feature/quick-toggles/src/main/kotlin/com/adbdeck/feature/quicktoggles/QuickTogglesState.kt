package com.adbdeck.feature.quicktoggles

/**
 * Идентификаторы quick-toggle элементов.
 */
enum class QuickToggleId {
    WIFI,
    MOBILE_DATA,
    BLUETOOTH,
    AIRPLANE_MODE,
    ANIMATIONS,
    STAY_AWAKE,
}

/**
 * Состояние toggle-элемента.
 */
enum class QuickToggleState {
    ON,
    OFF,
    CUSTOM,
    UNKNOWN,
}

/** Ключ `settings global` для `window_animation_scale`. */
const val ANIMATION_WINDOW_SCALE_KEY = "window_animation_scale"

/** Ключ `settings global` для `transition_animation_scale`. */
const val ANIMATION_TRANSITION_SCALE_KEY = "transition_animation_scale"

/** Ключ `settings global` для `animator_duration_scale`. */
const val ANIMATION_ANIMATOR_SCALE_KEY = "animator_duration_scale"

/**
 * Статус отдельного параметра анимаций.
 */
enum class AnimationScaleStatus {
    OK,
    ERROR,
    LOADING,
}

/**
 * Состояние одного параметра анимации.
 *
 * @param key Ключ параметра в `settings global`.
 * @param currentValue Текущее значение (`null` = unset/default).
 * @param draftValue Черновое значение для Slider/пресетов.
 * @param status Технический статус строки (`OK/ERROR/LOADING`).
 * @param error Текст ошибки для строки, если есть.
 */
data class AnimationScaleControl(
    val key: String,
    val currentValue: Float? = null,
    val draftValue: Float = 1f,
    val status: AnimationScaleStatus = AnimationScaleStatus.OK,
    val error: String? = null,
)

/**
 * Элемент quick-toggle списка.
 *
 * @param id Идентификатор toggle.
 * @param title Заголовок toggle.
 * @param state Текущее состояние (`ON/OFF/CUSTOM/UNKNOWN`).
 * @param canToggle Можно ли выполнять переключение.
 * @param isRunning Флаг выполнения операции toggle.
 * @param error Текст ошибки последней операции.
 * @param showOpenSettings Показывать кнопку "Open Settings" как fallback.
 * @param animationControls Состояние независимых параметров блока Animations.
 */
data class ToggleItem(
    val id: QuickToggleId,
    val title: String,
    val state: QuickToggleState = QuickToggleState.UNKNOWN,
    val canToggle: Boolean = true,
    val isRunning: Boolean = false,
    val error: String? = null,
    val showOpenSettings: Boolean = false,
    val animationControls: List<AnimationScaleControl> = emptyList(),
)

/**
 * Действие, требующее подтверждения пользователем.
 *
 * @param toggleId Идентификатор toggle.
 * @param targetState Целевое состояние после применения.
 */
data class PendingQuickToggleAction(
    val toggleId: QuickToggleId,
    val targetState: QuickToggleState,
)

/**
 * Краткосрочное сообщение верхнего уровня.
 */
data class QuickTogglesFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Полное состояние экрана Quick Toggles.
 */
data class QuickTogglesState(
    val activeDeviceId: String? = null,
    val items: List<ToggleItem> = defaultQuickToggleItems(),
    val isRefreshing: Boolean = false,
    val pendingAction: PendingQuickToggleAction? = null,
    val feedback: QuickTogglesFeedback? = null,
) {
    /**
     * `true`, если активное устройство доступно для ADB shell-команд.
     */
    val isDeviceAvailable: Boolean
        get() = !activeDeviceId.isNullOrBlank()
}

/**
 * Начальный список тумблеров в фиксированном порядке.
 */
fun defaultQuickToggleItems(): List<ToggleItem> {
    return listOf(
        ToggleItem(id = QuickToggleId.WIFI, title = ""),
        ToggleItem(id = QuickToggleId.MOBILE_DATA, title = ""),
        ToggleItem(id = QuickToggleId.BLUETOOTH, title = ""),
        ToggleItem(id = QuickToggleId.AIRPLANE_MODE, title = ""),
        ToggleItem(
            id = QuickToggleId.ANIMATIONS,
            title = "",
            animationControls = defaultAnimationScaleControls(),
        ),
        ToggleItem(id = QuickToggleId.STAY_AWAKE, title = ""),
    )
}

/**
 * Начальные состояния независимых параметров блока Animations.
 */
fun defaultAnimationScaleControls(): List<AnimationScaleControl> {
    return listOf(
        AnimationScaleControl(key = ANIMATION_WINDOW_SCALE_KEY),
        AnimationScaleControl(key = ANIMATION_TRANSITION_SCALE_KEY),
        AnimationScaleControl(key = ANIMATION_ANIMATOR_SCALE_KEY),
    )
}
