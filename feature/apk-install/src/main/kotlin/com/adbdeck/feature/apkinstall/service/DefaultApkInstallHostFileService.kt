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
    }

    override suspend fun selectApkFile(initialPath: String): Result<String?> = runCatchingPreserveCancellation {
        val dialogTitle = getString(Res.string.apk_install_dialog_choose_apk_title)
        val filterLabel = getString(Res.string.apk_install_dialog_apk_filter_label)
        withContext(Dispatchers.IO) {
            val chooser = JFileChooser(resolveInitialDirectory(initialPath)).apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = FileNameExtensionFilter(filterLabel, "apk")
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

            if (!file.isFile) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.FILE_NOT_FOUND,
                    originalPath = normalized,
                )
            }

            if (!file.name.endsWith(".apk", ignoreCase = true)) {
                return@withContext ApkFileValidationResult.Invalid(
                    reason = ApkFileValidationError.INVALID_EXTENSION,
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
