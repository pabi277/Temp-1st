package com.pabi277.messbillcheck.core

enum class Compare { MATCH, MANAGER_HIGHER, MANAGER_LOWER }

enum class Settle { DUE, ADVANCE, SETTLED }

data class BillLine(val label: String, val detail: String, val amountMinor: Minor)

/**
 * Deterministic bill maths: the whole point of the app, so it is pure Kotlin with no
 * Android, no clock and no locale access. Everything a user disputes is computed here.
 */
object Billing {

    data class Result(
        val periodId: String,
        val rateMinor: Minor,
        val basis: RateBasis,
        val rounding: BillRounding,
        val rebateMinDays: Int,
        val loggedMeals: Double,
        val daysPresent: Int,
        val rebateDays: Int,
        val billableUnits: Int,
        val mealCostMinor: Minor,
        val chargesMinor: Minor,
        val computedMinor: Minor,
        val statedMinor: Minor?,
        val deltaMinor: Minor,
        val compare: Compare,
        val depositsMinor: Minor,
        val paymentsMinor: Minor,
        val balanceMinor: Minor,
        val settle: Settle,
        val lines: List<BillLine>,
        val warnings: List<String>
    )

    /** Consecutive leave runs of at least `minDays` days are the only rebate-eligible days. */
    fun rebateDays(leaveDays: Set<Long>, startDay: Long, endDay: Long, minDays: Int): Int {
        if (leaveDays.isEmpty()) return 0
        // "no minimum" means every leave day counts, so the run length needed is 1.
        val needed = if (minDays <= 0) 1 else minDays
        var rebate = 0
        var run = 0
        var day = startDay
        while (day <= endDay) {
            if (leaveDays.contains(day)) {
                run += 1
            } else {
                if (run >= needed) rebate += run
                run = 0
            }
            day += 1
        }
        if (run >= needed) rebate += run
        return rebate
    }

    fun compute(ledger: Ledger, periodId: String): Result {
        val period = ledger.period(periodId)
            ?: throw IllegalArgumentException("unknown period: $periodId")
        val meals = ledger.mealsOf(periodId)
        val warnings = mutableListOf<String>()

        val outside = meals.filter { it.epochDay < period.startDay || it.epochDay > period.endDay }
        if (outside.isNotEmpty()) warnings.add("${outside.size} meal entries fall outside the period and were not billed")

        val leaveDays = meals.filter { it.isLeave }.map { it.epochDay }.toSet()
        val inRange = meals.filter { it.epochDay in period.startDay..period.endDay }
        val activeDays = inRange.filter { !it.isLeave && it.halves > 0 }.map { it.epochDay }.toSet()
        val loggedHalves = inRange.filter { !it.isLeave }.sumOf { it.halves }
        val rebateDays = rebateDays(leaveDays, period.startDay, period.endDay, period.rebateMinDays)

        val billableUnits: Int = when (period.basis) {
            // rate x meals logged, expressed in halves so the halving happens once, at rounding
            RateBasis.MEALS -> loggedHalves
            RateBasis.DAYS_PRESENT -> {
                val present = inRange.filter { !it.isLeave && it.halves > 0 }.size
                present * 2
            }
            RateBasis.FIXED_MONTH -> {
                val days = period.dayCount - rebateDays
                if (days < 0) 0 else days * 2
            }
        }

        val numerator = period.rateMinor * billableUnits
        val mealCost = Money.halfRound(numerator, period.rounding)

        val lines = mutableListOf<BillLine>()
        lines.add(
            BillLine(
                label = "meals",
                detail = "rate ${Money.formatRupees(period.rateMinor)} x $billableUnits halves (${loggedHalves / 2.0} meals, " +
                    "$rebateDays rebate days)",
                amountMinor = mealCost
            )
        )

        var chargesTotal: Minor = 0
        for (charge in ledger.chargesOf(periodId)) {
            val applied: Minor = when (charge.split) {
                SplitRule.FULL -> charge.amountMinor
                SplitRule.PER_MEMBER -> {
                    if (period.memberCount <= 0) {
                        warnings.add("charge '${charge.name}' needs a member count above zero and was not split")
                        0L
                    } else {
                        Money.divRound(charge.amountMinor, period.memberCount, period.rounding)
                    }
                }
                SplitRule.EXCLUDED -> 0L
            }
            chargesTotal += applied
            lines.add(
                BillLine(
                    label = charge.name,
                    detail = "${charge.split.name} ${Money.formatRupees(charge.amountMinor)} -> ${Money.formatRupees(applied)}",
                    amountMinor = applied
                )
            )
        }

        val computed = mealCost + chargesTotal
        val stated = ledger.statedOf(periodId)?.amountMinor
        val delta = if (stated == null) 0L else computed - stated
        val compare = when {
            stated == null -> Compare.MATCH
            delta == 0L -> Compare.MATCH
            stated > computed -> Compare.MANAGER_HIGHER
            else -> Compare.MANAGER_LOWER
        }

        val txns = ledger.txnsOf(periodId)
        val deposits = txns.filter { it.kind == DepositKind.DEPOSIT }.sumOf { it.amountMinor }
        val payments = txns.filter { it.kind == DepositKind.PAYMENT }.sumOf { it.amountMinor }
        val balance = computed - payments - deposits
        val settle = when {
            balance > 0 -> Settle.DUE
            balance < 0 -> Settle.ADVANCE
            else -> Settle.SETTLED
        }

        if (activeDays.isEmpty() && period.basis == RateBasis.FIXED_MONTH) {
            warnings.add("no meals logged, but the fixed-month basis still bills the whole period")
        }

        return Result(
            periodId = periodId,
            rateMinor = period.rateMinor,
            basis = period.basis,
            rounding = period.rounding,
            rebateMinDays = period.rebateMinDays,
            loggedMeals = loggedHalves / 2.0,
            daysPresent = activeDays.size,
            rebateDays = rebateDays,
            billableUnits = billableUnits,
            mealCostMinor = mealCost,
            chargesMinor = chargesTotal,
            computedMinor = computed,
            statedMinor = stated,
            deltaMinor = delta,
            compare = compare,
            depositsMinor = deposits,
            paymentsMinor = payments,
            balanceMinor = balance,
            settle = settle,
            lines = lines,
            warnings = warnings
        )
    }
}
