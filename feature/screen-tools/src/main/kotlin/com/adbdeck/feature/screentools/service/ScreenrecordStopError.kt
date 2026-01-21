package com.adbdeck.feature.screentools.service

/**
 * Стадия пайплайна сохранения записи экрана.
 */
enum class ScreenrecordStopStage {
    /** Остановка процесса `screenrecord` на устройстве. */
    STOP_ON_DEVICE,

    /** Копирование готового файла с устройства на хост. */
    COPY_TO_HOST,
}

/**
 * Типизированная ошибка остановки/сохранения записи.
 *
 * Нужна чтобы компонент мог различать:
 * - ошибку остановки на устройстве (можно безопасно ретраить stop),
 * - ошибку копирования на хост (запись уже остановлена).
 */
sealed class ScreenrecordStopError(
    val stage: ScreenrecordStopStage,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /**
     * Ошибка остановки процесса записи на устройстве.
     */
    class StopOnDevice(
        message: String,
        cause: Throwable? = null,
    ) : ScreenrecordStopError(
        stage = ScreenrecordStopStage.STOP_ON_DEVICE,
        message = message,
        cause = cause,
    )

    /**
     * Ошибка копирования видео на хост.
     */
    class CopyToHost(
        message: String,
        cause: Throwable? = null,
    ) : ScreenrecordStopError(
        stage = ScreenrecordStopStage.COPY_TO_HOST,
        message = message,
        cause = cause,
    )
}
