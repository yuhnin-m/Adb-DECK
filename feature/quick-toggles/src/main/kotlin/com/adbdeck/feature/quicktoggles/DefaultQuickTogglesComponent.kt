package com.adbdeck.feature.quicktoggles

import adbdeck.feature.quick_toggles.generated.resources.Res
import adbdeck.feature.quick_toggles.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.quicktoggles.service.AnimationScaleValue
import com.adbdeck.feature.quicktoggles.service.QuickToggleStatusSnapshot
import com.adbdeck.feature.quicktoggles.service.QuickTogglesService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [QuickTogglesComponent].
 */
class DefaultQuickTogglesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val quickTogglesService: QuickTogglesService,
) : QuickTogglesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(QuickTogglesState())
    override val state: StateFlow<QuickTogglesState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var feedbackJob: Job? = null
    private val toggleJobs = linkedMapOf<QuickToggleId, Job>()
    private val animationJobs = linkedMapOf<String, Job>()
    private val settingsJobs = linkedMapOf<QuickToggleId, Job>()
    private val feedbackMutex = Mutex()

    private var revision: Long = 0L
    private var lastObservedDeviceId: String? = null

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { selectedDevice ->
                val availableDeviceId = selectedDevice
                    ?.takeIf { it.state == DeviceState.DEVICE }
                    ?.deviceId
                if (availableDeviceId == lastObservedDeviceId) {
                    return@collect
                }

                lastObservedDeviceId = availableDeviceId
                revision++
                refreshJob?.cancel()
                toggleJobs.values.forEach(Job::cancel)
                animationJobs.values.forEach(Job::cancel)
                settingsJobs.values.forEach(Job::cancel)
                toggleJobs.clear()
                animationJobs.clear()
                settingsJobs.clear()

                _state.update { current ->
                    current.copy(
                        activeDeviceId = availableDeviceId,
                        items = current.items.map { item ->
                            item.copy(
                                state = QuickToggleState.UNKNOWN,
                                canToggle = availableDeviceId != null,
                                isRunning = false,
                                error = null,
                                showOpenSettings = false,
                                animationControls = defaultAnimationControlsFor(item.id),
                            )
                        },
                        isRefreshing = false,
                        pendingAction = null,
                    )
                }

                if (!availableDeviceId.isNullOrBlank()) {
                    refreshStatuses(showFeedback = false)
                }
            }
        }
    }

    override fun onRefresh() {
        refreshStatuses(showFeedback = true)
    }

    override fun onRefreshToggle(toggleId: QuickToggleId) {
        if (toggleId == QuickToggleId.ANIMATIONS) {
            refreshAnimationScales(showFeedbackOnFailure = true)
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        toggleJobs[toggleId]?.cancel()
        setItemRunning(toggleId = toggleId, running = true, clearError = true)

        val job = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()
            val result = quickTogglesService.readStatus(
                deviceId = deviceId,
                adbPath = adbPath,
                toggleId = toggleId,
            )
            if (_state.value.activeDeviceId != deviceId) return@launch

            _state.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        if (item.id != toggleId) return@map item
                        item.copy(
                            state = result.state,
                            canToggle = current.isDeviceAvailable,
                            isRunning = false,
                            error = if (result.state == QuickToggleState.UNKNOWN) result.error else null,
                            showOpenSettings = false,
                        )
                    },
                )
            }
        }

        toggleJobs[toggleId] = job
    }

    override fun onRequestToggle(toggleId: QuickToggleId, targetState: QuickToggleState) {
        if (targetState != QuickToggleState.ON && targetState != QuickToggleState.OFF) return

        val currentState = _state.value
        if (!currentState.isDeviceAvailable) {
            showFeedbackResource(
                messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        if (toggleId.requiresConfirmation()) {
            _state.update {
                it.copy(
                    pendingAction = PendingQuickToggleAction(
                        toggleId = toggleId,
                        targetState = targetState,
                    )
                )
            }
            return
        }

        performToggle(toggleId = toggleId, targetState = targetState)
    }

    override fun onAnimationDraftChanged(key: String, value: Float) {
        val bounded = value.coerceIn(0f, 10f)
        _state.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id != QuickToggleId.ANIMATIONS) return@map item
                    item.copy(
                        animationControls = ensureAnimationControls(item).map { control ->
                            if (control.key == key) {
                                control.copy(draftValue = bounded)
                            } else {
                                control
                            }
                        }
                    )
                }
            )
        }
    }

    override fun onSetAnimationScale(key: String) {
        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        animationJobs[key]?.cancel()
        val draftValue = currentAnimationControl(key)?.draftValue ?: 1f

        _state.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id != QuickToggleId.ANIMATIONS) return@map item
                    item.copy(
                        error = null,
                        showOpenSettings = false,
                        animationControls = ensureAnimationControls(item).map { control ->
                            if (control.key == key) {
                                control.copy(
                                    status = AnimationScaleStatus.LOADING,
                                    error = null,
                                )
                            } else {
                                control
                            }
                        },
                    )
                }
            )
        }

        val job = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()
            val setResult = quickTogglesService.setAnimationScale(
                deviceId = deviceId,
                adbPath = adbPath,
                key = key,
                value = draftValue,
            )

            // Read-back обязателен после set
            val readBack = quickTogglesService.readAnimationScales(
                deviceId = deviceId,
                adbPath = adbPath,
            )
            if (_state.value.activeDeviceId != deviceId) return@launch

            val writeError = setResult.exceptionOrNull()?.message
            _state.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        if (item.id != QuickToggleId.ANIMATIONS) return@map item

                        val mergedControls = mergeAnimationControls(
                            existing = ensureAnimationControls(item),
                            values = readBack,
                        )
                        val controlsWithWriteState = if (!writeError.isNullOrBlank()) {
                            mergedControls.map { control ->
                                if (control.key == key) {
                                    control.copy(
                                        status = AnimationScaleStatus.ERROR,
                                        error = writeError,
                                    )
                                } else {
                                    control
                                }
                            }
                        } else {
                            mergedControls
                        }

                        val keyReadError = controlsWithWriteState.firstOrNull { it.key == key }?.error
                        val finalError = writeError ?: keyReadError

                        item.copy(
                            state = deriveAnimationState(controlsWithWriteState),
                            canToggle = current.isDeviceAvailable,
                            animationControls = controlsWithWriteState,
                            error = finalError,
                            showOpenSettings = !finalError.isNullOrBlank(),
                        )
                    }
                )
            }

            if (!writeError.isNullOrBlank()) {
                val title = toggleTitle(QuickToggleId.ANIMATIONS)
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_toggle_failed,
                    isError = true,
                    title,
                    writeError,
                )
            }
        }

        animationJobs[key] = job
    }

    override fun onConfirmToggle() {
        val action = _state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null) }
        performToggle(toggleId = action.toggleId, targetState = action.targetState)
    }

    override fun onCancelToggle() {
        _state.update { it.copy(pendingAction = null) }
    }

    override fun onOpenSettings(toggleId: QuickToggleId) {
        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        settingsJobs[toggleId]?.cancel()
        setItemRunning(toggleId = toggleId, running = true)
        val job = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()
            val result = quickTogglesService.openSettings(
                deviceId = deviceId,
                adbPath = adbPath,
                toggleId = toggleId,
            )
            setItemRunning(toggleId = toggleId, running = false)

            val title = toggleTitle(toggleId)
            result.fold(
                onSuccess = {
                    showFeedbackResource(
                        messageRes = Res.string.quick_toggles_feedback_open_settings_success,
                        isError = false,
                        title,
                    )
                },
                onFailure = { error ->
                    showFeedbackResource(
                        messageRes = Res.string.quick_toggles_feedback_open_settings_failed,
                        isError = true,
                        title,
                        error.message.orEmpty().ifBlank {
                            getString(Res.string.quick_toggles_error_unknown)
                        },
                    )
                },
            )
        }
        settingsJobs[toggleId] = job
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(feedback = null) }
    }

    /**
     * Ручное или стартовое обновление всех статусов quick-toggle элементов.
     *
     * @param showFeedback Показывать итоговый banner с результатом чтения.
     */
    private fun refreshStatuses(showFeedback: Boolean) {
        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            if (showFeedback) {
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                    isError = true,
                )
            }
            return
        }

        refreshJob?.cancel()
        val refreshRevision = ++revision

        refreshJob = scope.launch {
            _state.update { current ->
                current.copy(
                    isRefreshing = true,
                    items = current.items.map { item ->
                        if (item.id == QuickToggleId.ANIMATIONS) {
                            item.copy(
                                error = null,
                                showOpenSettings = false,
                                animationControls = ensureAnimationControls(item).map { control ->
                                    control.copy(
                                        status = AnimationScaleStatus.LOADING,
                                        error = null,
                                    )
                                },
                            )
                        } else {
                            item.copy(
                                error = null,
                                showOpenSettings = false,
                                isRunning = false,
                            )
                        }
                    },
                )
            }

            val adbPath = settingsRepository.resolvedAdbPath()
            val snapshot = quickTogglesService.readStatuses(
                deviceId = deviceId,
                adbPath = adbPath,
            )
            if (!isRevisionValid(refreshRevision, deviceId)) return@launch

            _state.update { current ->
                current.copy(
                    isRefreshing = false,
                    items = applySnapshot(
                        items = current.items,
                        snapshot = snapshot,
                        isDeviceAvailable = current.isDeviceAvailable,
                    ),
                )
            }

            if (!showFeedback) return@launch

            val hasKnownStates = snapshot.states.values.any { it != QuickToggleState.UNKNOWN }
            if (hasKnownStates) {
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_refresh_success,
                    isError = false,
                )
            } else {
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_refresh_failed,
                    isError = true,
                )
            }
        }
    }

    private fun performToggle(
        toggleId: QuickToggleId,
        targetState: QuickToggleState,
    ) {
        val deviceId = _state.value.activeDeviceId ?: return
        val previous = toggleJobs[toggleId]
        previous?.cancel()

        setItemRunning(toggleId = toggleId, running = true, clearError = true)

        val job = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()
            val toggleResult = quickTogglesService.setToggle(
                deviceId = deviceId,
                adbPath = adbPath,
                toggleId = toggleId,
                targetState = targetState,
            )
            val readResult = quickTogglesService.readStatus(
                deviceId = deviceId,
                adbPath = adbPath,
                toggleId = toggleId,
            )
            if (_state.value.activeDeviceId != deviceId) return@launch

            val actualState = readResult.state
            val isApplied = actualState == targetState
            val fallbackReason = if (isApplied) {
                null
            } else {
                val toggleError = toggleResult.exceptionOrNull()?.message?.trim().orEmpty()
                val readError = readResult.error.orEmpty()
                when {
                    toggleError.isNotEmpty() -> toggleError
                    readError.isNotEmpty() -> readError
                    else -> getString(
                        Res.string.quick_toggles_error_not_applied,
                        stateLabel(targetState),
                    )
                }
            }

            _state.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        if (item.id != toggleId) {
                            item
                        } else if (item.id == QuickToggleId.ANIMATIONS) {
                            val mergedControls = mergeAnimationControls(
                                existing = ensureAnimationControls(item),
                                values = readResult.animationValues,
                            )
                            val firstError = mergedControls
                                .firstOrNull { it.status == AnimationScaleStatus.ERROR }
                                ?.error
                            item.copy(
                                state = deriveAnimationState(mergedControls),
                                canToggle = current.isDeviceAvailable,
                                isRunning = false,
                                error = fallbackReason ?: firstError,
                                showOpenSettings = !isApplied,
                                animationControls = mergedControls,
                            )
                        } else {
                            item.copy(
                                state = actualState,
                                canToggle = current.isDeviceAvailable,
                                isRunning = false,
                                error = fallbackReason,
                                showOpenSettings = !isApplied,
                            )
                        }
                    },
                )
            }

            val title = toggleTitle(toggleId)
            if (isApplied) {
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_toggle_success,
                    isError = false,
                    title,
                    stateLabel(targetState),
                )
            } else {
                showFeedbackResource(
                    messageRes = Res.string.quick_toggles_feedback_toggle_failed,
                    isError = true,
                    title,
                    fallbackReason.orEmpty(),
                )
            }
        }

        toggleJobs[toggleId] = job
    }

    private fun refreshAnimationScales(showFeedbackOnFailure: Boolean) {
        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.quick_toggles_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        animationJobs.values.forEach(Job::cancel)
        animationJobs.clear()

        _state.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id != QuickToggleId.ANIMATIONS) return@map item
                    item.copy(
                        error = null,
                        showOpenSettings = false,
                        animationControls = ensureAnimationControls(item).map { control ->
                            control.copy(
                                status = AnimationScaleStatus.LOADING,
                                error = null,
                            )
                        },
                    )
                }
            )
        }

        val job = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()
            val values = quickTogglesService.readAnimationScales(
                deviceId = deviceId,
                adbPath = adbPath,
            )
            if (_state.value.activeDeviceId != deviceId) return@launch

            _state.update { current ->
                current.copy(
                    items = current.items.map { item ->
                        if (item.id != QuickToggleId.ANIMATIONS) return@map item

                        val mergedControls = mergeAnimationControls(
                            existing = ensureAnimationControls(item),
                            values = values,
                        )
                        val firstError = mergedControls.firstOrNull { it.status == AnimationScaleStatus.ERROR }?.error

                        item.copy(
                            state = deriveAnimationState(mergedControls),
                            canToggle = current.isDeviceAvailable,
                            animationControls = mergedControls,
                            error = firstError,
                            showOpenSettings = false,
                        )
                    }
                )
            }

            if (showFeedbackOnFailure) {
                val hasError = values.any { !it.error.isNullOrBlank() }
                if (hasError) {
                    val title = toggleTitle(QuickToggleId.ANIMATIONS)
                    val errorText = values.firstOrNull { !it.error.isNullOrBlank() }?.error.orEmpty()
                    showFeedbackResource(
                        messageRes = Res.string.quick_toggles_feedback_toggle_failed,
                        isError = true,
                        title,
                        errorText,
                    )
                }
            }
        }

        animationJobs["refresh_all"] = job
    }

    private fun applySnapshot(
        items: List<ToggleItem>,
        snapshot: QuickToggleStatusSnapshot,
        isDeviceAvailable: Boolean,
    ): List<ToggleItem> {
        return items.map { item ->
            if (item.id == QuickToggleId.ANIMATIONS) {
                val values = snapshot.animationValues[item.id].orEmpty()
                val mergedControls = mergeAnimationControls(
                    existing = ensureAnimationControls(item),
                    values = values,
                )
                val firstError = mergedControls.firstOrNull { it.status == AnimationScaleStatus.ERROR }?.error

                item.copy(
                    state = deriveAnimationState(mergedControls),
                    canToggle = isDeviceAvailable,
                    isRunning = false,
                    error = firstError,
                    showOpenSettings = false,
                    animationControls = mergedControls,
                )
            } else {
                val newState = snapshot.states[item.id] ?: item.state
                val readError = snapshot.readErrors[item.id]
                item.copy(
                    state = newState,
                    canToggle = isDeviceAvailable,
                    isRunning = false,
                    error = if (newState == QuickToggleState.UNKNOWN) readError else null,
                    showOpenSettings = false,
                )
            }
        }
    }

    private fun mergeAnimationControls(
        existing: List<AnimationScaleControl>,
        values: List<AnimationScaleValue>,
    ): List<AnimationScaleControl> {
        val valueMap = values.associateBy { it.key }
        return existing.map { control ->
            val source = valueMap[control.key] ?: return@map control.copy(
                status = AnimationScaleStatus.OK,
                error = null,
            )

            val status = if (source.error.isNullOrBlank()) {
                AnimationScaleStatus.OK
            } else {
                AnimationScaleStatus.ERROR
            }

            control.copy(
                currentValue = source.value,
                draftValue = source.value ?: control.draftValue,
                status = status,
                error = source.error,
            )
        }
    }

    private fun deriveAnimationState(controls: List<AnimationScaleControl>): QuickToggleState {
        if (controls.any { it.status == AnimationScaleStatus.ERROR }) {
            return QuickToggleState.UNKNOWN
        }

        val values = controls.map { it.currentValue }
        return when {
            values.all { it != null && it.approxEquals(0f) } -> QuickToggleState.OFF
            values.all { it != null && it.approxEquals(1f) } -> QuickToggleState.ON
            else -> QuickToggleState.CUSTOM
        }
    }

    private fun currentAnimationControl(key: String): AnimationScaleControl? {
        val animationsItem = _state.value.items.firstOrNull { it.id == QuickToggleId.ANIMATIONS } ?: return null
        return ensureAnimationControls(animationsItem).firstOrNull { it.key == key }
    }

    private fun setItemRunning(
        toggleId: QuickToggleId,
        running: Boolean,
        clearError: Boolean = false,
    ) {
        _state.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id != toggleId) return@map item
                    item.copy(
                        isRunning = running,
                        error = if (clearError) null else item.error,
                        showOpenSettings = if (clearError) false else item.showOpenSettings,
                    )
                }
            )
        }
    }

    private fun isRevisionValid(
        expectedRevision: Long,
        expectedDeviceId: String,
    ): Boolean {
        return revision == expectedRevision && _state.value.activeDeviceId == expectedDeviceId
    }

    private fun QuickToggleId.requiresConfirmation(): Boolean {
        return this == QuickToggleId.WIFI || this == QuickToggleId.AIRPLANE_MODE
    }

    private fun ensureAnimationControls(item: ToggleItem): List<AnimationScaleControl> {
        return if (item.animationControls.isNotEmpty()) {
            item.animationControls
        } else {
            defaultAnimationScaleControls()
        }
    }

    private fun defaultAnimationControlsFor(toggleId: QuickToggleId): List<AnimationScaleControl> {
        return if (toggleId == QuickToggleId.ANIMATIONS) {
            defaultAnimationScaleControls()
        } else {
            emptyList()
        }
    }

    private suspend fun toggleTitle(toggleId: QuickToggleId): String {
        val titleRes = when (toggleId) {
            QuickToggleId.WIFI -> Res.string.quick_toggles_toggle_wifi
            QuickToggleId.MOBILE_DATA -> Res.string.quick_toggles_toggle_mobile_data
            QuickToggleId.BLUETOOTH -> Res.string.quick_toggles_toggle_bluetooth
            QuickToggleId.AIRPLANE_MODE -> Res.string.quick_toggles_toggle_airplane_mode
            QuickToggleId.ANIMATIONS -> Res.string.quick_toggles_toggle_animations
            QuickToggleId.STAY_AWAKE -> Res.string.quick_toggles_toggle_stay_awake
        }
        return getString(titleRes)
    }

    private suspend fun stateLabel(state: QuickToggleState): String {
        val labelRes = when (state) {
            QuickToggleState.ON -> Res.string.quick_toggles_state_on
            QuickToggleState.OFF -> Res.string.quick_toggles_state_off
            QuickToggleState.CUSTOM -> Res.string.quick_toggles_state_custom
            QuickToggleState.UNKNOWN -> Res.string.quick_toggles_state_unknown
        }
        return getString(labelRes)
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update {
            it.copy(
                feedback = QuickTogglesFeedback(
                    message = message,
                    isError = isError,
                )
            )
        }

        feedbackJob = scope.launch {
            delay(3_000)
            _state.update { current ->
                if (current.feedback?.message == message) {
                    current.copy(feedback = null)
                } else {
                    current
                }
            }
        }
    }

    private fun showFeedbackResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        scope.launch {
            feedbackMutex.withLock {
                val message = getString(messageRes, *args)
                showFeedback(message = message, isError = isError)
            }
        }
    }
}
