package com.adbdeck.core.adb.api.packages

/**
 * Контракт клиента для работы с установленными пакетами через ADB.
 *
 * Все операции возвращают [Result]: в случае ошибки — [Result.failure] с понятным сообщением.
 * Все функции являются `suspend` и выполняют I/O в соответствующем контексте.
 *
 * Типичные ADB-команды, используемые реализацией:
 * - `adb -s <id> shell pm list packages -f` — список пакетов
 * - `adb -s <id> shell dumpsys package <pkg>` — детали пакета
 * - `adb -s <id> shell am force-stop <pkg>` — принудительная остановка
 * - `adb -s <id> shell pm clear <pkg>` — очистка данных
 * - `adb -s <id> shell pm uninstall <pkg>` — удаление
 * - `adb -s <id> shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS` — системная инфо
 * - `adb -s <id> shell pm grant/revoke <pkg> <perm>` — управление разрешениями
 * - `adb -s <id> shell pm path <pkg>` + `adb -s <id> pull ...` — выгрузка APK
 */
interface PackageClient {

    /**
     * Получить список всех установленных пакетов.
     *
     * Выполняет `pm list packages -f`. Типы пакетов определяются по пути APK:
     * `/system`, `/product`, `/vendor` → [PackageType.SYSTEM], иначе → [PackageType.USER].
     *
     * @param deviceId  Серийный номер / адрес устройства (передаётся в `adb -s`).
     * @param adbPath   Путь к исполняемому файлу `adb` (по умолчанию `"adb"`).
     * @return Список пакетов, отсортированный по имени пакета.
     */
    suspend fun getPackages(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<List<AppPackage>>

    /**
     * Получить подробную информацию о конкретном пакете.
     *
     * Выполняет `dumpsys package <packageName>`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param adbPath     Путь к `adb`.
     * @return [PackageDetails] с разобранными полями.
     */
    suspend fun getPackageDetails(
        deviceId: String,
        packageName: String,
        adbPath: String = "adb",
    ): Result<PackageDetails>

    /**
     * Запустить приложение через Intent LAUNCHER.
     *
     * Выполняет `monkey -p <pkg> -c android.intent.category.LAUNCHER 1`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun launchApp(
        deviceId: String,
        packageName: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Принудительно остановить приложение.
     *
     * Выполняет `am force-stop <packageName>`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun forceStop(
        deviceId: String,
        packageName: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Очистить данные приложения (эквивалент «Очистить данные» в системных настройках).
     *
     * Выполняет `pm clear <packageName>`. Возвращает ошибку, если вывод не содержит `Success`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun clearData(
        deviceId: String,
        packageName: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Удалить приложение с устройства.
     *
     * Выполняет `pm uninstall [--keep-data] <packageName>`.
     * Системные приложения нельзя удалить без root — в таком случае вернётся ошибка.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param keepData    Если `true`, сохранить данные и кэш после удаления APK.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun uninstall(
        deviceId: String,
        packageName: String,
        keepData: Boolean = false,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Открыть системный экран «Информация о приложении» (App Info).
     *
     * Запускает Activity с action `android.settings.APPLICATION_DETAILS_SETTINGS`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun openAppInfo(
        deviceId: String,
        packageName: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Выгрузить основной APK-файл пакета на хост.
     *
     * Реализация должна определить путь к base APK на устройстве и выполнить `adb pull`.
     * Для split-приложений выгружается только основной APK (base.apk).
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param localPath   Абсолютный путь к файлу назначения на хосте (например `/tmp/app.apk`).
     * @param adbPath     Путь к `adb`.
     */
    suspend fun exportBaseApk(
        deviceId: String,
        packageName: String,
        localPath: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Выдать runtime-разрешение приложению.
     *
     * Выполняет `pm grant <packageName> <permission>`.
     * Работает только с runtime-разрешениями (dangerous permissions).
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param permission  Полное имя разрешения (например `android.permission.READ_CONTACTS`).
     * @param adbPath     Путь к `adb`.
     */
    suspend fun grantPermission(
        deviceId: String,
        packageName: String,
        permission: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Отозвать runtime-разрешение у приложения.
     *
     * Выполняет `pm revoke <packageName> <permission>`.
     *
     * @param deviceId    Серийный номер / адрес устройства.
     * @param packageName Имя пакета.
     * @param permission  Полное имя разрешения.
     * @param adbPath     Путь к `adb`.
     */
    suspend fun revokePermission(
        deviceId: String,
        packageName: String,
        permission: String,
        adbPath: String = "adb",
    ): Result<Unit>
}
