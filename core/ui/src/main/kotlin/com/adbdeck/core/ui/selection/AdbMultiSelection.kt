package com.adbdeck.core.ui.selection

import androidx.compose.runtime.Immutable

/**
 * Универсальное состояние множественного выбора элементов списка.
 *
 * Используется в desktop-экранах с паттерном:
 * - click: single select;
 * - Shift+click: range select;
 * - Ctrl/Cmd+click: toggle item.
 */
@Immutable
data class AdbMultiSelectionState<T>(
    val selectedIds: Set<T> = emptySet(),
    val anchorId: T? = null,
)

/**
 * Оставляет в состоянии выбора только элементы, которые присутствуют в [visibleIds].
 *
 * Нужен после фильтрации/обновления списка, чтобы не хранить «битые» ссылки
 * на уже невидимые строки.
 */
fun <T> AdbMultiSelectionState<T>.retainVisible(
    visibleIds: Collection<T>,
): AdbMultiSelectionState<T> {
    val visibleSet = visibleIds.toHashSet()
    val normalizedSelection = selectedIds.filterTo(linkedSetOf()) { id -> id in visibleSet }
    val normalizedAnchor = anchorId?.takeIf { id -> id in visibleSet }
    if (normalizedSelection == selectedIds && normalizedAnchor == anchorId) return this
    return copy(selectedIds = normalizedSelection, anchorId = normalizedAnchor)
}

/** Сбросить выделение и anchor-строку. */
fun <T> AdbMultiSelectionState<T>.clearSelection(): AdbMultiSelectionState<T> =
    if (selectedIds.isEmpty() && anchorId == null) this else AdbMultiSelectionState()

/**
 * Выделить все строки из [visibleIds].
 */
fun <T> AdbMultiSelectionState<T>.selectAll(
    visibleIds: List<T>,
): AdbMultiSelectionState<T> {
    if (visibleIds.isEmpty()) return clearSelection()
    return AdbMultiSelectionState(
        selectedIds = visibleIds.toSet(),
        anchorId = visibleIds.first(),
    )
}

/**
 * Применяет пользовательское действие выбора строки.
 *
 * @param itemId Id строки, по которой кликнули.
 * @param visibleIds Идентификаторы строк в текущем визуальном порядке.
 * @param additiveSelection true для Ctrl/Cmd+Click.
 * @param rangeSelection true для Shift+Click.
 */
fun <T> AdbMultiSelectionState<T>.onItemSelectionRequested(
    itemId: T,
    visibleIds: List<T>,
    additiveSelection: Boolean,
    rangeSelection: Boolean,
): AdbMultiSelectionState<T> {
    if (visibleIds.isEmpty()) return this

    if (rangeSelection && anchorId != null) {
        val anchorIndex = visibleIds.indexOf(anchorId)
        val targetIndex = visibleIds.indexOf(itemId)
        if (anchorIndex != -1 && targetIndex != -1) {
            val rangeIds = if (anchorIndex <= targetIndex) {
                visibleIds.subList(anchorIndex, targetIndex + 1)
            } else {
                visibleIds.subList(targetIndex, anchorIndex + 1)
            }
            val updatedSelection = if (additiveSelection) {
                selectedIds + rangeIds
            } else {
                rangeIds.toSet()
            }
            return copy(selectedIds = updatedSelection)
        }
    }

    val updatedSelection = when {
        additiveSelection && itemId in selectedIds -> selectedIds - itemId
        additiveSelection -> selectedIds + itemId
        else -> setOf(itemId)
    }
    return copy(selectedIds = updatedSelection, anchorId = itemId)
}
