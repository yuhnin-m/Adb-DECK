package com.adbdeck.feature.deeplinks.models

import androidx.compose.runtime.Immutable

@Immutable
data class DeepLinksSuggestion(
    val value: String,
    val title: String = value,
    val description: String = "",
)

internal val DeepLinksActionSuggestions: List<DeepLinksSuggestion> = listOf(
    DeepLinksSuggestion(
        value = "android.intent.action.VIEW",
        description = "Открыть или показать ресурс по URI (deeplink, https, content://).",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.MAIN",
        description = "Главный вход в приложение, обычно с android.intent.category.LAUNCHER.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.SEND",
        description = "Поделиться одним объектом (текст, файл или URI).",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.SEND_MULTIPLE",
        description = "Поделиться несколькими объектами (несколько файлов или URI).",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.EDIT",
        description = "Открыть ресурс на редактирование, если есть обработчик.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.DIAL",
        description = "Открыть звонилку с номером без выполнения звонка.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.CALL",
        description = "Совершить звонок по номеру (может потребовать разрешения или подтверждение).",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.SEARCH",
        description = "Выполнить поиск в приложении или системе.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.WEB_SEARCH",
        description = "Открыть веб-поиск по запросу.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.OPEN_DOCUMENT",
        description = "Открыть системный выбор файла (Storage Access Framework).",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.CREATE_DOCUMENT",
        description = "Создать файл через системный диалог выбора места.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.action.GET_CONTENT",
        description = "Выбрать контент по MIME-типу (упрощенный file picker).",
    ),
    DeepLinksSuggestion(
        value = "android.provider.MediaStore.ACTION_IMAGE_CAPTURE",
        description = "Открыть камеру для создания фото.",
    ),
    DeepLinksSuggestion(
        value = "android.provider.MediaStore.ACTION_VIDEO_CAPTURE",
        description = "Открыть камеру для записи видео.",
    ),
    DeepLinksSuggestion(
        value = "android.settings.LOCATION_SOURCE_SETTINGS",
        description = "Открыть настройки геолокации.",
    ),
    DeepLinksSuggestion(
        value = "android.settings.AIRPLANE_MODE_SETTINGS",
        description = "Открыть настройки режима \"в самолете\".",
    ),
)

internal val DeepLinksCategorySuggestions: List<DeepLinksSuggestion> = listOf(
    DeepLinksSuggestion(
        value = "android.intent.category.DEFAULT",
        description = "Базовая категория для implicit intent; без нее многие activity не матчатся.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.BROWSABLE",
        description = "Для deeplink и web link; обычно используется вместе с VIEW и URI.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.LAUNCHER",
        description = "Activity отображается в лаунчере; обычно используется с MAIN.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.HOME",
        description = "Категория home-screen и launcher-активностей.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_BROWSER",
        description = "Предпочтительно открыть приложение категории Browser.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_EMAIL",
        description = "Предпочтительно открыть приложение категории Email.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_MAPS",
        description = "Предпочтительно открыть приложение категории Maps.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_MESSAGING",
        description = "Предпочтительно открыть приложение категории Messaging.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_CALENDAR",
        description = "Предпочтительно открыть приложение категории Calendar.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_CONTACTS",
        description = "Предпочтительно открыть приложение категории Contacts.",
    ),
    DeepLinksSuggestion(
        value = "android.intent.category.APP_GALLERY",
        description = "Предпочтительно открыть приложение категории Gallery.",
    ),
)

internal fun packageSuggestionsToUi(packages: List<String>): List<DeepLinksSuggestion> =
    packages.map { packageName ->
        DeepLinksSuggestion(
            value = packageName,
            description = "Установленный пакет на выбранном устройстве.",
        )
    }
