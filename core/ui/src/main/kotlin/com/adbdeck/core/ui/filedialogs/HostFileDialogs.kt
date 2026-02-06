package com.adbdeck.core.ui.filedialogs

import java.awt.EventQueue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Открыть диалог сохранения файла на хосте.
 *
 * @param defaultName Имя файла по умолчанию.
 * @param ext Расширение без точки.
 * @param desc Описание типа файла для фильтра.
 * @return Абсолютный путь к выбранному файлу, или `null`, если пользователь отменил выбор.
 */
fun showSaveFileDialog(
    defaultName: String,
    ext: String,
    desc: String,
): String? = runOnEdt {
    val chooser = JFileChooser().apply {
        dialogTitle = "Сохранить как"
        selectedFile = File(defaultName)
        fileFilter = FileNameExtensionFilter("$desc (*.$ext)", ext)
        isAcceptAllFileFilterUsed = false
    }

    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
        return@runOnEdt null
    }

    val selected = chooser.selectedFile ?: return@runOnEdt null
    if (selected.name.endsWith(".$ext", ignoreCase = true)) {
        selected.absolutePath
    } else {
        "${selected.absolutePath}.$ext"
    }
}

/**
 * Открыть диалог выбора файла для чтения на хосте.
 *
 * @param title Заголовок диалога.
 * @param extensions Расширения фильтра без точки.
 * @return Абсолютный путь к выбранному файлу, или `null`, если пользователь отменил выбор.
 */
fun showOpenFileDialog(
    title: String,
    vararg extensions: String,
): String? = runOnEdt {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        if (extensions.isNotEmpty()) {
            fileFilter = FileNameExtensionFilter(
                extensions.joinToString("/") { it.uppercase() },
                *extensions,
            )
        }
        isAcceptAllFileFilterUsed = true
    }

    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
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
