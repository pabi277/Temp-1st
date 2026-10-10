package com.pabi277.messbillcheck.ui

import android.os.Bundle
import android.widget.LinearLayout
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.Billing
import com.pabi277.messbillcheck.core.Cal
import com.pabi277.messbillcheck.core.LedgerEdits
import com.pabi277.messbillcheck.core.Money

class TodayActivity : ScreenActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val l = ledger()
        val col = layout()
        col.addView(heading(getString(R.string.app_name)))
        val period = LedgerHost.activePeriod(l)
        if (period == null) {
            col.addView(body(getString(R.string.no_period_body)))
            col.addView(button(getString(R.string.cta_start_month)) {
                startMonthIfNeeded()
                render()
            })
            setContentView(root(col))
            return
        }

        val today = Cal.today()
        val dayInPeriod = today in period.startDay..period.endDay
        col.addView(body(getString(R.string.today_state, Cal.formatIso(today))))
        if (!dayInPeriod) {
            col.addView(body(getString(R.string.err_outside)))
        }
        col.addView(body(getString(R.string.today_hint)))

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun pick(halves: Int, leave: Boolean) {
            if (!dayInPeriod) return
            store.mutate { LedgerEdits.setMeals(it, period.id, today, halves, leave) }
            render()
        }
        if (dayInPeriod) {
            row1.addView(button(getString(R.string.btn_absent)) { pick(0, false) })
            row1.addView(button(getString(R.string.btn_half)) { pick(1, false) })
            row2.addView(button(getString(R.string.btn_full)) { pick(2, false) })
            row2.addView(button(getString(R.string.btn_double)) { pick(4, false) })
            col.addView(row1)
            col.addView(row2)
            val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row3.addView(button(getString(R.string.btn_leave)) { pick(0, true) })
            row3.addView(button(getString(R.string.btn_clear)) {
                store.mutate { LedgerEdits.clearMeal(it, period.id, today) }
                render()
            })
            col.addView(row3)
        }

        val r = Billing.compute(l, period.id)
        col.addView(
            body(
                getString(R.string.today_month_so_far, mealText(r.loggedMeals), r.daysPresent, r.rebateDays)
            )
        )
        col.addView(textRow(getString(R.string.bill_computed), Money.formatRupees(r.computedMinor)))
        col.addView(textRow(getString(R.string.money_balance), Money.formatRupees(r.balanceMinor), true))
        if (period.closed) {
            col.addView(body(getString(R.string.err_closed)))
        }
        setContentView(root(col))
    }

    /** "12" for a whole count, "12.5" when a half-day is in it - never a rupee string. */
    private fun mealText(meals: Double): String =
        if (meals % 1.0 == 0.0) meals.toLong().toString() else meals.toString()
}
