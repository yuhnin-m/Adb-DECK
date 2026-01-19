package com.adbdeck.feature.fileexplorer

/**
 * Типизированные категории ошибок File Explorer.
 *
 * Используются в компоненте для единообразного маппинга в локализованные
 * сообщения UI вместо прямого проброса `Throwable.message`.
 */
internal enum class FileExplorerErrorType {
    READ_LOCAL_DIRECTORY,
    READ_DEVICE_DIRECTORY,
    READ_DEVICE_ROOTS,
    TRANSFER_FAILED,
    OPERATION_FAILED,
}
