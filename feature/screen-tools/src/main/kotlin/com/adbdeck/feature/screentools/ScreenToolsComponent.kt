package com.adbdeck.feature.screentools

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт feature Screen Tools.
 */
interface ScreenToolsComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<ScreenToolsState>

    /** Переключить вкладку. */
    fun onSelectTab(tab: ScreenToolsTab)

    /** Изменить директорию вывода скриншотов. */
    fun onScreenshotOutputDirectoryChanged(path: String)

    /** Изменить профиль качества скриншотов. */
    fun onScreenshotQualityChanged(quality: ScreenshotQualityPreset)

    /** Изменить директорию вывода записей экрана. */
    fun onScreenrecordOutputDirectoryChanged(path: String)

    /** Изменить профиль качества видеозаписи. */
    fun onScreenrecordQualityChanged(quality: ScreenrecordQualityPreset)

    /** Сделать скриншот. */
    fun onTakeScreenshot()

    /** Скопировать последний скриншот в буфер обмена хоста. */
    fun onCopyLastScreenshotToClipboard()

    /** Открыть последний скриншот в системном приложении. */
    fun onOpenLastScreenshotFile()

    /** Открыть директорию скриншотов. */
    fun onOpenScreenshotFolder()

    /** Запустить запись экрана. */
    fun onStartRecording()

    /** Остановить запись экрана. */
    fun onStopRecording()

    /** Открыть последний сохраненный видеофайл. */
    fun onOpenLastVideoFile()

    /** Открыть директорию видеозаписей. */
    fun onOpenVideoFolder()

    /** Скрыть текущий feedback-баннер. */
    fun onDismissFeedback()
}
