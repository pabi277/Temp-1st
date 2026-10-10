package com.pabi277.messbillcheck.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.Cal
import com.pabi277.messbillcheck.core.Ledger
import com.pabi277.messbillcheck.core.LedgerStore

/**
 * Shared chrome: the in-app language, the five-screen navigation strip, plain view helpers and
 * the empty state every screen shows before a month exists. Framework views only, so v1 needs no
 * androidx, no Compose compiler plugin and no permission.
 */
abstract class ScreenActivity : Activity() {

    protected lateinit var store: LedgerStore

    override fun attachBaseContext(newBase: Context) {
        val tag = Prefs.language(newBase)
        super.attachBaseContext(AppLocale.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LedgerHost.store(this)
    }

    protected fun ledger(): Ledger = store.load()

    protected fun periodIdOrNull(): String? = LedgerHost.activePeriod(ledger())?.id

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    protected fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
    )

    protected fun textRow(label: String, value: String, small: Boolean = false): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), if (small) dp(4) else dp(8), dp(16), if (small) dp(4) else dp(8))
            minimumHeight = dp(44)
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            TextView(this).apply {
                text = label
                textSize = if (small) 13f else 15f
                setTextColor(getColor(if (small) android.R.color.darker_gray else android.R.color.black))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        row.addView(
            TextView(this).apply {
                text = value
                textSize = if (small) 13f else 15f
                setTypeface(typeface, if (small) 0 else android.graphics.Typeface.BOLD)
                gravity = Gravity.END
            }
        )
        return row
    }

    protected fun heading(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(16), dp(16), dp(16), dp(4))
    }

    protected fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        setPadding(dp(16), dp(6), dp(16), dp(6))
        setTextIsSelectable(true)
    }

    protected fun button(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        minimumHeight = dp(48)
        isAllCaps = false
        contentDescription = label
        setOnClickListener { onClick() }
    }

    protected fun wrap(scroll: LinearLayout): ScrollView = ScrollView(this).apply {
        addView(scroll)
    }

    /** The stack is five screens deep at most; switching is a plain intent. */
    protected fun navStrip(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(getColor(android.R.color.background_light))
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        val screens = listOf(
            getString(R.string.nav_today) to TodayActivity::class.java,
            getString(R.string.nav_bill) to BillActivity::class.java,
            getString(R.string.nav_month) to MonthActivity::class.java,
            getString(R.string.nav_money) to MoneyActivity::class.java,
            getString(R.string.nav_settings) to SettingsActivity::class.java
        )
        for ((label, target) in screens) {
            val isCurrent = javaClass == target
            bar.addView(
                Button(this).apply {
                    text = label
                    isAllCaps = false
                    textSize = 12f
                    minWidth = 0
                    minimumWidth = 0
                    setPadding(dp(4), dp(6), dp(4), dp(6))
                    contentDescription = label
                    setTypeface(typeface, if (isCurrent) android.graphics.Typeface.BOLD else 0)
                    setOnClickListener {
                        if (!isCurrent) {
                            startActivity(Intent(this@ScreenActivity, target))
                        }
                    }
                }
            )
        }
        return bar
    }

    protected fun layout(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp(24))
    }

    /** Root: nav strip on top, then scrollable content. */
    protected fun root(content: LinearLayout): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(navStrip())
            addView(wrap(content))
        }
        return column
    }

    protected fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    protected fun startMonthIfNeeded(): Boolean {
        if (periodIdOrNull() != null) return true
        val today = Cal.today()
        val ymd = Cal.fromEpochDay(today)
        val first = Cal.epochDay(ymd[0], ymd[1], 1)
        val last = first + Cal.daysInMonth(ymd[0], ymd[1]) - 1
        val d = ledger().defaults
        store.mutate { l ->
            com.pabi277.messbillcheck.core.LedgerEdits.newPeriod(
                l, "%04d-%02d".format(ymd[0], ymd[1]), first, last,
                d.rateMinor, d.basis, d.rebateMinDays, d.rounding, d.memberCount
            )
        }
        toast(getString(R.string.month_started, "%04d-%02d".format(ymd[0], ymd[1])))
        return true
    }
}
