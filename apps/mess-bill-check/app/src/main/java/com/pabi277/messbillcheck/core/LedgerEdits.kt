package com.pabi277.messbillcheck.core

/**
 * Every state change in the app, expressed as pure Ledger -> Ledger functions, so the rules
 * that users argue about (immutability, one row per date, refusing to edit a closed month)
 * are testable without Android.
 */
object LedgerEdits {

    fun newPeriod(
        l: Ledger,
        label: String,
        startDay: Long,
        endDay: Long,
        rateMinor: Minor,
        basis: RateBasis,
        rebateMinDays: Int,
        rounding: BillRounding,
        memberCount: Int
    ): Ledger {
        require(label.isNotBlank()) { "period needs a label" }
        require(rateMinor >= 0) { "rate must not be negative" }
        require(memberCount > 0) { "memberCount must be positive" }
        val id = "p%d".format(startDay)
        require(l.period(id) == null) { "a period already starts on ${Cal.formatIso(startDay)}" }
        val overlapping = l.periods.filter { it.startDay <= endDay && startDay <= it.endDay && !it.closed }
        require(overlapping.isEmpty()) {
            "period overlaps ${overlapping.joinToString { it.label }}; periods must not overlap"
        }
        val period = Period(id, label, startDay, endDay, rateMinor, basis, rebateMinDays, rounding, memberCount, false)
        return l.copy(periods = l.periods + period)
    }

    /** Meal entries may only be written while the period is open; the newest write wins. */
    fun setMeals(l: Ledger, periodId: String, epochDay: Long, halves: Int, isLeave: Boolean, note: String = ""): Ledger {
        val period = requireOpen(l, periodId)
        require(epochDay in period.startDay..period.endDay) {
            "${Cal.formatIso(epochDay)} is outside ${period.label}"
        }
        require(halves in 0..8) { "implausible meal count for one day: $halves halves" }
        val without = l.meals.filterNot { it.periodId == periodId && it.epochDay == epochDay }
        val entry = MealEntry(periodId, epochDay, if (isLeave) 0 else halves, isLeave, note)
        return l.copy(meals = without + entry)
    }

    fun addCharge(l: Ledger, periodId: String, name: String, amountMinor: Minor, split: SplitRule, note: String = ""): Ledger {
        requireOpen(l, periodId)
        require(name.isNotBlank()) { "charge needs a name" }
        require(amountMinor >= 0) { "charge amount must not be negative" }
        val id = "c${System.nanoTime()}"
        return l.copy(charges = l.charges + ChargeLine(periodId, id, name.trim(), amountMinor, split, note))
    }

    fun removeCharge(l: Ledger, periodId: String, chargeId: String): Ledger {
        requireOpen(l, periodId)
        return l.copy(charges = l.charges.filterNot { it.periodId == periodId && it.id == chargeId })
    }

    fun addTxn(l: Ledger, periodId: String, epochDay: Long, kind: DepositKind, amountMinor: Minor, note: String = ""): Ledger {
        requireOpen(l, periodId)
        require(amountMinor > 0) { "a deposit or payment must be above zero" }
        val id = "t${System.nanoTime()}"
        return l.copy(txns = l.txns + Txn(periodId, id, epochDay, kind, amountMinor, note))
    }

    fun removeTxn(l: Ledger, periodId: String, txnId: String): Ledger {
        requireOpen(l, periodId)
        return l.copy(txns = l.txns.filterNot { it.periodId == periodId && it.id == txnId })
    }

    /** The manager's number is always editable: it is their claim, not our record. */
    fun setStated(l: Ledger, periodId: String, amountMinor: Minor?, sourceNote: String = ""): Ledger {
        require(l.period(periodId) != null) { "unknown period: $periodId" }
        val without = l.stated.filterNot { it.periodId == periodId }
        return if (amountMinor == null) {
            l.copy(stated = without)
        } else {
            require(amountMinor >= 0) { "stated bill must not be negative" }
            l.copy(stated = without + StatedBill(periodId, amountMinor, sourceNote))
        }
    }

    fun close(l: Ledger, periodId: String): Ledger {
        val period = requireOpen(l, periodId)
        val updated = period.copy(closed = true)
        return l.copy(periods = l.periods.map { if (it.id == periodId) updated else it })
    }

    /** Reopening never edits the closed period; it forks a fresh one carrying the same rules. */
    fun reopen(l: Ledger, periodId: String): Pair<Ledger, Period> {
        val period = l.period(periodId) ?: throw IllegalArgumentException("unknown period: $periodId")
        require(period.closed) { "period ${period.label} is already open" }
        val id = "p%d-r%d".format(period.startDay, System.currentTimeMillis() % 100000)
        val fresh = period.copy(id = id, label = "${period.label} (reopened)", closed = false)
        return Pair(
            l.copy(
                periods = l.periods + fresh,
                meals = l.meals.map { if (it.periodId == periodId) it.copy(periodId = fresh.id) else it },
                charges = l.charges.filter { it.periodId != periodId } +
                    l.charges.filter { it.periodId == periodId }.map { it.copy(periodId = fresh.id, id = "c${System.nanoTime()}") }
            ),
            fresh
        )
    }

    fun setDefaults(l: Ledger, defaults: Defaults): Ledger = l.copy(defaults = defaults)

    private fun requireOpen(l: Ledger, periodId: String): Period {
        val period = l.period(periodId) ?: throw IllegalArgumentException("unknown period: $periodId")
        if (period.closed) throw IllegalStateException(
            "${period.label} is closed and cannot be edited; reopen it to create a corrected month instead"
        )
        return period
    }
}
