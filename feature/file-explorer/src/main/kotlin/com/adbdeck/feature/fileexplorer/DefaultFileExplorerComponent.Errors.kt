package com.adbdeck.feature.fileexplorer

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.file_explorer_error_operation_failed
import adbdeck.feature.file_explorer.generated.resources.file_explorer_error_read_device_directory
import adbdeck.feature.file_explorer.generated.resources.file_explorer_error_read_device_roots
import adbdeck.feature.file_explorer.generated.resources.file_explorer_error_read_local_directory
import adbdeck.feature.file_explorer.generated.resources.file_explorer_error_transfer_generic
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Преобразует [cause] в понятное локализованное сообщение для UI.
 *
 * Базовая часть сообщения определяется через [type], детализация причины
 * добавляется только если в исключении есть непустой текст.
 */
internal suspend fun DefaultFileExplorerComponent.resolveErrorMessage(
    type: FileExplorerErrorType,
    cause: Throwable?,
): String {
    val base = getString(type.messageRes)
    val details = cause?.message
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return base

    return "$base: $details"
}

private val FileExplorerErrorType.messageRes: StringResource
    get() = when (this) {
        FileExplorerErrorType.READ_LOCAL_DIRECTORY -> Res.string.file_explorer_error_read_local_directory
        FileExplorerErrorType.READ_DEVICE_DIRECTORY -> Res.string.file_explorer_error_read_device_directory
        FileExplorerErrorType.READ_DEVICE_ROOTS -> Res.string.file_explorer_error_read_device_roots
        FileExplorerErrorType.TRANSFER_FAILED -> Res.string.file_explorer_error_transfer_generic
        FileExplorerErrorType.OPERATION_FAILED -> Res.string.file_explorer_error_operation_failed
    }
