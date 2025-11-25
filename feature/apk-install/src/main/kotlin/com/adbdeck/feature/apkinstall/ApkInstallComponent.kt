package com.adbdeck.feature.apkinstall

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт feature установки APK.
 */
interface ApkInstallComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<ApkInstallState>

    /** Изменить путь к APK-файлу. */
    fun onApkPathChanged(path: String)

    /** Запустить установку выбранного APK. */
    fun onInstallApk()

    /** Очистить лог установки. */
    fun onClearLog()

    /** Скрыть feedback-сообщение. */
    fun onDismissFeedback()
}
