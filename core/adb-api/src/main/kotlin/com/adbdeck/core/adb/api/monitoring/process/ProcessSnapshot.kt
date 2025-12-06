package com.adbdeck.core.adb.api.monitoring.process

/**
 * Снимок состояния всей системы: список процессов + системные метрики.
 *
 * Возвращается из [com.adbdeck.core.adb.api.monitoring.SystemMonitorClient.getProcessSnapshot] как единый объект,
 * чтобы минимизировать количество ADB-вызовов.
 *
 * @param processes        Список всех процессов устройства.
 * @param systemCpuPercent Суммарное использование CPU (0–100).
 *                         Парсится из заголовка `top` или вычисляется из `/proc/stat`.
 * @param totalRamKb       Общий объём RAM в KB.
 * @param usedRamKb        Используемая RAM в KB.
 * @param freeRamKb        Свободная RAM в KB.
 */
data class ProcessSnapshot(
    val processes: List<ProcessInfo>,
    val systemCpuPercent: Float = 0f,
    val totalRamKb: Long = 0L,
    val usedRamKb: Long = 0L,
    val freeRamKb: Long = 0L,
)
