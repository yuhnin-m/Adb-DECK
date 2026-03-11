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

    /** Выбрать APK-файл через системный file chooser. */
    fun onPickApkFile()

    /**
     * Обработать путь APK, полученный из drag&drop.
     *
     * Этот callback может приходить не с Compose-потока, поэтому реализация
     * должна безопасно переключаться на основной scope компонента.
     */
    fun onApkPathDropped(path: String)

    /** Переключить установку test-only пакетов (`adb install -t`). */
    fun onAllowTestOnlyChanged(allow: Boolean)

    /** Запустить установку выбранного APK. */
    fun onInstallApk()

    /** Скопировать текущий текст результата установки в clipboard. */
    fun onCopyStatusResult()

    /** Очистить лог установки. */
    fun onClearLog()

    /** Скрыть feedback-сообщение. */
    fun onDismissFeedback()
}
