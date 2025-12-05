package com.adbdeck.feature.packages

import com.adbdeck.core.adb.api.packages.AppPackage
import com.adbdeck.core.adb.api.packages.PackageDetails

// ── Состояние списка пакетов ──────────────────────────────────────────────────

/**
 * Состояние загрузки списка пакетов.
 */
sealed class PackagesListState {

    /** Устройство не выбрано или недоступно — список загружать нечего. */
    data object NoDevice : PackagesListState()

    /** Список пакетов загружается с устройства. */
    data object Loading : PackagesListState()

    /**
     * Список пакетов успешно загружен.
     *
     * @param packages Полный список пакетов без фильтрации.
     */
    data class Success(val packages: List<AppPackage>) : PackagesListState()

    /**
     * Произошла ошибка при загрузке.
     *
     * @param message Человекочитаемое описание ошибки.
     */
    data class Error(val message: String) : PackagesListState()
}

// ── Состояние панели деталей ──────────────────────────────────────────────────

/**
 * Состояние загрузки детальной информации о выбранном пакете.
 */
sealed class PackageDetailState {

    /** Пакет не выбран — панель не отображается. */
    data object Idle : PackageDetailState()

    /** Детали выбранного пакета загружаются. */
    data object Loading : PackageDetailState()

    /**
     * Детали успешно загружены.
     *
     * @param details Разобранная информация о пакете.
     */
    data class Success(val details: PackageDetails) : PackageDetailState()

    /**
     * Ошибка при загрузке деталей.
     *
     * @param message Описание ошибки.
     */
    data class Error(val message: String) : PackageDetailState()
}

// ── Вспомогательные типы ──────────────────────────────────────────────────────

/**
 * Фильтр по типу пакета.
 */
enum class PackageTypeFilter {
    /** Показывать все пакеты. */
    ALL,

    /** Только пользовательские приложения. */
    USER,

    /** Только системные приложения. */
    SYSTEM,
}

/**
 * Порядок сортировки списка пакетов.
 */
enum class PackageSortOrder {
    /** Сортировка по имени пакета (reverse-DNS). */
    BY_NAME,

    /** Сортировка по метке приложения (appLabel) — обратно алфавиту. */
    BY_LABEL,
}

/**
 * Ожидающее подтверждения деструктивное действие.
 *
 * Пока [PackagesState.pendingAction] != null, в UI отображается диалог подтверждения.
 */
sealed class PendingPackageAction {
    /** Удалить пакет с устройства. */
    data class Uninstall(val pkg: AppPackage, val keepData: Boolean = false) : PendingPackageAction()

    /** Очистить данные пакета. */
    data class ClearData(val pkg: AppPackage) : PendingPackageAction()
}

/**
 * Краткосрочная обратная связь после выполнения действия.
 *
 * Автоматически удаляется из состояния через 3 секунды.
 *
 * @param message Текст для отображения.
 * @param isError `true` если операция завершилась с ошибкой (красный цвет), иначе зелёный.
 */
data class ActionFeedback(
    val message: String,
    val isError: Boolean,
)

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние экрана пакетов.
 *
 * @param listState        Состояние загрузки списка пакетов.
 * @param filteredPackages Отфильтрованный и отсортированный список для отображения.
 * @param searchQuery      Текстовый поиск по имени пакета / APK-пути.
 * @param typeFilter       Фильтр по типу пакета (ALL / USER / SYSTEM).
 * @param sortOrder        Порядок сортировки.
 * @param selectedPackage  Выбранный пакет (отображается в панели деталей).
 * @param detailState      Состояние загрузки деталей выбранного пакета.
 * @param pendingAction    Ожидающее подтверждения деструктивное действие (uninstall / clear).
 * @param actionFeedback   Краткосрочное сообщение о результате последней операции.
 * @param isActionRunning  Флаг выполняющегося действия (блокирует кнопки).
 */
data class PackagesState(
    val listState: PackagesListState = PackagesListState.NoDevice,
    val filteredPackages: List<AppPackage> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: PackageTypeFilter = PackageTypeFilter.ALL,
    val sortOrder: PackageSortOrder = PackageSortOrder.BY_NAME,
    val selectedPackage: AppPackage? = null,
    val detailState: PackageDetailState = PackageDetailState.Idle,
    val pendingAction: PendingPackageAction? = null,
    val actionFeedback: ActionFeedback? = null,
    val isActionRunning: Boolean = false,
)
