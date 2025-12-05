package com.adbdeck.core.adb.api.packages

/**
 * Полная информация об установленном пакете.
 *
 * Извлекается из вывода команды `adb shell dumpsys package <packageName>`.
 * Часть полей может отсутствовать — зависит от версии Android и прав доступа.
 *
 * @param packageName        Имя пакета.
 * @param appLabel           Человекочитаемое название приложения (best-effort из dumpsys).
 * @param versionName        Строковая версия (например `2.4.1`).
 * @param versionCode        Числовой код версии.
 * @param uid                Android UID процесса (используется ОС для разграничения прав).
 * @param codePath           Директория с APK-файлами пакета на устройстве.
 * @param dataDir            Директория данных приложения (`/data/user/0/<pkg>`).
 * @param firstInstallTime   Время первой установки (строка из dumpsys).
 * @param lastUpdateTime     Время последнего обновления (строка из dumpsys).
 * @param targetSdk          Target SDK version.
 * @param minSdk             Minimum SDK version.
 * @param isSystem           Флаг системного приложения (из `flags=[ ... SYSTEM ... ]`).
 * @param isEnabled          Включён ли пакет.
 * @param isDebuggable       Флаг отладочной сборки (из `flags=[ ... DEBUGGABLE ... ]`).
 * @param isSuspended        Флаг приостановленного пакета (suspended через `pm suspend`).
 * @param launcherActivity   Главная activity для запуска (если удалось определить).
 * @param nativeLibPath      Путь к нативным библиотекам (.so файлам).
 * @param runtimePermissions Карта runtime-разрешений: имя разрешения → выдано ли.
 */
data class PackageDetails(
    val packageName: String,

    // ── Идентификация ────────────────────────────────────────────
    val appLabel: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val uid: Int = 0,

    // ── Пути ──────────────────────────────────────────────────────
    val codePath: String = "",
    val dataDir: String = "",
    val nativeLibPath: String = "",

    // ── Временные метки ───────────────────────────────────────────
    val firstInstallTime: String = "",
    val lastUpdateTime: String = "",

    // ── SDK ───────────────────────────────────────────────────────
    val targetSdk: Int = 0,
    val minSdk: Int = 0,

    // ── Флаги ─────────────────────────────────────────────────────
    val isSystem: Boolean = false,
    val isEnabled: Boolean = true,
    val isDebuggable: Boolean = false,
    val isSuspended: Boolean = false,

    // ── Activity ──────────────────────────────────────────────────
    val launcherActivity: String = "",

    // ── Разрешения ────────────────────────────────────────────────
    /**
     * Карта runtime-разрешений: ключ — полное имя разрешения,
     * значение — выдано (`true`) или отклонено (`false`).
     *
     * Парсится из секции `runtimePermissions:` вывода `dumpsys package`.
     */
    val runtimePermissions: Map<String, Boolean> = emptyMap(),
)
