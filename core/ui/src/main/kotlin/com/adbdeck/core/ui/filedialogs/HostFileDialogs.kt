package com.adbdeck.core.ui.filedialogs

import java.awt.EventQueue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Режим выбора в системном файловом диалоге.
 */
enum class HostFileSelectionMode {
    FILES_ONLY,
    DIRECTORIES_ONLY,
    FILES_AND_DIRECTORIES,
}

/**
 * Фильтр файлов для системного диалога.
 *
 * @param description Текст фильтра, отображаемый в диалоге.
 * @param extensions  Список расширений без точки.
 */
data class HostFileDialogFilter(
    val description: String,
    val extensions: List<String>,
)

/**
 * Политика обработки расширения в диалоге сохранения.
 */
sealed interface SaveFileExtensionPolicy {
    /** Возвращать путь как есть, без автодобавления расширения. */
    data object KeepSelected : SaveFileExtensionPolicy

    /**
     * Добавлять [defaultExtension], если выбранное имя файла не оканчивается
     * ни на одно из [acceptedExtensions].
     */
    data class AppendIfMissing(
        val defaultExtension: String,
        val acceptedExtensions: Set<String> = setOf(defaultExtension),
    ) : SaveFileExtensionPolicy
}

/**
 * Конфигурация диалога открытия файла/директории.
 */
data class OpenFileDialogConfig(
    val title: String,
    val initialPath: String? = null,
    val selectionMode: HostFileSelectionMode = HostFileSelectionMode.FILES_ONLY,
    val filters: List<HostFileDialogFilter> = emptyList(),
    val isAcceptAllFileFilterUsed: Boolean = true,
)

/**
 * Конфигурация диалога сохранения.
 */
data class SaveFileDialogConfig(
    val title: String,
    val defaultFileName: String? = null,
    val initialPath: String? = null,
    val filters: List<HostFileDialogFilter> = emptyList(),
    val isAcceptAllFileFilterUsed: Boolean = true,
    val extensionPolicy: SaveFileExtensionPolicy = SaveFileExtensionPolicy.KeepSelected,
)

/**
 * Открывает системный диалог сохранения с расширенной конфигурацией.
 *
 * @return Абсолютный путь к выбранному файлу или `null`, если пользователь отменил выбор.
 */
fun showSaveFileDialog(config: SaveFileDialogConfig): String? = runOnEdt {
    val chooser = createFileChooser(initialPath = config.initialPath).apply {
        dialogTitle = config.title
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = config.isAcceptAllFileFilterUsed
        applyFilters(config.filters)
    }

    config.defaultFileName
        ?.takeIf { it.isNotBlank() }
        ?.let { chooser.selectedFile = File(it) }

    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
        return@runOnEdt null
    }

    val selectedFile = chooser.selectedFile ?: return@runOnEdt null
    when (val policy = config.extensionPolicy) {
        SaveFileExtensionPolicy.KeepSelected -> selectedFile.absolutePath
        is SaveFileExtensionPolicy.AppendIfMissing -> appendExtensionIfMissing(selectedFile, policy)
    }
}

/**
 * Открывает системный диалог выбора файла/директории с расширенной конфигурацией.
 *
 * @return Абсолютный путь к выбранному элементу или `null`, если пользователь отменил выбор.
 */
fun showOpenFileDialog(config: OpenFileDialogConfig): String? = runOnEdt {
    val chooser = createFileChooser(initialPath = config.initialPath).apply {
        dialogTitle = config.title
        fileSelectionMode = config.selectionMode.toChooserMode()
        isAcceptAllFileFilterUsed = config.isAcceptAllFileFilterUsed
        applyFilters(config.filters)
    }

    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

private fun createFileChooser(initialPath: String?): JFileChooser {
    val initialDirectory = resolveInitialDirectory(initialPath)
    return if (initialDirectory != null) JFileChooser(initialDirectory) else JFileChooser()
}

private fun resolveInitialDirectory(initialPath: String?): File? {
    val normalized = initialPath?.trim().orEmpty()
    if (normalized.isBlank()) return null

    val initialFile = File(normalized).takeIf { it.exists() } ?: return null
    return if (initialFile.isDirectory) initialFile else initialFile.parentFile
}

private fun JFileChooser.applyFilters(filters: List<HostFileDialogFilter>) {
    if (filters.isEmpty()) return

    resetChoosableFileFilters()
    var primaryFilter: FileNameExtensionFilter? = null
    filters.forEach { filter ->
        val normalizedExtensions = filter.extensions
            .map { it.trim().removePrefix(".") }
            .filter { it.isNotBlank() }
        if (normalizedExtensions.isEmpty()) return@forEach

        val swingFilter = FileNameExtensionFilter(
            filter.description,
            *normalizedExtensions.toTypedArray(),
        )
        addChoosableFileFilter(swingFilter)
        if (primaryFilter == null) {
            primaryFilter = swingFilter
        }
    }

    primaryFilter?.let { fileFilter = it }
}

private fun HostFileSelectionMode.toChooserMode(): Int = when (this) {
    HostFileSelectionMode.FILES_ONLY -> JFileChooser.FILES_ONLY
    HostFileSelectionMode.DIRECTORIES_ONLY -> JFileChooser.DIRECTORIES_ONLY
    HostFileSelectionMode.FILES_AND_DIRECTORIES -> JFileChooser.FILES_AND_DIRECTORIES
}

private fun appendExtensionIfMissing(
    selectedFile: File,
    policy: SaveFileExtensionPolicy.AppendIfMissing,
): String {
    val normalizedDefault = policy.defaultExtension
        .trim()
        .removePrefix(".")
        .ifBlank { "txt" }

    val normalizedAccepted = policy.acceptedExtensions
        .map { it.trim().removePrefix(".") }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf(normalizedDefault) }

    val hasAcceptedExtension = normalizedAccepted.any { ext ->
        selectedFile.name.endsWith(".$ext", ignoreCase = true)
    }
    return if (hasAcceptedExtension) {
        selectedFile.absolutePath
    } else {
        "${selectedFile.absolutePath}.$normalizedDefault"
    }
}

private inline fun <T> runOnEdt(crossinline block: () -> T): T {
    if (EventQueue.isDispatchThread()) {
        return block()
    }

    var result: Result<T>? = null
    EventQueue.invokeAndWait {
        result = runCatching(block)
    }
    return result?.getOrThrow() ?: error("EDT execution did not produce a result")
}
