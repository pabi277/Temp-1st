package com.pabi277.messbillcheck.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.LinearLayout
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.Billing
import com.pabi277.messbillcheck.core.Cal
import com.pabi277.messbillcheck.core.LedgerEdits
import com.pabi277.messbillcheck.core.LedgerJson
import com.pabi277.messbillcheck.core.MealEntry
import com.pabi277.messbillcheck.core.StorageException

class MonthActivity : ScreenActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun state(entry: MealEntry?): String = when {
        entry == null -> "-"
        entry.isLeave -> getString(R.string.btn_leave)
        entry.halves == 0 -> getString(R.string.btn_absent)
        entry.halves == 1 -> getString(R.string.btn_half)
        entry.halves == 2 -> getString(R.string.btn_full)
        else -> "${entry.halves / 2.0}"
    }

    private fun render() {
        val l = ledger()
        val period = LedgerHost.activePeriod(l)
        val col = layout()
        col.addView(heading(getString(R.string.month_title)))
        if (period == null) {
            col.addView(body(getString(R.string.no_period_body)))
            col.addView(button(getString(R.string.cta_start_month)) {
                startMonthIfNeeded()
                render()
            })
            setContentView(root(col))
            return
        }

        val r = Billing.compute(l, period.id)
        col.addView(body("${period.label} · ${getString(if (period.closed) R.string.month_closed_badge else R.string.month_open_badge)}"))
        col.addView(body(getString(R.string.month_days, period.dayCount, r.daysPresent, r.rebateDays)))
        col.addView(body(getString(R.string.month_head)))

        var day = period.startDay
        while (day <= period.endDay) {
            val entry = l.mealOn(period.id, day)
            val label = "%s  %s".format(Cal.formatIso(day), state(entry))
            val row = textRow(label, if (entry?.isLeave == true) "!" else "", small = true)
            if (!period.closed) {
                row.setOnClickListener {
                    val next = when {
                        entry == null -> 2
                        entry.halves == 2 -> 1
                        entry.halves == 1 -> 0
                        else -> 2
                    }
                    store.mutate { LedgerEdits.setMeals(it, period.id, day, next, false) }
                    render()
                }
            }
            col.addView(row)
            day += 1
        }

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (period.closed) {
            actions.addView(button(getString(R.string.month_reopen)) {
                store.mutate { l2 -> LedgerEdits.reopen(l2, period.id).first }
                render()
            })
        } else {
            actions.addView(button(getString(R.string.month_close)) { confirmClose(period.id, period.label) })
        }
        col.addView(actions)
        for (w in r.warnings) col.addView(body("! $w"))
        setContentView(root(col))
    }

    private fun confirmClose(id: String, label: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.close_confirm_title, label))
            .setMessage(getString(R.string.close_confirm_body))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val updated = store.mutate { LedgerEdits.close(it, id) }
                try {
                    store.writeSnapshot(id, LedgerJson.write(updated))
                } catch (e: StorageException) {
                    toast(e.message ?: "snapshot")
                }
                toast(getString(R.string.month_closed_toast, label))
                render()
            }
            .show()
    }
}
