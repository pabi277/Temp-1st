package com.pabi277.messbillcheck.ui

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.Billing
import com.pabi277.messbillcheck.core.Compare
import com.pabi277.messbillcheck.core.LedgerEdits
import com.pabi277.messbillcheck.core.Money
import com.pabi277.messbillcheck.core.Settle

class BillActivity : ScreenActivity() {

    private var input: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val l = ledger()
        val period = LedgerHost.activePeriod(l)
        val col = layout()
        col.addView(heading(getString(R.string.bill_title)))
        if (period == null) {
            col.addView(body(getString(R.string.err_no_period)))
            setContentView(root(col))
            return
        }

        val r = Billing.compute(l, period.id)
        val basisLabel = when (period.basis) {
            com.pabi277.messbillcheck.core.RateBasis.MEALS -> getString(R.string.basis_meals)
            com.pabi277.messbillcheck.core.RateBasis.DAYS_PRESENT -> getString(R.string.basis_days)
            com.pabi277.messbillcheck.core.RateBasis.FIXED_MONTH -> getString(R.string.basis_fixed)
        }
        col.addView(body(getString(R.string.bill_basis_note, basisLabel, Money.formatRupees(period.rateMinor), period.rebateMinDays)))
        col.addView(heading(getString(R.string.bill_head_lines)))
        for (line in r.lines) {
            col.addView(textRow(line.label, Money.formatRupees(line.amountMinor)))
            if (line.detail.isNotBlank()) col.addView(body("    ${line.detail}"))
        }
        col.addView(textRow(getString(R.string.bill_computed), Money.formatRupees(r.computedMinor)))
        r.statedMinor?.let { col.addView(textRow(getString(R.string.bill_stated), Money.formatRupees(it))) }
        val difference = when (r.compare) {
            Compare.MATCH -> if (r.statedMinor == null) getString(R.string.bill_no_stated) else getString(R.string.bill_match)
            Compare.MANAGER_HIGHER -> getString(R.string.bill_manager_higher, Money.formatRupees(-r.deltaMinor))
            Compare.MANAGER_LOWER -> getString(R.string.bill_manager_lower, Money.formatRupees(-r.deltaMinor))
        }
        col.addView(TextView(this).apply {
            text = difference
            textSize = 17f
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val settleLine = when (r.settle) {
            Settle.DUE -> getString(R.string.money_due)
            Settle.ADVANCE -> getString(R.string.money_advance)
            Settle.SETTLED -> getString(R.string.money_settled)
        }
        col.addView(textRow(settleLine, Money.formatRupees(if (r.settle == Settle.ADVANCE) -r.balanceMinor else r.balanceMinor)))
        col.addView(body(getString(R.string.bill_balance, Money.formatRupees(r.balanceMinor))))
        for (w in r.warnings) col.addView(body("! $w"))

        val entryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val field = EditText(this).apply {
            hint = getString(R.string.bill_stated_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            contentDescription = getString(R.string.bill_stated)
            setText(l.statedOf(period.id)?.let { Money.formatRupees(it.amountMinor).removePrefix("\u20B9") } ?: "")
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        input = field
        entryRow.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        entryRow.addView(button(getString(R.string.bill_save_stated)) {
            val amount = Money.parseRupees(field.text.toString())
            if (amount == null || amount < 0) {
                toast(getString(R.string.err_amount))
            } else {
                store.mutate { LedgerEdits.setStated(it, period.id, amount) }
                render()
            }
        })
        col.addView(entryRow)
        setContentView(root(col))
    }
}
