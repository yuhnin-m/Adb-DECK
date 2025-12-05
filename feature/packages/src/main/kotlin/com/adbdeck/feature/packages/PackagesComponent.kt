package com.adbdeck.feature.packages

import com.adbdeck.core.adb.api.packages.AppPackage
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана установленных пакетов.
 *
 * Управляет:
 * - Загрузкой и обновлением списка пакетов с активного устройства
 * - Фильтрацией / сортировкой
 * - Выбором пакета и загрузкой его деталей
 * - Выполнением ADB-действий над пакетами
 * - Диалогами подтверждения деструктивных операций
 */
interface PackagesComponent {

    /** Текущее полное состояние экрана. */
    val state: StateFlow<PackagesState>

    // ── Список пакетов ──────────────────────────────────────────

    /** Перезагрузить список пакетов с текущего активного устройства. */
    fun onRefresh()

    // ── Фильтры и сортировка ────────────────────────────────────

    /**
     * Изменить текстовый запрос поиска.
     *
     * Поиск выполняется по [AppPackage.packageName] и [AppPackage.apkPath].
     *
     * @param query Строка поиска (пустая строка — сбросить фильтр).
     */
    fun onSearchChanged(query: String)

    /**
     * Изменить фильтр по типу пакета.
     *
     * @param filter [PackageTypeFilter.ALL], [PackageTypeFilter.USER] или [PackageTypeFilter.SYSTEM].
     */
    fun onTypeFilterChanged(filter: PackageTypeFilter)

    /**
     * Изменить порядок сортировки.
     *
     * @param order [PackageSortOrder.BY_NAME] (по имени пакета) или [PackageSortOrder.BY_LABEL].
     */
    fun onSortOrderChanged(order: PackageSortOrder)

    // ── Выбор и детали ──────────────────────────────────────────

    /**
     * Выбрать пакет и загрузить его детали в правую панель.
     *
     * @param pkg Пакет для отображения деталей.
     */
    fun onSelectPackage(pkg: AppPackage)

    /** Снять выделение (скрыть панель деталей). */
    fun onClearSelection()

    /**
     * Внешняя навигация: показать пакет по имени и открыть его детали.
     *
     * Используется при переходе из других экранов (например System Monitor).
     */
    fun onRevealPackage(packageName: String)

    // ── ADB-действия ────────────────────────────────────────────

    /**
     * Запустить приложение через LAUNCHER-Intent.
     *
     * @param pkg Пакет для запуска.
     */
    fun onLaunchApp(pkg: AppPackage)

    /**
     * Принудительно остановить приложение.
     *
     * @param pkg Пакет для остановки.
     */
    fun onForceStop(pkg: AppPackage)

    /**
     * Открыть системный экран «Информация о приложении».
     *
     * @param pkg Пакет для отображения в системных настройках.
     */
    fun onOpenAppInfo(pkg: AppPackage)

    /**
     * Скопировать имя пакета в системный буфер обмена.
     *
     * Не требует подключённого устройства. Обрабатывается в UI.
     *
     * @param pkg Пакет, имя которого нужно скопировать.
     */
    fun onCopyPackageName(pkg: AppPackage)

    // ── Деструктивные действия (требуют подтверждения) ──────────

    /**
     * Запросить очистку данных — показывает диалог подтверждения.
     *
     * Устанавливает [PackagesState.pendingAction] в [PendingPackageAction.ClearData].
     *
     * @param pkg Пакет, данные которого будут очищены.
     */
    fun onRequestClearData(pkg: AppPackage)

    /**
     * Запросить удаление пакета — показывает диалог подтверждения.
     *
     * Устанавливает [PackagesState.pendingAction] в [PendingPackageAction.Uninstall].
     *
     * @param pkg Пакет для удаления.
     */
    fun onRequestUninstall(pkg: AppPackage)

    /**
     * Подтвердить ожидающее деструктивное действие и выполнить его.
     *
     * Вызывается из диалога подтверждения.
     */
    fun onConfirmAction()

    /** Отменить ожидающее деструктивное действие и закрыть диалог. */
    fun onCancelAction()

    // ── Разрешения (рекомендуется вызывать с deailState = Success) ──

    /**
     * Выдать runtime-разрешение выбранному пакету.
     *
     * Обновляет [PackageDetailState] после выполнения.
     *
     * @param pkg        Пакет.
     * @param permission Полное имя разрешения (например `android.permission.CAMERA`).
     */
    fun onGrantPermission(pkg: AppPackage, permission: String)

    /**
     * Отозвать runtime-разрешение у выбранного пакета.
     *
     * @param pkg        Пакет.
     * @param permission Полное имя разрешения.
     */
    fun onRevokePermission(pkg: AppPackage, permission: String)

    // ── Обратная связь ──────────────────────────────────────────

    /** Скрыть текущее сообщение обратной связи ([PackagesState.actionFeedback]). */
    fun onDismissFeedback()
}
