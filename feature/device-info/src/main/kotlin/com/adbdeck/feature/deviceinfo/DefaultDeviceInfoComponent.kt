package com.adbdeck.feature.deviceinfo

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.deviceinfo.service.DeviceInfoService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [DeviceInfoComponent].
 *
 * Архитектура:
 * 1. Подписка на [DeviceManager.selectedDeviceFlow] и синхронизация active device.
 * 2. Ручной refresh запускает параллельную загрузку независимых секций.
 * 3. Каждая секция обновляет state отдельно (loading/success/error).
 * 4. Экспорт JSON работает с текущим состоянием секций без повторных ADB-вызовов.
 *
 * @param componentContext Decompose-контекст.
 * @param deviceManager Менеджер активного устройства.
 * @param settingsRepository Репозиторий настроек (путь к adb).
 * @param deviceInfoService Сервис загрузки секций Device Info.
 */
class DefaultDeviceInfoComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val deviceInfoService: DeviceInfoService,
) : DeviceInfoComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(DeviceInfoState())
    override val state: StateFlow<DeviceInfoState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var feedbackJob: Job? = null

    private var refreshRevision: Long = 0L
    private var lastObservedDeviceId: String? = null

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                val availableDeviceId = device
                    ?.takeIf { it.state == DeviceState.DEVICE }
                    ?.deviceId

                if (availableDeviceId == lastObservedDeviceId) {
                    return@collect
                }

                lastObservedDeviceId = availableDeviceId
                refreshRevision++
                refreshJob?.cancel()

                val sections = if (availableDeviceId != null) {
                    val message = getString(Res.string.device_info_section_manual_refresh_required)
                    defaultDeviceInfoSections(DeviceInfoSectionLoadState.Error(message))
                } else {
                    defaultDeviceInfoSections()
                }

                _state.update {
                    it.copy(
                        activeDeviceId = availableDeviceId,
                        sections = sections,
                        isRefreshing = false,
                        lastUpdatedAtMillis = null,
                    )
                }
            }
        }
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    override fun onRefresh() {
        val deviceId = _state.value.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.device_info_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        refreshJob?.cancel()
        val revision = ++refreshRevision

        refreshJob = scope.launch {
            _state.update {
                it.copy(
                    isRefreshing = true,
                    sections = defaultDeviceInfoSections(DeviceInfoSectionLoadState.Loading),
                )
            }

            val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

            val sectionJobs = DeviceInfoSectionKind.entries.map { section ->
                launch {
                    val result = deviceInfoService.loadSection(
                        section = section,
                        deviceId = deviceId,
                        adbPath = adbPath,
                    )

                    if (!isRefreshValid(revision, deviceId)) return@launch

                    val loadState = result.fold(
                        onSuccess = { DeviceInfoSectionLoadState.Success(it) },
                        onFailure = { error ->
                            val fallback = getString(Res.string.device_info_section_error_unknown)
                            DeviceInfoSectionLoadState.Error(
                                message = error.message?.trim().takeUnless { it.isNullOrBlank() } ?: fallback,
                            )
                        },
                    )

                    updateSectionState(section = section, loadState = loadState)
                }
            }

            sectionJobs.joinAll()

            if (!isRefreshValid(revision, deviceId)) return@launch

            _state.update {
                it.copy(
                    isRefreshing = false,
                    lastUpdatedAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    // ── Export ───────────────────────────────────────────────────────────────

    override fun onExportJson(path: String) {
        val normalizedPath = path.trim()
        if (normalizedPath.isBlank()) return

        val snapshot = _state.value
        val deviceId = snapshot.activeDeviceId
        if (deviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.device_info_feedback_device_unavailable,
                isError = true,
            )
            return
        }

        if (snapshot.isExportingJson) return

        scope.launch {
            _state.update { it.copy(isExportingJson = true) }

            val result = runCatchingNonCancellation {
                val finalPath = ensureJsonPath(normalizedPath)
                val payload = buildExportJson(
                    deviceId = deviceId,
                    sections = snapshot.sections,
                )

                withContext(Dispatchers.IO) {
                    val file = File(finalPath)
                    file.parentFile?.mkdirs()
                    file.writeText(payload)
                }

                finalPath
            }

            _state.update { it.copy(isExportingJson = false) }

            result.fold(
                onSuccess = { savedPath ->
                    showFeedbackResource(
                        messageRes = Res.string.device_info_feedback_export_success,
                        isError = false,
                        savedPath,
                    )
                },
                onFailure = { error ->
                    val details = error.message.orEmpty().ifBlank {
                        getString(Res.string.device_info_section_error_unknown)
                    }
                    showFeedbackResource(
                        messageRes = Res.string.device_info_feedback_export_failed,
                        isError = true,
                        details,
                    )
                },
            )
        }
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(feedback = null) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateSectionState(
        section: DeviceInfoSectionKind,
        loadState: DeviceInfoSectionLoadState,
    ) {
        _state.update { current ->
            current.copy(
                sections = current.sections.map { sectionState ->
                    if (sectionState.kind == section) {
                        sectionState.copy(state = loadState)
                    } else {
                        sectionState
                    }
                }
            )
        }
    }

    private fun isRefreshValid(revision: Long, deviceId: String): Boolean {
        return refreshRevision == revision && _state.value.activeDeviceId == deviceId
    }

    private fun ensureJsonPath(path: String): String {
        return if (path.endsWith(".json", ignoreCase = true)) path else "$path.json"
    }

    private fun buildExportJson(
        deviceId: String,
        sections: List<DeviceInfoSection>,
    ): String {
        val root = buildJsonObject {
            put("schemaVersion", JsonPrimitive(1))
            put("timestamp", JsonPrimitive(Instant.now().toString()))
            put("deviceId", JsonPrimitive(deviceId))
            put(
                "sections",
                buildJsonArray {
                    sections.forEach { section ->
                        add(sectionToJson(section))
                    }
                }
            )
        }

        return Json {
            prettyPrint = true
        }.encodeToString(JsonObject.serializer(), root)
    }

    private fun sectionToJson(section: DeviceInfoSection): JsonObject {
        return buildJsonObject {
            put("id", JsonPrimitive(section.kind.id))
            put("title", JsonPrimitive(section.kind.exportTitle))

            when (val sectionState = section.state) {
                is DeviceInfoSectionLoadState.Loading -> {
                    put("state", JsonPrimitive("loading"))
                    put("rows", JsonArray(emptyList()))
                }

                is DeviceInfoSectionLoadState.Error -> {
                    put("state", JsonPrimitive("error"))
                    put("error", JsonPrimitive(sectionState.message))
                    put("rows", JsonArray(emptyList()))
                }

                is DeviceInfoSectionLoadState.Success -> {
                    put("state", JsonPrimitive("success"))
                    put(
                        "rows",
                        buildJsonArray {
                            sectionState.rows.forEach { row ->
                                add(
                                    buildJsonObject {
                                        put("key", JsonPrimitive(row.key))
                                        put("value", JsonPrimitive(row.value))
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update {
            it.copy(
                feedback = DeviceInfoFeedback(
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
            val message = getString(messageRes, *args)
            showFeedback(message = message, isError = isError)
        }
    }

    private suspend fun <T> runCatchingNonCancellation(
        block: suspend () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
