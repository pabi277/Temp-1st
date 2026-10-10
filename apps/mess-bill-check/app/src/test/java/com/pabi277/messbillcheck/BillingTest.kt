package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingTest {

    private val start = Cal.epochDay(2026, 10, 1)
    private val end = Cal.epochDay(2026, 10, 31)

    private fun ledgerWith(period: Period, meals: List<MealEntry> = emptyList()) =
        Ledger(schemaVersion = Ledger.SCHEMA_VERSION, periods = listOf(period), meals = meals)

    private fun period(
        basis: RateBasis = RateBasis.DAYS_PRESENT,
        rebate: Int = 0,
        rate: Long = 25_000,
        members: Int = 5,
        rounding: BillRounding = BillRounding.NEAREST
    ) = Period("p1", "October 2026", start, end, rate, basis, rebate, rounding, members, false)

    private fun days(n: Int, halves: Int = 2) = (0 until n).map { MealEntry("p1", start + it, halves, false) }

    @Test
    fun `full month of days present at 250 rupees a day`() {
        val r = Billing.compute(ledgerWith(period(), days(28)), "p1")
        assertEquals(56, r.billableUnits)
        assertEquals(700_000L, r.computedMinor)
        assertEquals(28, r.daysPresent)
    }

    @Test
    fun `half meals are billed exactly once, never as a double`() {
        val meals = listOf(
            MealEntry("p1", start, 2, false),
            MealEntry("p1", start + 1, 1, false)
        )
        val p = period(basis = RateBasis.MEALS)
        val r = Billing.compute(ledgerWith(p, meals), "p1")
        assertEquals(3, r.billableUnits)
        assertEquals(37_500L, r.computedMinor) // 250.00 + 125.00
        assertEquals(1.5, r.loggedMeals, 1e-9)
    }

    @Test
    fun `meals basis ignores rebate days but fixed month does not`() {
        val leave = (0 until 4).map { MealEntry("p1", start + it, 0, true) }
        val meals = leave + days(20)
        val mealsBasis = Billing.compute(ledgerWith(period(RateBasis.MEALS, rebate = 3), meals), "p1")
        assertEquals(4, mealsBasis.rebateDays) // counted, but not subtracted on the meals basis
        assertEquals(40, mealsBasis.billableUnits)

        val fixed = Billing.compute(ledgerWith(period(RateBasis.FIXED_MONTH, rebate = 3), meals), "p1")
        assertEquals(4, fixed.rebateDays) // one run of 4 >= 3
        assertEquals((31 - 4) * 2, fixed.billableUnits)
    }

    @Test
    fun `short leave runs below the minimum earn no rebate`() {
        val meals = listOf(
            MealEntry("p1", start, 0, true),
            MealEntry("p1", start + 1, 0, true),
            MealEntry("p1", start + 5, 0, true)
        )
        val p = period(RateBasis.FIXED_MONTH, rebate = 3)
        val r = Billing.compute(ledgerWith(p, meals), "p1")
        assertEquals(0, r.rebateDays)
        assertEquals(31 * 2, r.billableUnits)
    }

    @Test
    fun `rebate runs are per consecutive block`() {
        assertEquals(0, Billing.rebateDays(setOf(start, start + 1), start, end, 3))
        assertEquals(3, Billing.rebateDays(setOf(start, start + 1, start + 2), start, end, 3))
        assertEquals(2, Billing.rebateDays(setOf(start, start + 1), start, end, 0))
    }

    @Test
    fun `fixed month with no meals still bills and says so`() {
        val r = Billing.compute(ledgerWith(period(RateBasis.FIXED_MONTH)), "p1")
        assertEquals(31 * 2, r.billableUnits)
        assertTrue(r.warnings.isNotEmpty())
    }

    @Test
    fun `charges respect their split rule`() {
        val l = ledgerWith(period(), days(20)).let {
            it.copy(
                charges = listOf(
                    ChargeLine("p1", "c1", "Maid", 100_000, SplitRule.PER_MEMBER),
                    ChargeLine("p1", "c2", "My extra oil", 5_000, SplitRule.FULL),
                    ChargeLine("p1", "c3", "Owner's gas", 50_000, SplitRule.EXCLUDED)
                )
            )
        }
        val r = Billing.compute(l, "p1")
        assertEquals(20_000L + 5_000L, r.chargesMinor)
        assertEquals(500_000L + 25_000L, r.computedMinor)
    }

    @Test
    fun `a period that does not start on the first of a month counts days correctly`() {
        val s = Cal.epochDay(2026, 1, 15)
        val e = Cal.epochDay(2026, 2, 14)
        val p = Period("px", "Mid Jan to Mid Feb", s, e, 25_000, RateBasis.FIXED_MONTH, 0, BillRounding.NEAREST, 1, false)
        val r = Billing.compute(Ledger(Ledger.SCHEMA_VERSION, periods = listOf(p)), "px")
        assertEquals(31, p.dayCount)
        assertEquals(31 * 2 * 25_000 / 2, r.computedMinor)
    }

    @Test
    fun `february in a leap year bills 29 days`() {
        val s = Cal.epochDay(2024, 2, 1)
        val e = Cal.epochDay(2024, 2, 29)
        val p = Period("pleap", "Feb 2024", s, e, 10_000, RateBasis.FIXED_MONTH, 0, BillRounding.NEAREST, 1, false)
        val r = Billing.compute(Ledger(Ledger.SCHEMA_VERSION, periods = listOf(p)), "pleap")
        assertEquals(29, p.dayCount)
        assertEquals(290_000L, r.computedMinor)
    }

    @Test
    fun `manager asking more shows as higher with the rupee difference`() {
        val l = ledgerWith(period(), days(20)).copy(stated = listOf(StatedBill("p1", 600_000)))
        val r = Billing.compute(l, "p1")
        assertEquals(500_000L, r.computedMinor)
        assertEquals(-100_000L, r.deltaMinor)
        assertEquals(Compare.MANAGER_HIGHER, r.compare)
    }

    @Test
    fun `identical bills are a match and no statement is neutral`() {
        val matched = ledgerWith(period(), days(20)).copy(stated = listOf(StatedBill("p1", 500_000)))
        assertEquals(Compare.MATCH, Billing.compute(matched, "p1").compare)
        val none = Billing.compute(ledgerWith(period(), days(20)), "p1")
        assertEquals(null, none.statedMinor)
        assertEquals(0L, none.deltaMinor)
        assertFalse(none.compare == Compare.MANAGER_LOWER)
    }

    @Test
    fun `deposits above the bill become an advance, below it a due`() {
        val base = ledgerWith(period(), days(20)) // computed 5000.00
        val owed = base.copy(txns = listOf(Txn("p1", "t1", start, DepositKind.PAYMENT, 100_000)))
        val r1 = Billing.compute(owed, "p1")
        assertEquals(Settle.DUE, r1.settle)
        assertEquals(400_000L, r1.balanceMinor)

        val prepaid = base.copy(txns = listOf(Txn("p1", "t1", start, DepositKind.DEPOSIT, 700_000)))
        val r2 = Billing.compute(prepaid, "p1")
        assertEquals(Settle.ADVANCE, r2.settle)
        assertEquals(-200_000L, r2.balanceMinor)

        val square = base.copy(txns = listOf(Txn("p1", "t1", start, DepositKind.DEPOSIT, 500_000)))
        assertEquals(Settle.SETTLED, Billing.compute(square, "p1").settle)
    }

    @Test
    fun `entries dated outside the period are excluded and reported`() {
        val meals = days(20) + listOf(MealEntry("p1", end + 3, 2, false))
        val r = Billing.compute(ledgerWith(period(), meals), "p1")
        assertEquals(20, r.daysPresent)
        assertTrue(r.warnings.any { it.contains("outside the period") })
    }

    @Test
    fun `a one-member mess pays the whole shared charge`() {
        val p = period(RateBasis.DAYS_PRESENT, members = 1)
        val l = ledgerWith(p, days(1)).copy(
            periods = listOf(p.copy(memberCount = 1)),
            charges = listOf(ChargeLine("p1", "c1", "Maid", 10_000, SplitRule.PER_MEMBER))
        )
        assertEquals(10_000L, Billing.compute(l, "p1").chargesMinor)
    }
}
