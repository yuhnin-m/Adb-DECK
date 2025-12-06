package com.adbdeck.core.adb.api.intents

/**
 * Параметры для запуска Deep Link (ACTION_VIEW + URI).
 *
 * @param uri         URI для запуска (обязательно).
 * @param packageName Пакет-приёмник (опционально).
 * @param component   Компонент activity (опционально, формат: `pkg/.Activity`).
 * @param action      Android-action (по умолчанию ACTION_VIEW).
 * @param category    Android-category (опционально).
 */
data class DeepLinkParams(
    val uri: String,
    val packageName: String = "",
    val component: String = "",
    val action: String = "android.intent.action.VIEW",
    val category: String = "",
)
