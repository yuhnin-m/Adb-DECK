package com.adbdeck.feature.contacts.models

/**
 * Состояние длительной операции (импорт/экспорт).
 *
 * @param title          Заголовок модального окна.
 * @param status         Текущий статус операции.
 * @param currentStep    Текущий шаг (для determinate progress), `null` если неизвестно.
 * @param totalSteps     Общее число шагов, `null` если неизвестно.
 * @param isIndeterminate `true`, если прогресс нельзя посчитать.
 * @param logs           Журнал выполнения (последние события).
 */
data class ContactsOperationState(
    val title: String,
    val status: String,
    val currentStep: Int? = null,
    val totalSteps: Int? = null,
    val isIndeterminate: Boolean = true,
    val logs: List<String> = emptyList(),
)
