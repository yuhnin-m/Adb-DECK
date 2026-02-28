package com.adbdeck.feature.screentools.service

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_dialog_choose_directory_title
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_desktop_unsupported
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_file_not_found
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_folder_not_found
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_image_file_not_found
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_image_read_failed
import adbdeck.feature.screen_tools.generated.resources.screen_tools_error_open_unsupported
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.awt.Desktop
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

/**
 * Desktop-реализация [HostFileService].
 */
class DefaultHostFileService : HostFileService {
    private companion object {
        private val HOME_DIR: String = System.getProperty("user.home")
    }

    override fun defaultScreenshotDirectory(): String =
        Paths.get(HOME_DIR, "Pictures", "ADBDeck").toString()

    override fun defaultScreenrecordDirectory(): String =
        Paths.get(HOME_DIR, "Videos", "ADBDeck").toString()

    override suspend fun ensureDirectory(path: String): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            Files.createDirectories(Path.of(path))
        }
    }

    override suspend fun openFile(path: String): Result<Unit> = runCatchingPreserveCancellation {
        val fileNotFound = getString(Res.string.screen_tools_error_file_not_found, path)
        val desktopUnsupported = getString(Res.string.screen_tools_error_desktop_unsupported)
        val openUnsupported = getString(Res.string.screen_tools_error_open_unsupported)
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.isFile) error(fileNotFound)
            openWithDesktop(
                target = file,
                desktopUnsupportedMessage = desktopUnsupported,
                openUnsupportedMessage = openUnsupported,
            )
        }
    }

    override suspend fun openFolder(path: String): Result<Unit> = runCatchingPreserveCancellation {
        val folderNotFound = getString(Res.string.screen_tools_error_folder_not_found, path)
        val desktopUnsupported = getString(Res.string.screen_tools_error_desktop_unsupported)
        val openUnsupported = getString(Res.string.screen_tools_error_open_unsupported)
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.isDirectory) error(folderNotFound)
            openWithDesktop(
                target = folder,
                desktopUnsupportedMessage = desktopUnsupported,
                openUnsupportedMessage = openUnsupported,
            )
        }
    }

    override suspend fun selectDirectory(initialPath: String): Result<String?> = runCatchingPreserveCancellation {
        val dialogTitle = getString(Res.string.screen_tools_dialog_choose_directory_title)
        withContext(Dispatchers.IO) {
            val chooserInitialPath = File(initialPath)
                .takeIf { it.exists() }
                ?.absolutePath
                ?: HOME_DIR
            showOpenFileDialog(
                OpenFileDialogConfig(
                    title = dialogTitle,
                    initialPath = chooserInitialPath,
                    selectionMode = HostFileSelectionMode.DIRECTORIES_ONLY,
                    isAcceptAllFileFilterUsed = false,
                ),
            )
        }
    }

    override suspend fun copyImageToClipboard(path: String): Result<Unit> = runCatchingPreserveCancellation {
        val fileNotFound = getString(Res.string.screen_tools_error_image_file_not_found, path)
        val imageReadFailed = getString(Res.string.screen_tools_error_image_read_failed, path)
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.isFile) error(fileNotFound)

            val image = ImageIO.read(file) ?: error(imageReadFailed)
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(ImageTransferable(image), NoOpClipboardOwner)
        }
    }

    override fun isFile(path: String?): Boolean =
        path?.let { File(it).isFile } == true

    override fun isDirectory(path: String): Boolean =
        File(path).isDirectory

    /** Открывает файл/папку системным файловым менеджером или приложением. */
    private fun openWithDesktop(
        target: File,
        desktopUnsupportedMessage: String,
        openUnsupportedMessage: String,
    ) {
        if (!Desktop.isDesktopSupported()) {
            error(desktopUnsupportedMessage)
        }

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            error(openUnsupportedMessage)
        }

        desktop.open(target)
    }

    /**
     * Transferable для изображения в системном буфере обмена.
     */
    private class ImageTransferable(
        private val image: Image,
    ) : Transferable {

        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            flavor == DataFlavor.imageFlavor

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) {
                throw IllegalArgumentException("Unsupported DataFlavor: $flavor")
            }
            return image
        }
    }

    /** Пустой владелец буфера обмена. */
    private data object NoOpClipboardOwner : ClipboardOwner {
        override fun lostOwnership(
            clipboard: java.awt.datatransfer.Clipboard?,
            contents: Transferable?,
        ) = Unit
    }
}
