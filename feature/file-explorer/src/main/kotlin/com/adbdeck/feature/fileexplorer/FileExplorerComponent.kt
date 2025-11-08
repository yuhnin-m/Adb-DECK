package com.adbdeck.feature.fileexplorer

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт экрана двухпанельного файлового менеджера.
 *
 * Содержит действия навигации по панелям, операций с файлами и transfer-операций
 * между хостом и Android-устройством.
 */
interface FileExplorerComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<FileExplorerState>

    // ── Навигация по локальной панели ─────────────────────────────────────────

    /** Обновить список локальной директории. */
    fun onRefreshLocal()

    /** Перейти в локальной панели на уровень выше. */
    fun onLocalUp()

    /** Открыть локальную директорию [path]. */
    fun onOpenLocalDirectory(path: String)

    /** Выбрать элемент [path] в локальной панели. */
    fun onSelectLocal(path: String)

    // ── Навигация по device-панели ────────────────────────────────────────────

    /** Обновить список директории на устройстве. */
    fun onRefreshDevice()

    /** Перейти на уровень выше в device-панели. */
    fun onDeviceUp()

    /** Открыть директорию [path] на устройстве. */
    fun onOpenDeviceDirectory(path: String)

    /** Выбрать элемент [path] на устройстве. */
    fun onSelectDevice(path: String)

    /** Выбрать корневой раздел [path] для device-панели. */
    fun onSelectDeviceRoot(path: String)

    // ── Действия над файлами ──────────────────────────────────────────────────

    /** Запросить удаление выбранного элемента на стороне [side]. */
    fun onRequestDelete(side: ExplorerSide)

    /** Подтвердить удаление. */
    fun onConfirmDelete()

    /** Отменить удаление. */
    fun onCancelDelete()

    /** Открыть диалог создания директории на стороне [side]. */
    fun onRequestCreateDirectory(side: ExplorerSide)

    /** Изменить имя создаваемой директории. */
    fun onCreateDirectoryNameChanged(value: String)

    /** Подтвердить создание директории. */
    fun onConfirmCreateDirectory()

    /** Отменить создание директории. */
    fun onCancelCreateDirectory()

    /** Открыть диалог переименования выбранного элемента на стороне [side]. */
    fun onRequestRename(side: ExplorerSide)

    /** Изменить новое имя в диалоге rename. */
    fun onRenameValueChanged(value: String)

    /** Подтвердить переименование. */
    fun onConfirmRename()

    /** Отменить переименование. */
    fun onCancelRename()

    // ── Переносы между панелями ───────────────────────────────────────────────

    /** Выполнить `push` выбранного локального элемента на устройство. */
    fun onPushSelected()

    /** Выполнить `pull` выбранного элемента с устройства на хост. */
    fun onPullSelected()

    /** Подтвердить overwrite в конфликте переноса. */
    fun onConfirmTransferConflict()

    /** Отменить overwrite в конфликте переноса. */
    fun onCancelTransferConflict()

    /** Отменить текущий перенос. */
    fun onCancelTransfer()

    // ── Вспомогательные UI-события ────────────────────────────────────────────

    /** Сообщить, что путь [path] скопирован в буфер обмена. */
    fun onPathCopied(path: String)

    /** Скрыть текущий feedback-баннер. */
    fun onDismissFeedback()
}
