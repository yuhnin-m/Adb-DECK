package com.adbdeck.feature.screentools.service

import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override fun defaultScreenshotDirectory(): String =
        Paths.get(homeDir(), "Pictures", "ADBDeck").toString()

    override fun defaultScreenrecordDirectory(): String =
        Paths.get(homeDir(), "Videos", "ADBDeck").toString()

    override suspend fun ensureDirectory(path: String): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            Files.createDirectories(Path.of(path))
        }
    }

    override suspend fun openFile(path: String): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.isFile) error("Файл не найден: $path")
            openWithDesktop(file)
        }
    }

    override suspend fun openFolder(path: String): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.isDirectory) error("Папка не найдена: $path")
            openWithDesktop(folder)
        }
    }

    override suspend fun copyImageToClipboard(path: String): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.isFile) error("Файл изображения не найден: $path")

            val image = ImageIO.read(file) ?: error("Не удалось прочитать изображение: $path")
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(ImageTransferable(image), NoOpClipboardOwner)
        }
    }

    override fun isFile(path: String?): Boolean =
        path?.let { File(it).isFile } == true

    override fun isDirectory(path: String): Boolean =
        File(path).isDirectory

    /** Возвращает домашнюю директорию текущего пользователя. */
    private fun homeDir(): String = System.getProperty("user.home")

    /** Открывает файл/папку системным файловым менеджером или приложением. */
    private fun openWithDesktop(target: File) {
        if (!Desktop.isDesktopSupported()) {
            error("Desktop API не поддерживается в текущем окружении")
        }

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            error("Операция открытия файла не поддерживается системой")
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
                throw IllegalArgumentException("Неподдерживаемый DataFlavor: $flavor")
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
