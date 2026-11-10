package com.eliya.callguard.data

object PhoneNumbers {

    /** Approximate seconds per ring cycle. Used to convert "rings" into a delay. */
    const val SECONDS_PER_RING = 5

    /**
     * Builds a comparison key that ignores formatting and country prefix:
     * "+972 50-123-4567", "00972501234567" and "050-1234567" all produce "501234567".
     */
    fun key(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return if (digits.length >= 9) digits.takeLast(9) else digits
    }

    /** Light formatting for display, e.g. "0501234567" -> "050-123-4567". */
    fun pretty(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val local = when {
            digits.startsWith("972") && digits.length == 12 -> "0" + digits.drop(3)
            digits.startsWith("00972") && digits.length == 14 -> "0" + digits.drop(5)
            else -> digits
        }
        return when {
            local.length == 10 && local.startsWith("05") ->
                "${local.substring(0, 3)}-${local.substring(3, 6)}-${local.substring(6)}"
            local.length == 9 && local.startsWith("0") ->
                "${local.substring(0, 2)}-${local.substring(2, 5)}-${local.substring(5)}"
            else -> raw.trim()
        }
    }
}
