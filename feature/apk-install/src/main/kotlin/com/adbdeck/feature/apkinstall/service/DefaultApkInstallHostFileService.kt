package com.adbdeck.feature.apkinstall.service

import adbdeck.feature.apk_install.generated.resources.Res
import adbdeck.feature.apk_install.generated.resources.apk_install_dialog_apk_filter_label
import adbdeck.feature.apk_install.generated.resources.apk_install_dialog_choose_apk_title
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.core.ui.filedialogs.HostFileDialogFilter
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/**
 * Desktop-реализация [ApkInstallHostFileService].
 */
class DefaultApkInstallHostFileService : ApkInstallHostFileService {

    private companion object {
        private val HOME_DIR: String = System.getProperty("user.home")
        private val INSTALLABLE_EXTENSIONS = setOf("apk", "aab", "apks", "xapk")
    }

    override suspend fun selectApkFile(initialPath: String): Result<String?> = runCatchingPreserveCancellation {
        val dialogTitle = getString(Res.string.apk_install_dialog_choose_apk_title)
        val filterLabel = getString(Res.string.apk_install_dialog_apk_filter_label)
        withContext(Dispatchers.IO) {
            val initialDirectory = resolveInitialDirectory(initialPath).absolutePath
            showOpenFileDialog(
                OpenFileDialogConfig(
                    title = dialogTitle,
                    initialPath = initialDirectory,
                    selectionMode = HostFileSelectionMode.FILES_AND_DIRECTORIES,
                    filters = listOf(
                        HostFileDialogFilter(
                            description = filterLabel,
                            extensions = INSTALLABLE_EXTENSIONS.toList(),
                        ),
                    ),
                    isAcceptAllFileFilterUsed = true,
                ),
            )
        }
    }

    override suspend fun validateApkPath(path: String): ApkFileValidationResult =
        withContext(Dispatchers.IO) {
            val normalized = path.trim()
            if (normalized.isBlank()) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.EMPTY_PATH,
                    originalPath = path,
                )
            }

            val file = runCatching { File(normalized) }.getOrElse {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.IO_ACCESS_ERROR,
                    originalPath = normalized,
                )
            }

            if (!file.exists()) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.FILE_NOT_FOUND,
                    originalPath = normalized,
                )
            }

            if (file.isDirectory) {
                val hasSplitApkFiles = file.walkTopDown()
                    .maxDepth(3)
                    .any { child ->
                        child.isFile && child.name.endsWith(".apk", ignoreCase = true)
                    }
                if (!hasSplitApkFiles) {
                    return@withContext ApkFileValidationResult.Invalid(
                        reason = ApkFileValidationError.UNSUPPORTED_FORMAT,
                        originalPath = normalized,
                    )
                }
                return@withContext ApkFileValidationResult.Valid(
                    absolutePath = file.absolutePath,
                    fileName = file.name.ifBlank { file.absolutePath },
                )
            }

            if (!file.isFile) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.FILE_NOT_FOUND,
                    originalPath = normalized,
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in INSTALLABLE_EXTENSIONS) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.UNSUPPORTED_FORMAT,
                    originalPath = normalized,
                )
            }

            ApkFileValidationResult.Valid(
                absolutePath = file.absolutePath,
                fileName = file.name,
            )
        }

    /** Определяет стартовую директорию для file chooser. */
    private fun resolveInitialDirectory(initialPath: String): File {
        val initialFile = File(initialPath).takeIf { it.exists() }
        return when {
            initialFile == null -> File(HOME_DIR)
            initialFile.isDirectory -> initialFile
            else -> initialFile.parentFile ?: File(HOME_DIR)
        }
    }
}
