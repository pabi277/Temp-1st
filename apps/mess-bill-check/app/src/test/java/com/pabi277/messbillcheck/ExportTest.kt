package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTest {

    private val start = Cal.epochDay(2026, 10, 1)
    private val end = Cal.epochDay(2026, 10, 31)
    private val id get() = "p$start"

    private fun ledger(): Ledger {
        var l = Ledger(schemaVersion = Ledger.SCHEMA_VERSION)
        l = LedgerEdits.newPeriod(l, "October 2026", start, end, 25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5)
        l = LedgerEdits.setMeals(l, id, start, 2, false)
        l = LedgerEdits.setMeals(l, id, start + 1, 0, true)
        l = LedgerEdits.addCharge(l, id, "Maid, gas", 100_000, SplitRule.PER_MEMBER, "split \"5 ways\"")
        l = LedgerEdits.addTxn(l, id, start, DepositKind.PAYMENT, 1_000)
        l = LedgerEdits.setStated(l, id, 60_000, "written on the notice board")
        return l
    }

    @Test
    fun `every csv row parses back to the header's eight columns`() {
        val l = ledger()
        val text = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
        for (line in text.trim().lines().drop(1)) {
            val columns = Csv.split(line)
            assertEquals("wrong column count in: $line", 8, columns.size)
        }
    }

    @Test
    fun `commas and quotes in user text stay inside one field`() {
        val l = ledger()
        val row = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
            .trim().lines().first { it.startsWith("charge,") }
        val columns = Csv.split(row)
        assertEquals("Maid, gas", columns[3])
        assertEquals("split \"5 ways\"", columns[7])
        assertEquals(8, columns.size)
    }

    @Test
    fun `pdf text always carries the as-of stamp and the difference`() {
        val l = ledger()
        val lines = Export.pdfLines(l, id, Billing.compute(l, id), "2026-10-11")
        assertTrue(lines.any { it.startsWith("October 2026") })
        assertTrue(lines.any { it.contains("Prepared 2026-10-11") && it.contains("not sent anywhere") })
        assertTrue(lines.any { it.contains("Difference") && it.contains("above my count") })
        assertTrue(lines.any { it.contains("Due") })
    }

    @Test
    fun `a settled month says settled, not due`() {
        var l = ledger()
        // computed is 450.00 and 10.00 was already paid, so 440.00 settles it
        l = LedgerEdits.addTxn(l, id, end, DepositKind.PAYMENT, 44_000)
        val r = Billing.compute(l, id)
        assertEquals(Settle.SETTLED, r.settle)
        assertTrue(Export.pdfLines(l, id, r, "2026-10-11").any { it.startsWith("Settled") })
    }
}
