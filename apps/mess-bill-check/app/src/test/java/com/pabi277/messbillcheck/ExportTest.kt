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
    fun `every csv row has the header's eight columns`() {
        val l = ledger()
        val text = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
        for (line in text.trim().lines()) {
            val columns = Regex("\"(?:[^\"]|\"\")*\"|[^,]+").findAll(line).count()
            assertEquals("wrong column count in: $line", 8, columns)
        }
    }

    @Test
    fun `csv has a stable header and one row per record`() {
        val l = ledger()
        val text = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
        val lines = text.trim().lines()
        assertEquals("mess-bill-check,1", lines[0])
        assertEquals("kind,section,date,label,meals,amount_rupees,rule,note", lines[1])
        assertEquals(2, lines.count { it.startsWith("meal,") })
        assertEquals(1, lines.count { it.startsWith("charge,") })
        assertEquals(1, lines.count { it.startsWith("txn,") })
        assertEquals(1, lines.count { it.startsWith("bill,computed") })
        assertEquals(1, lines.count { it.startsWith("bill,stated_by_manager") })
    }

    @Test
    fun `amounts are plain decimals so a spreadsheet parses them`() {
        val l = ledger()
        val text = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
        // meals 250.00 + maid 1000.00/5 = 200.00 -> 450.00
        assertTrue(text, text.contains(",450.00,"))
        assertFalse(text, text.contains("₹"))
        assertFalse(text, text.contains(",1,250"))
    }

    @Test
    fun `commas and quotes in user text are quoted, not spliced into columns`() {
        val l = ledger()
        val row = Export.csv(l, id, Billing.compute(l, id), "2026-10-11")
            .lines().first { it.startsWith("charge,") }
        assertTrue(row, row.contains("\"Maid, gas\""))
        assertTrue(row, row.contains("\"split \"\"5 ways\"\"\""))

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
