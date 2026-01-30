package com.adbdeck.feature.apkinstall.service

import adbdeck.feature.apk_install.generated.resources.Res
import adbdeck.feature.apk_install.generated.resources.apk_install_dialog_apk_filter_label
import adbdeck.feature.apk_install.generated.resources.apk_install_dialog_choose_apk_title
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import java.awt.EventQueue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
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
            val chooser = JFileChooser(resolveInitialDirectory(initialPath)).apply {
                fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                isAcceptAllFileFilterUsed = true
                fileFilter = FileNameExtensionFilter(
                    filterLabel,
                    *INSTALLABLE_EXTENSIONS.toTypedArray(),
                )
                this.dialogTitle = dialogTitle
            }

            var selectedPath: String? = null
            EventQueue.invokeAndWait {
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selectedPath = chooser.selectedFile?.absolutePath
                }
            }
            selectedPath
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
