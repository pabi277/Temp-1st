package com.pabi277.messbillcheck.core

enum class RateBasis {
    /** rate x meals actually logged (half-meals count as 0.5). */
    MEALS,

    /** rate x days on which at least one meal was logged, rebate days removed. */
    DAYS_PRESENT,

    /** rate x every day in the period, rebate days removed - the "fixed month" mess style. */
    FIXED_MONTH
}

enum class BillRounding { NEAREST, FLOOR, CEILING }

enum class SplitRule {
    /** charged to me in full (e.g. my own extra items) */
    FULL,

    /** divided across the mess by member count (e.g. maid, gas, water) */
    PER_MEMBER,

    /** shown but not billed to me (e.g. owner-side costs) */
    EXCLUDED
}

enum class DepositKind { DEPOSIT, PAYMENT }

data class MealEntry(val periodId: String, val epochDay: Long, val halves: Int, val isLeave: Boolean, val note: String = "") {
    init {
        require(halves >= 0 && halves % 1 == 0) { "halves must be >= 0, was $halves" }
    }

    /** Whole-meal equivalent, e.g. 1 full + 1 half = 1.5. */
    val mealUnits: Double get() = halves / 2.0
}

data class ChargeLine(
    val periodId: String,
    val id: String,
    val name: String,
    val amountMinor: Minor,
    val split: SplitRule,
    val note: String = ""
)

data class Txn(
    val periodId: String,
    val id: String,
    val epochDay: Long,
    val kind: DepositKind,
    val amountMinor: Minor,
    val note: String = ""
)

data class StatedBill(val periodId: String, val amountMinor: Minor, val sourceNote: String = "")

data class Period(
    val id: String,
    val label: String,
    val startDay: Long,
    val endDay: Long,
    val rateMinor: Minor,
    val basis: RateBasis,
    val rebateMinDays: Int,
    val rounding: BillRounding,
    val memberCount: Int,
    val closed: Boolean
) {
    init {
        require(endDay >= startDay) { "period $label ends before it starts" }
        require(memberCount > 0) { "memberCount must be positive" }
        require(rateMinor >= 0) { "rate must not be negative" }
        require(rebateMinDays >= 0) { "rebateMinDays must be >= 0" }
    }

    val dayCount: Int get() = (endDay - startDay + 1).toInt()
}

data class Defaults(
    val rateMinor: Minor,
    val basis: RateBasis,
    val rebateMinDays: Int,
    val rounding: BillRounding,
    val memberCount: Int,
    val languageTag: String
)

/**
 * The whole app state. Flat lists keyed by periodId keep the serialised form trivial and
 * make "one row per date per period" a property the store can enforce and test.
 */
data class Ledger(
    val schemaVersion: Int,
    val periods: List<Period> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val charges: List<ChargeLine> = emptyList(),
    val txns: List<Txn> = emptyList(),
    val stated: List<StatedBill> = emptyList(),
    val defaults: Defaults = Defaults(25_000L, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5, "en")
) {
    fun period(id: String): Period? = periods.firstOrNull { it.id == id }

    fun mealsOf(id: String): List<MealEntry> = meals.filter { it.periodId == id }.sortedBy { it.epochDay }

    /** Only one meal row per date: the newest write wins. */
    fun mealOn(id: String, epochDay: Long): MealEntry? = meals.firstOrNull { it.periodId == id && it.epochDay == epochDay }

    fun chargesOf(id: String): List<ChargeLine> = charges.filter { it.periodId == id }

    fun txnsOf(id: String): List<Txn> = txns.filter { it.periodId == id }.sortedBy { it.epochDay }

    fun statedOf(id: String): StatedBill? = stated.firstOrNull { it.periodId == id }

    companion object {
        const val SCHEMA_VERSION = 1

        /** A newer file was written by a newer app: refuse it, never truncate it. */
        fun canRestoreFrom(fileVersion: Int): Boolean = fileVersion <= SCHEMA_VERSION
    }
}
