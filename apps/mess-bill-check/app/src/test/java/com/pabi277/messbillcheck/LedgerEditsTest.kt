package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LedgerEditsTest {

    private val start = Cal.epochDay(2026, 10, 1)
    private val end = Cal.epochDay(2026, 10, 31)
    private val defaults = Defaults(25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5, "en")

    private fun fresh(): Ledger {
        val l = Ledger(schemaVersion = Ledger.SCHEMA_VERSION, defaults = defaults)
        return LedgerEdits.newPeriod(l, "October 2026", start, end, 25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5)
    }

    @Test
    fun `one row per date, newest write wins`() {
        var l = fresh()
        l = LedgerEdits.setMeals(l, "p$start", start, 2, false)
        l = LedgerEdits.setMeals(l, "p$start", start, 1, false)
        assertEquals(1, l.mealsOf("p$start").size)
        assertEquals(1, l.mealOn("p$start", start)?.halves)
    }

    @Test
    fun `marking leave clears the logged meals for that day`() {
        var l = fresh()
        l = LedgerEdits.setMeals(l, "p$start", start, 2, false)
        l = LedgerEdits.setMeals(l, "p$start", start, 0, true)
        val entry = l.mealOn("p$start", start)!!
        assertTrue(entry.isLeave)
        assertEquals(0, entry.halves)
    }

    @Test
    fun `entries outside the period are refused`() {
        try {
            LedgerEdits.setMeals(fresh(), "p$start", end + 1, 2, false)
            fail("expected refusal")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("outside"))
        }
    }

    @Test
    fun `overlapping open periods are refused but closed ones do not block`() {
        try {
            LedgerEdits.newPeriod(fresh(), "Overlap", start + 10, end + 10, 25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5)
            fail("expected refusal")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("overlaps"))
        }
        val closed = LedgerEdits.close(fresh(), "p$start")
        val next = LedgerEdits.newPeriod(closed, "Next", start + 10, end + 10, 25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5)
        assertEquals(2, next.periods.size)
    }

    @Test
    fun `a closed month cannot be edited in any way`() {
        val l = LedgerEdits.close(fresh(), "p$start")
        val id = "p$start"
        val attempts = listOf(
            { LedgerEdits.setMeals(l, id, start, 2, false) },
            { LedgerEdits.addCharge(l, id, "Maid", 1_000, SplitRule.PER_MEMBER) },
            { LedgerEdits.removeCharge(l, id, "c1") },
            { LedgerEdits.addTxn(l, id, start, DepositKind.PAYMENT, 1_000) },
            { LedgerEdits.removeTxn(l, id, "t1") }
        )
        for (attempt in attempts) {
            try {
                attempt()
                fail("a mutation slipped past the closed-period guard")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("closed"))
            }
        }
    }

    @Test
    fun `the manager's stated bill stays editable after close`() {
        val l = LedgerEdits.close(fresh(), "p$start")
        val updated = LedgerEdits.setStated(l, "p$start", 600_000, "note slipped under my door")
        assertEquals(600_000L, updated.statedOf("p$start")?.amountMinor)
        assertNull(LedgerEdits.setStated(updated, "p$start", null).statedOf("p$start"))
    }

    @Test
    fun `reopen forks a new open period instead of editing the closed one`() {
        val closed = LedgerEdits.close(
            LedgerEdits.setMeals(fresh(), "p$start", start, 2, false),
            "p$start"
        )
        val (l, fresh2) = LedgerEdits.reopen(closed, "p$start")
        assertTrue(closed.period("p$start")!!.closed)
        assertTrue(!fresh2.closed)
        assertEquals(2, l.periods.size)
        assertEquals(1, l.mealsOf(fresh2.id).size)
        assertEquals(2, l.mealsOf(fresh2.id).first().halves)
        assertEquals("October 2026 (reopened)", fresh2.label)
    }

    @Test
    fun `transactions and charges are scoped to their period`() {
        var l = fresh()
        l = LedgerEdits.addTxn(l, "p$start", start, DepositKind.DEPOSIT, 50_000)
        l = LedgerEdits.addTxn(l, "p$start", start + 3, DepositKind.PAYMENT, 10_000)
        assertEquals(2, l.txnsOf("p$start").size)
        assertEquals(start, l.txnsOf("p$start").first().epochDay)
        val id = l.txnsOf("p$start").last().id
        l = LedgerEdits.removeTxn(l, "p$start", id)
        assertEquals(1, l.txnsOf("p$start").size)
        l = LedgerEdits.addCharge(l, "p$start", "Water", 3_000, SplitRule.PER_MEMBER)
        assertEquals(1, l.chargesOf("p$start").size)
        assertNotNull(Billing.compute(l, "p$start"))
    }

    @Test
    fun `a negative or zero transaction is refused`() {
        try {
            LedgerEdits.addTxn(fresh(), "p$start", start, DepositKind.PAYMENT, 0)
            fail("expected refusal")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("above zero"))
        }
    }
}
