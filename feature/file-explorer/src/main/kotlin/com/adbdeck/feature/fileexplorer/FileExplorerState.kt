package com.adbdeck.feature.fileexplorer

/**
 * Сторона двухпанельного проводника.
 */
enum class ExplorerSide {
    /** Локальная файловая система хоста. */
    LOCAL,

    /** Файловая система Android-устройства. */
    DEVICE,
}

/**
 * Тип элемента файловой системы.
 */
enum class ExplorerFileType {
    /** Директория. */
    DIRECTORY,

    /** Обычный файл. */
    FILE,

    /** Символическая ссылка. */
    SYMLINK,

    /** Прочие типы. */
    OTHER,
}

/**
 * Унифицированное представление файла/папки для локальной и device-панели.
 *
 * @param name Название элемента без родительского пути.
 * @param fullPath Абсолютный путь элемента.
 * @param type Тип элемента.
 * @param sizeBytes Размер в байтах (если известен).
 * @param modifiedEpochMillis Время модификации в epoch millis (если известно).
 */
data class ExplorerFileItem(
    val name: String,
    val fullPath: String,
    val type: ExplorerFileType,
    val sizeBytes: Long? = null,
    val modifiedEpochMillis: Long? = null,
) {
    /** `true`, если элемент является директорией. */
    val isDirectory: Boolean get() = type == ExplorerFileType.DIRECTORY
}

/**
 * Состояние списка элементов на панели.
 */
sealed class ExplorerListState {
    /** Нет активного устройства или оно недоступно (для device-панели). */
    data class NoDevice(val message: String) : ExplorerListState()

    /** Идёт загрузка списка. */
    data object Loading : ExplorerListState()

    /** Загрузка успешна, но элементов нет. */
    data object Empty : ExplorerListState()

    /** Ошибка загрузки списка. */
    data class Error(val message: String) : ExplorerListState()

    /** Элементы успешно загружены. */
    data class Success(val items: List<ExplorerFileItem>) : ExplorerListState()
}

/**
 * Состояние одной панели проводника.
 *
 * @param currentPath Текущий открытый путь.
 * @param listState Состояние списка элементов.
 * @param selectedPath Абсолютный путь выбранного элемента, если есть.
 */
data class ExplorerPanelState(
    val currentPath: String,
    val listState: ExplorerListState,
    val selectedPath: String? = null,
)

/**
 * Направление файлового переноса.
 */
enum class TransferDirection {
    /** Копирование с хоста на устройство (`adb push`). */
    PUSH,

    /** Копирование с устройства на хост (`adb pull`). */
    PULL,
}

/**
 * Состояние текущей операции переноса.
 *
 * Если [progress] == `null`, UI должен отрисовать indeterminate progress.
 */
data class TransferState(
    val direction: TransferDirection,
    val sourcePath: String,
    val targetPath: String,
    val status: String,
    val progress: Float? = null,
)

/**
 * Диалог подтверждения удаления.
 */
data class DeleteDialogState(
    val side: ExplorerSide,
    val item: ExplorerFileItem,
)

/**
 * Диалог создания директории.
 */
data class CreateDirectoryDialogState(
    val side: ExplorerSide,
    val parentPath: String,
    val name: String = "",
)

/**
 * Диалог переименования элемента.
 */
data class RenameDialogState(
    val side: ExplorerSide,
    val item: ExplorerFileItem,
    val newName: String,
)

/**
 * Диалог подтверждения overwrite при переносе.
 */
data class TransferConflictDialogState(
    val direction: TransferDirection,
    val sourcePath: String,
    val targetPath: String,
)

/**
 * Краткосрочный баннер обратной связи.
 */
data class ExplorerFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Полное состояние экрана File Explorer.
 */
data class FileExplorerState(
    val localPanel: ExplorerPanelState,
    val devicePanel: ExplorerPanelState,
    val activeDeviceId: String? = null,
    val deviceRoots: List<String> = emptyList(),
    val isActionRunning: Boolean = false,
    val transferState: TransferState? = null,
    val deleteDialog: DeleteDialogState? = null,
    val createDirectoryDialog: CreateDirectoryDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val transferConflictDialog: TransferConflictDialogState? = null,
    val feedback: ExplorerFeedback? = null,
)
