package com.adbdeck.feature.update.install

import java.nio.file.Path

/**
 * Платформенный установщик уже загруженного пакета обновления.
 */
interface AppUpdateInstaller {

    /**
     * Поддерживается ли in-app установка для текущей платформы и URL ассета.
     */
    fun canInstallInApp(downloadUrl: String): Boolean

    /**
     * Подготавливает установку обновления из загруженного файла.
     *
     * Ожидается, что вызывающая сторона завершит текущее приложение после успешного вызова,
     * чтобы внешний installer-скрипт мог заменить бинарник.
     */
    suspend fun installFromDownloadedPackage(packageFile: Path)
}
