package com.adbdeck.feature.scrcpy

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана scrcpy.
 *
 * Управляет:
 * - Наблюдением за активным устройством через DeviceManager
 * - Запуском и остановкой внешнего процесса scrcpy
 * - Формированием команды на основе текущей конфигурации
 * - Загрузкой пути к scrcpy из настроек
 * - Корректным завершением процесса при уничтожении компонента
 */
interface ScrcpyComponent {

    /** Текущее полное состояние экрана. */
    val state: StateFlow<ScrcpyState>

    // ── Управление процессом ──────────────────────────────────

    /**
     * Запустить scrcpy для активного устройства.
     *
     * Запуск невозможен если:
     * - нет активного устройства
     * - scrcpy не настроен (пустой путь)
     * - процесс уже запущен
     */
    fun startScrcpy()

    /**
     * Остановить запущенный процесс scrcpy.
     *
     * После остановки состояние переходит в [ScrcpyProcessState.IDLE].
     */
    fun stopScrcpy()

    // ── Конфигурация ─────────────────────────────────────────

    /**
     * Изменить ограничение максимального разрешения.
     *
     * @param resolution Новое значение или [ScrcpyMaxResolution.NO_LIMIT].
     */
    fun onMaxResolutionChanged(resolution: ScrcpyMaxResolution)

    /**
     * Изменить частоту кадров.
     *
     * @param fps Новая частота кадров.
     */
    fun onFpsChanged(fps: ScrcpyFps)

    /**
     * Изменить битрейт видео.
     *
     * @param bitrate Строка в Мбит/с. Пустая строка — не передавать флаг.
     */
    fun onBitrateChanged(bitrate: String)

    /**
     * Переключить разрешение управления устройством.
     *
     * @param enabled `true` — передавать ввод на устройство.
     */
    fun onAllowInputChanged(enabled: Boolean)

    /**
     * Переключить выключение экрана устройства при зеркалировании.
     *
     * @param enabled `true` — выключить экран.
     */
    fun onTurnScreenOffChanged(enabled: Boolean)

    /**
     * Переключить отображение касаний на экране устройства.
     *
     * @param enabled `true` — показывать касания.
     */
    fun onShowTouchesChanged(enabled: Boolean)

    /**
     * Переключить режим "не засыпать".
     *
     * @param enabled `true` — удерживать устройство активным.
     */
    fun onStayAwakeChanged(enabled: Boolean)

    /**
     * Переключить полноэкранный режим окна.
     *
     * @param enabled `true` — запускать в полноэкранном режиме.
     */
    fun onFullscreenChanged(enabled: Boolean)

    /**
     * Переключить режим "поверх всех окон".
     *
     * @param enabled `true` — окно остаётся поверх остальных.
     */
    fun onAlwaysOnTopChanged(enabled: Boolean)

    /**
     * Переключить режим окна без рамки.
     *
     * @param enabled `true` — окно без заголовка и рамки.
     */
    fun onBorderlessChanged(enabled: Boolean)

    /**
     * Изменить ширину окна scrcpy в пикселях.
     *
     * @param width Строковое значение ширины. Пустая строка — авто.
     */
    fun onWindowWidthChanged(width: String)

    /**
     * Изменить высоту окна scrcpy в пикселях.
     *
     * @param height Строковое значение высоты. Пустая строка — авто.
     */
    fun onWindowHeightChanged(height: String)

    /**
     * Изменить видеокодек.
     *
     * @param codec Новый кодек ([ScrcpyVideoCodec]).
     */
    fun onVideoCodecChanged(codec: ScrcpyVideoCodec)

    /**
     * Изменить режим эмуляции клавиатуры.
     *
     * @param mode Новый режим ([ScrcpyInputMode]).
     */
    fun onKeyboardModeChanged(mode: ScrcpyInputMode)

    /**
     * Изменить режим эмуляции мыши.
     *
     * @param mode Новый режим ([ScrcpyInputMode]).
     */
    fun onMouseModeChanged(mode: ScrcpyInputMode)

    // ── Навигация ────────────────────────────────────────────

    /**
     * Перейти на экран Settings.
     *
     * Вызывается из кнопки "Открыть настройки" при ненастроенном scrcpy.
     */
    fun onOpenSettings()

    // ── Обратная связь ───────────────────────────────────────

    /** Скрыть текущее сообщение обратной связи. */
    fun onDismissFeedback()
}
