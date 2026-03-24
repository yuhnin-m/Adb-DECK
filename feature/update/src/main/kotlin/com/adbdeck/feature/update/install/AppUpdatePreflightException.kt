package com.adbdeck.feature.update.install

/**
 * Техническая ошибка preflight-проверки перед in-app установкой обновления.
 *
 * [reason] используется бизнес-слоем для выбора локализованного пользовательского текста.
 */
class AppUpdatePreflightException(
    val reason: AppUpdatePreflightFailureReason,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)

/**
 * Машиночитаемые причины отказа preflight-проверки.
 */
enum class AppUpdatePreflightFailureReason {
    UNSUPPORTED_PLATFORM_OR_ASSET,
    CURRENT_APP_BUNDLE_NOT_FOUND,
    CURRENT_APP_BUNDLE_INVALID,
    TARGET_DIRECTORY_MISSING,
    TARGET_DIRECTORY_NOT_WRITABLE,
}
