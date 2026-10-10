package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `indian digit grouping`() {
        assertEquals("₹0.00", Money.formatRupees(0))
        assertEquals("₹99.99", Money.formatRupees(9_999))
        assertEquals("₹1,000.00", Money.formatRupees(100_000))
        assertEquals("₹1,23,456.75", Money.formatRupees(12_345_675))
        assertEquals("₹1,23,45,678.90", Money.formatRupees(1_234_567_890))
        assertEquals("-₹250.00", Money.formatRupees(-25_000))
    }

    @Test
    fun `half meal rounding modes`() {
        // rate 25001 x one half-meal = 12500.5 paise
        assertEquals(12_501L, Money.halfRound(25_001, BillRounding.NEAREST))
        assertEquals(12_500L, Money.halfRound(25_001, BillRounding.FLOOR))
        assertEquals(12_501L, Money.halfRound(25_001, BillRounding.CEILING))
        assertEquals(12_500L, Money.halfRound(25_000, BillRounding.NEAREST))
    }

    @Test
    fun `per member split rounding`() {
        assertEquals(33_333L, Money.divRound(100_000, 3, BillRounding.NEAREST))
        assertEquals(66_667L, Money.divRound(200_000, 3, BillRounding.NEAREST))
        assertEquals(66_666L, Money.divRound(200_000, 3, BillRounding.FLOOR))
        assertEquals(66_667L, Money.divRound(200_000, 3, BillRounding.CEILING))
        assertEquals(20_000L, Money.divRound(100_000, 5, BillRounding.NEAREST))
    }
}
