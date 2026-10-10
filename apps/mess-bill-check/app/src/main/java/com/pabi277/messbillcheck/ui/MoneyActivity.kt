package com.pabi277.messbillcheck.ui

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.Billing
import com.pabi277.messbillcheck.core.Cal
import com.pabi277.messbillcheck.core.DepositKind
import com.pabi277.messbillcheck.core.LedgerEdits
import com.pabi277.messbillcheck.core.Money
import com.pabi277.messbillcheck.core.Settle

class MoneyActivity : ScreenActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val l = ledger()
        val period = LedgerHost.activePeriod(l)
        val col = layout()
        col.addView(heading(getString(R.string.money_title)))
        if (period == null) {
            col.addView(body(getString(R.string.err_no_period)))
            setContentView(root(col))
            return
        }

        val r = Billing.compute(l, period.id)
        val settle = when (r.settle) {
            Settle.DUE -> getString(R.string.money_due)
            Settle.ADVANCE -> getString(R.string.money_advance)
            Settle.SETTLED -> getString(R.string.money_settled)
        }
        col.addView(textRow(settle, Money.formatRupees(if (r.settle == Settle.ADVANCE) -r.balanceMinor else r.balanceMinor)))
        col.addView(body(getString(R.string.money_balance, Money.formatRupees(r.balanceMinor))))

        val txns = l.txnsOf(period.id)
        if (txns.isEmpty()) {
            col.addView(body(getString(R.string.money_empty)))
        }
        for (t in txns) {
            val label = getString(
                R.string.money_line,
                Cal.formatIso(t.epochDay),
                if (t.kind == DepositKind.DEPOSIT) getString(R.string.money_deposit) else getString(R.string.money_payment)
            )
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), dp(6), dp(8), dp(6))
                minimumHeight = dp(48)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            row.addView(
                android.widget.TextView(this).apply {
                    text = "$label ${Money.formatRupees(t.amountMinor)}" + if (t.note.isBlank()) "" else "\n${t.note}"
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
            if (!period.closed) {
                row.addView(button(getString(R.string.money_delete)) {
                    store.mutate { LedgerEdits.removeTxn(it, period.id, t.id) }
                    render()
                })
            }
            col.addView(row)
        }

        val field = EditText(this).apply {
            hint = getString(R.string.money_amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            contentDescription = getString(R.string.money_amount_hint)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        fun add(kind: DepositKind) {
            val amount = Money.parseRupees(field.text.toString())
            if (amount == null || amount <= 0) {
                toast(getString(R.string.err_amount))
                return
            }
            if (period.closed) {
                toast(getString(R.string.err_closed))
                return
            }
            store.mutate { LedgerEdits.addTxn(it, period.id, Cal.today(), kind, amount) }
            render()
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(button(getString(R.string.money_deposit)) { add(DepositKind.DEPOSIT) })
        row.addView(button(getString(R.string.money_payment)) { add(DepositKind.PAYMENT) })
        col.addView(row)
        setContentView(root(col))
    }
}
