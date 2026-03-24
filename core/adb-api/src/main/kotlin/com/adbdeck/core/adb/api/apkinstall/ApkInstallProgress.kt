package com.adbdeck.core.adb.api.apkinstall

/**
 * Прогресс установки APK.
 *
 * @param progress Значение `0f..1f`, либо `null`, если процент недоступен.
 * @param event Типизированное событие прогресса.
 */
data class ApkInstallProgress(
    val progress: Float?,
    val event: ApkInstallProgressEvent,
)

/**
 * Событие прогресса установки APK.
 */
sealed interface ApkInstallProgressEvent {

    /**
     * Шаг пайплайна установки, который можно локализовать в UI.
     */
    data class Stage(
        val stage: ApkInstallStage,
    ) : ApkInstallProgressEvent

    /**
     * Сырая строка вывода инструментов (adb / bundletool) для технического лога.
     */
    data class OutputLine(
        val line: String,
    ) : ApkInstallProgressEvent
}

/**
 * Типы шагов пайплайна установки APK.
 */
enum class ApkInstallStage {
    USING_BUNDLETOOL_INSTALL_APKS,
    BUNDLETOOL_FAILED_FALLBACK_TO_ADB,
    APKS_WITHOUT_TOC_FALLBACK_TO_ADB,
    BUILDING_APKS_FROM_AAB,
    INSTALLING_GENERATED_APKS,
}
