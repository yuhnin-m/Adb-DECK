package com.adbdeck.feature.notifications

import com.adbdeck.core.adb.api.notifications.NotificationRecord

/**
 * Пересчитывает отображаемый список уведомлений с учетом фильтров и сортировки.
 */
internal fun calculateDisplayedNotifications(state: NotificationsState): List<NotificationRecord> {
    val source: List<NotificationRecord> = when (state.filter) {
        NotificationsFilter.ALL -> {
            val currentKeys = state.currentNotifications.asSequence().map { it.key }.toHashSet()
            val historicalOnly = state.snapshotHistory.filter { it.key !in currentKeys }
            (state.currentNotifications + historicalOnly).distinctBy { it.key }
        }

        NotificationsFilter.CURRENT -> state.currentNotifications
        NotificationsFilter.HISTORICAL -> {
            val currentKeys = state.currentNotifications.asSequence().map { it.key }.toHashSet()
            state.snapshotHistory.filter { it.key !in currentKeys }
        }

        NotificationsFilter.SAVED -> state.savedNotifications.map { it.record }
    }

    val filtered = source
        .filter { state.searchQuery.isBlank() || it.matchesQuery(state.searchQuery) }
        .filter {
            state.packageFilter.isBlank() ||
                it.packageName.contains(state.packageFilter, ignoreCase = true)
        }

    return when (state.sortOrder) {
        NotificationsSortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.postedAt ?: 0L }
        NotificationsSortOrder.OLDEST_FIRST -> filtered.sortedBy { it.postedAt ?: Long.MAX_VALUE }
        NotificationsSortOrder.BY_PACKAGE -> filtered.sortedBy { it.packageName }
    }
}

/**
 * Проверяет совпадение поискового запроса с полями уведомления.
 */
internal fun NotificationRecord.matchesQuery(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true

    return packageName.contains(normalizedQuery, ignoreCase = true) ||
        title?.contains(normalizedQuery, ignoreCase = true) == true ||
        text?.contains(normalizedQuery, ignoreCase = true) == true
}
