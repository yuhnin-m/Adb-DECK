package com.adbdeck.feature.notifications

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationsFilteringTest {

    @Test
    fun `all filter merges current and historical without duplicates`() {
        val currentA = record(key = "a", packageName = "com.a", postedAt = 2_000)
        val currentB = record(key = "b", packageName = "com.b", postedAt = 3_000)
        val historicalDuplicateB = record(key = "b", packageName = "com.b.old", postedAt = 1_000)
        val historicalC = record(key = "c", packageName = "com.c", postedAt = 500)

        val state = NotificationsState(
            filter = NotificationsFilter.ALL,
            sortOrder = NotificationsSortOrder.NEWEST_FIRST,
            currentNotifications = listOf(currentA, currentB),
            snapshotHistory = listOf(historicalDuplicateB, historicalC),
        )

        val result = calculateDisplayedNotifications(state)

        assertEquals(listOf("b", "a", "c"), result.map { it.key })
    }

    @Test
    fun `search query matches package title and text`() {
        val packageMatch = record(key = "pkg", packageName = "com.example.package")
        val titleMatch = record(key = "title", packageName = "com.other", title = "Hello title")
        val textMatch = record(key = "text", packageName = "com.third", text = "Some query inside")

        val state = NotificationsState(
            filter = NotificationsFilter.CURRENT,
            searchQuery = "query",
            currentNotifications = listOf(packageMatch, titleMatch, textMatch),
        )

        val result = calculateDisplayedNotifications(state)

        assertEquals(listOf("text"), result.map { it.key })
    }

    @Test
    fun `sort by package uses package name ascending`() {
        val zRecord = record(key = "z", packageName = "com.z")
        val aRecord = record(key = "a", packageName = "com.a")
        val mRecord = record(key = "m", packageName = "com.m")

        val state = NotificationsState(
            filter = NotificationsFilter.CURRENT,
            sortOrder = NotificationsSortOrder.BY_PACKAGE,
            currentNotifications = listOf(zRecord, aRecord, mRecord),
        )

        val result = calculateDisplayedNotifications(state)

        assertEquals(listOf("a", "m", "z"), result.map { it.key })
    }

    private fun record(
        key: String,
        packageName: String,
        title: String? = null,
        text: String? = null,
        postedAt: Long? = null,
    ): NotificationRecord = NotificationRecord(
        key = key,
        packageName = packageName,
        notificationId = 1,
        tag = null,
        importance = 3,
        channelId = null,
        title = title,
        text = text,
        subText = null,
        bigText = null,
        summaryText = null,
        category = null,
        flags = 0,
        isOngoing = false,
        isClearable = true,
        postedAt = postedAt,
        group = null,
        sortKey = null,
        actionsCount = null,
        actionTitles = emptyList(),
        imageParameters = emptyMap(),
        rawBlock = "",
    )
}
