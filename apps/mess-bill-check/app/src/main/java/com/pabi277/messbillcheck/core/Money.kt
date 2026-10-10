package com.pabi277.messbillcheck.core

/** All money in this app is an exact count of paise. Never a double, never a float. */
typealias Minor = Long

object Money {
    const val PAISE_PER_RUPEE = 100L

    /** Rounds a value expressed as `numerator / 2` (used for half-meals). */
    fun halfRound(numerator: Long, mode: BillRounding): Long {
        require(numerator >= 0) { "negative bill numerator: $numerator" }
        return when (mode) {
            BillRounding.FLOOR -> numerator / 2
            BillRounding.CEILING -> (numerator + 1) / 2
            BillRounding.NEAREST -> (numerator + 1) / 2 // .5 rounds up, matching mess practice
        }
    }

    /** Divides with the same rounding policy, used for per-member charge splits. */
    fun divRound(total: Long, divisor: Int, mode: BillRounding): Long {
        require(divisor > 0) { "member count must be positive, was $divisor" }
        require(total >= 0) { "negative charge total: $total" }
        val d = divisor.toLong()
        return when (mode) {
            BillRounding.FLOOR -> total / d
            BillRounding.CEILING -> (total + d - 1) / d
            BillRounding.NEAREST -> (total + d / 2) / d
        }
    }

    /** `₹1,23,456.75` with Indian digit grouping; negative amounts keep a leading minus. */
    fun formatRupees(minor: Minor): String {
        val negative = minor < 0
        val abs = if (negative) -minor else minor
        val rupees = abs / PAISE_PER_RUPEE
        val paise = (abs % PAISE_PER_RUPEE).toInt()
        val grouped = groupIndian(rupees)
        val body = "$grouped.${paise.toString().padStart(2, '0')}"
        return if (negative) "-₹$body" else "₹$body"
    }

    /**
     * Accepts `250`, `250.5`, `250.50`, `₹1,234.50` or `1,234` and returns paise.
     * Returns null for anything ambiguous, including three decimal digits, so a typo is
     * never silently reinterpreted as a different amount.
     */
    fun parseRupees(text: String): Minor? {
        val cleaned = text.trim()
            .removePrefix("\u20B9")
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val negative = cleaned.startsWith("-")
        val body = if (negative) cleaned.removePrefix("-") else cleaned
        if (body.isEmpty() || !body.all { it.isDigit() || it == '.' }) return null
        val parts = body.split('.')
        if (parts.size > 2) return null
        val rupees = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        if (parts[0].length > 12) return null
        var minor = rupees * PAISE_PER_RUPEE
        if (parts.size == 2) {
            val frac = parts[1]
            if (frac.length > 2) return null
            val paise = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 10
                else -> frac.toLong()
            }
            minor += paise
        }
        return if (negative) -minor else minor
    }

    private fun groupIndian(rupees: Long): String {
        val s = rupees.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val front = StringBuilder()
        var i = rest.length
        while (i > 0) {
            val start = if (i - 2 < 0) 0 else i - 2
            if (front.isNotEmpty()) front.insert(0, ',')
            front.insert(0, rest.substring(start, i))
            i = start
        }
        return "$front,$last3"
    }
}
