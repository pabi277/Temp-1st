package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalTest {

    @Test
    fun `leap year rules`() {
        assertTrue(Cal.isLeap(2000))
        assertTrue(Cal.isLeap(2024))
        assertFalse(Cal.isLeap(2100))
        assertFalse(Cal.isLeap(2026))
        assertEquals(29, Cal.daysInMonth(2024, 2))
        assertEquals(28, Cal.daysInMonth(2026, 2))
        assertEquals(31, Cal.daysInMonth(2026, 1))
        assertEquals(30, Cal.daysInMonth(2026, 9))
        assertEquals(365, Cal.daysInYear(2026))
    }

    @Test
    fun `epoch day matches known reference values`() {
        assertEquals(0L, Cal.epochDay(1970, 1, 1))
        assertEquals(11016L, Cal.epochDay(2000, 2, 29))
        assertEquals(19782L, Cal.epochDay(2024, 2, 29))
        assertEquals(20737L, Cal.epochDay(2026, 10, 11))
        assertEquals(-141086L, Cal.epochDay(1583, 9, 21))
    }

    @Test
    fun `round trip over four years of days`() {
        var day = Cal.epochDay(2023, 1, 1)
        val last = Cal.epochDay(2026, 12, 31)
        var count = 0
        while (day <= last) {
            val ymd = Cal.fromEpochDay(day)
            assertEquals(day, Cal.epochDay(ymd[0], ymd[1], ymd[2]))
            assertEquals("%04d-%02d-%02d".format(ymd[0], ymd[1], ymd[2]), Cal.formatIso(day))
            day += 1
            count += 1
        }
        assertEquals(1461, count)
    }

    @Test
    fun `weekday of known dates`() {
        assertEquals(3, Cal.weekday(Cal.epochDay(1970, 1, 1))) // Thursday
        assertEquals(6, Cal.weekday(Cal.epochDay(2026, 10, 11))) // Sunday
        assertEquals(0, Cal.weekday(Cal.epochDay(2026, 10, 12))) // Monday is index 0
    }

    @Test
    fun `day of year across a leap boundary`() {
        assertEquals(1, Cal.dayOfYear(2026, 1, 1))
        assertEquals(61, Cal.dayOfYear(2024, 3, 1)) // 31 + 29 + 1
        assertEquals(60, Cal.dayOfYear(2026, 3, 1)) // 31 + 28 + 1
    }
}
