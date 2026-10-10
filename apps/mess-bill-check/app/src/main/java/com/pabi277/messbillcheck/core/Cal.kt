package com.pabi277.messbillcheck.core

/**
 * Proleptic-Gregorian civil-date maths, implemented instead of `java.time` because
 * `minSdk 24` would otherwise require core-library desugaring and one more dependency.
 * Algorithms are the well-known days-from-civil / civil-from-days pair; `epochDay`
 * here is days since 1970-01-01 (day 0), matching Java's `LocalDate.toEpochDay`.
 */
object Cal {
    const val DAYS_PER_ERA = 146_097L
    const val EPOCH_OFFSET = 719_468L

    fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeap(year)) 29 else 28
        else -> throw IllegalArgumentException("month out of range: $month")
    }

    fun daysInYear(year: Int): Int = if (isLeap(year)) 366 else 365

    fun dayOfYear(year: Int, month: Int, day: Int): Int {
        var n = day
        for (m in 1 until month) n += daysInMonth(year, m)
        return n
    }

    fun epochDay(year: Int, month: Int, day: Int): Long {
        var y = year
        if (month <= 2) y -= 1
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = if (month > 2) month - 3 else month + 9
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe.toLong() * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * DAYS_PER_ERA + doe - EPOCH_OFFSET
    }

    /** Returns year, month, day. */
    fun fromEpochDay(z0: Long): IntArray {
        val z = z0 + EPOCH_OFFSET
        val era = (if (z >= 0) z else z - (DAYS_PER_ERA - 1)) / DAYS_PER_ERA
        val doe = z - era * DAYS_PER_ERA
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return intArrayOf((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
    }

    /** 0 = Monday .. 6 = Sunday. 1970-01-01 was a Thursday (3). */
    fun weekday(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7).toInt()

    fun formatIso(epochDay: Long): String {
        val ymd = fromEpochDay(epochDay)
        return "%04d-%02d-%02d".format(ymd[0], ymd[1], ymd[2])
    }

    fun today(todaySupplier: () -> Long = { System.currentTimeMillis() / 86_400_000L }): Long =
        todaySupplier()
}
