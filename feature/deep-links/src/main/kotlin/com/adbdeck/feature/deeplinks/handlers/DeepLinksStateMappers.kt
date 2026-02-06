package com.adbdeck.feature.deeplinks.handlers

import com.adbdeck.core.adb.api.intents.DeepLinkParams
import com.adbdeck.core.adb.api.intents.IntentParams
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.feature.deeplinks.models.DeepLinksState

internal fun DeepLinksState.toDeepLinkParams(): DeepLinkParams = DeepLinkParams(
    uri = dlUri,
    action = dlAction,
    packageName = dlPackage,
    component = dlComponent,
    category = dlCategory,
)

internal fun DeepLinksState.toIntentParams(): IntentParams = IntentParams(
    action = itAction,
    dataUri = itDataUri,
    packageName = itPackage,
    component = itComponent,
    categories = itCategories,
    flags = itFlags,
    extras = itExtras,
)

internal fun DeepLinksState.withDeepLinkParams(params: DeepLinkParams): DeepLinksState = copy(
    mode = LaunchMode.DEEP_LINK,
    dlUri = params.uri,
    dlAction = params.action,
    dlPackage = params.packageName,
    dlComponent = params.component,
    dlCategory = params.category,
)

internal fun DeepLinksState.withIntentParams(params: IntentParams): DeepLinksState = copy(
    mode = LaunchMode.INTENT,
    itAction = params.action,
    itDataUri = params.dataUri,
    itPackage = params.packageName,
    itComponent = params.component,
    itCategories = params.categories,
    itFlags = params.flags,
    itExtras = params.extras,
)
