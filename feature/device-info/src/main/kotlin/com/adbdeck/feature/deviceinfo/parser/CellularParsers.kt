package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка сотовых параметров из `dumpsys telephony.registry`.
 */
internal data class CellularHighlights(
    val operatorAlphaLong: String?,
    val operatorAlphaShort: String?,
    val mcc: String?,
    val mnc: String?,
    val rplmn: String?,
    val voiceRegState: String?,
    val dataRegState: String?,
    val roamingType: String?,
    val voiceTechnology: String?,
    val dataTechnology: String?,
    val channelNumber: String?,
    val lteBands: String?,
    val cellBandwidths: String?,
    val bandwidth: String?,
    val cellCi: String?,
    val cellPci: String?,
    val cellTac: String?,
    val cellEarfcn: String?,
    val nrAvailable: String?,
    val enDcAvailable: String?,
    val dcNrRestricted: String?,
    val nrFrequencyRange: String?,
    val carrierAggregation: String?,
)

/**
 * Парсит ключевые значения из `dumpsys telephony.registry`.
 *
 * Парсер intentionally best-effort:
 * структура дампа меняется от Android/вендора к вендору, поэтому
 * ищем несколько альтернативных ключей и аккуратно нормализуем значения.
 */
internal fun parseCellularHighlights(output: String): CellularHighlights {
    val normalized = output.replace('\u0000', ' ')

    return CellularHighlights(
        operatorAlphaLong = extractFieldValue(normalized, "mOperatorAlphaLong"),
        operatorAlphaShort = extractFieldValue(normalized, "mOperatorAlphaShort"),
        mcc = extractFieldValue(normalized, "mMcc", "mcc"),
        mnc = extractFieldValue(normalized, "mMnc", "mnc"),
        rplmn = extractFieldValue(normalized, "rRplmn"),
        voiceRegState = normalizeRegistrationState(
            extractFieldValue(normalized, "mVoiceRegState"),
        ),
        dataRegState = normalizeRegistrationState(
            extractFieldValue(normalized, "mDataRegState"),
        ),
        roamingType = normalizeRoamingType(
            extractFieldValue(normalized, "roamingType", "mRoamingType"),
        ),
        voiceTechnology = normalizeRadioTechnology(
            extractFieldValue(
                normalized,
                "getRilVoiceRadioTechnology",
                "mRilVoiceRadioTechnology",
                "mVoiceRadioTechnology",
            ),
        ),
        dataTechnology = normalizeRadioTechnology(
            extractFieldValue(
                normalized,
                "getRilDataRadioTechnology",
                "mRilDataRadioTechnology",
                "mDataRadioTechnology",
            ),
        ),
        channelNumber = extractFieldValue(normalized, "mChannelNumber"),
        lteBands = extractFieldValue(normalized, "mBands"),
        cellBandwidths = extractFieldValue(normalized, "mCellBandwidths"),
        bandwidth = extractFieldValue(normalized, "mBandwidth"),
        cellCi = extractFieldValue(normalized, "mCi"),
        cellPci = extractFieldValue(normalized, "mPci"),
        cellTac = extractFieldValue(normalized, "mTac"),
        cellEarfcn = extractFieldValue(normalized, "mEarfcn"),
        nrAvailable = extractFieldValue(normalized, "isNrAvailable"),
        enDcAvailable = extractFieldValue(normalized, "isEnDcAvailable"),
        dcNrRestricted = extractFieldValue(normalized, "isDcNrRestricted"),
        nrFrequencyRange = extractFieldValue(normalized, "mNrFrequencyRange"),
        carrierAggregation = extractFieldValue(normalized, "isUsingCarrierAggregation"),
    )
}

private fun extractFieldValue(
    output: String,
    vararg keys: String,
): String? {
    keys.forEach { key ->
        val match = Regex(
            pattern = """(?:^|[^A-Za-z0-9_.-])${Regex.escape(key)}\s*[=:]\s*([^\n\r]+)""",
            option = RegexOption.MULTILINE,
        ).find(output) ?: return@forEach

        val cleaned = trimToFieldBoundary(match.groupValues[1])
        if (cleaned.isNotEmpty()) return cleaned
    }

    return null
}

private fun trimToFieldBoundary(raw: String): String {
    val start = raw.trim()
    val nextKey = NEXT_KEY_PATTERN.find(start)
    val truncated = if (nextKey != null) {
        start.substring(0, nextKey.range.first)
    } else {
        start
    }

    return truncated.trim().trimEnd(',', ';')
}

private fun normalizeRegistrationState(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    extractParenthesizedValue(value)?.let { return it }

    return when {
        value.contains("IN_SERVICE", ignoreCase = true) -> "IN_SERVICE"
        value.contains("OUT_OF_SERVICE", ignoreCase = true) -> "OUT_OF_SERVICE"
        value.contains("EMERGENCY_ONLY", ignoreCase = true) -> "EMERGENCY_ONLY"
        value.contains("POWER_OFF", ignoreCase = true) -> "POWER_OFF"
        else -> {
            when (DIGIT_PREFIX_PATTERN.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()) {
                0 -> "IN_SERVICE"
                1 -> "OUT_OF_SERVICE"
                2 -> "EMERGENCY_ONLY"
                3 -> "POWER_OFF"
                else -> value
            }
        }
    }
}

private fun normalizeRoamingType(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        value.contains("NOT_ROAMING", ignoreCase = true) -> "NOT_ROAMING"
        value.contains("ROAMING", ignoreCase = true) -> "ROAMING"
        else -> {
            when (DIGIT_PREFIX_PATTERN.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()) {
                0 -> "NOT_ROAMING"
                null -> value
                else -> "ROAMING"
            }
        }
    }
}

private fun normalizeRadioTechnology(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    extractParenthesizedValue(value)?.let { return it }

    val code = DIGIT_PREFIX_PATTERN.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return code?.let(::mapRadioTechFromCode) ?: value
}

private fun extractParenthesizedValue(raw: String): String? {
    return PARENTHESIS_PATTERN.find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun mapRadioTechFromCode(code: Int): String = when (code) {
    1 -> "GPRS"
    2 -> "EDGE"
    3 -> "UMTS"
    4 -> "CDMA"
    5 -> "EVDO_0"
    6 -> "EVDO_A"
    7 -> "1xRTT"
    8 -> "HSDPA"
    9 -> "HSUPA"
    10 -> "HSPA"
    11 -> "IDEN"
    12 -> "EVDO_B"
    13 -> "LTE"
    14 -> "EHRPD"
    15 -> "HSPAP"
    16 -> "GSM"
    17 -> "TD_SCDMA"
    18 -> "IWLAN"
    19 -> "LTE_CA"
    20 -> "NR"
    else -> code.toString()
}

private val DIGIT_PREFIX_PATTERN = Regex("""^\s*(-?\d+)""")
private val PARENTHESIS_PATTERN = Regex("""\(([^)]+)\)""")
private val NEXT_KEY_PATTERN = Regex("""(?:(?:,\s*)|\s+)[A-Za-z][A-Za-z0-9_.-]*\s*[=:]""")
