package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.ContactAccount

internal fun ContactAccount.localizedLabel(localAccountLabel: String): String = when {
    isLocal -> localAccountLabel
    accountName.isNotBlank() -> "$accountName ($accountType)"
    else -> accountType
}
