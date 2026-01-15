package com.adbdeck.feature.quicktoggles

import kotlin.math.abs

/**
 * Сравнение float-значений с допуском для стабильной UI/ADB-логики.
 */
internal fun Float.approxEquals(
    value: Float,
    epsilon: Float = 0.0001f,
): Boolean {
    return abs(this - value) < epsilon
}
