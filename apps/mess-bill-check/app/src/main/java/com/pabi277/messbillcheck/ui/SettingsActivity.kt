package com.pabi277.messbillcheck.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.view.View
import com.pabi277.messbillcheck.R
import com.pabi277.messbillcheck.core.BillRounding
import com.pabi277.messbillcheck.core.Billing
import com.pabi277.messbillcheck.core.Cal
import com.pabi277.messbillcheck.core.Defaults
import com.pabi277.messbillcheck.core.Export
import com.pabi277.messbillcheck.core.LedgerEdits
import com.pabi277.messbillcheck.core.Money
import com.pabi277.messbillcheck.core.RateBasis
import com.pabi277.messbillcheck.core.StorageException

class SettingsActivity : ScreenActivity() {

    private val pickFile = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val l = ledger()
        val d = l.defaults
        val col = layout()
        col.addView(heading(getString(R.string.set_title)))

        val rate = EditText(this).apply {
            hint = getString(R.string.set_rate)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(Money.formatRupees(d.rateMinor).removePrefix("\u20B9"))
            contentDescription = getString(R.string.set_rate)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val members = EditText(this).apply {
            hint = getString(R.string.set_members)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(d.memberCount.toString())
            contentDescription = getString(R.string.set_members)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val rebate = EditText(this).apply {
            hint = getString(R.string.set_rebate)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(d.rebateMinDays.toString())
            contentDescription = getString(R.string.set_rebate)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        col.addView(rate)
        col.addView(members)
        col.addView(rebate)

        val basis = choices(
            listOf(
                RateBasis.MEALS.name to getString(R.string.basis_meals),
                RateBasis.DAYS_PRESENT.name to getString(R.string.basis_days),
                RateBasis.FIXED_MONTH.name to getString(R.string.basis_fixed)
            ),
            d.basis.name
        )
        col.addView(heading(getString(R.string.set_basis)))
        col.addView(basis.group)

        val rounding = choices(
            listOf(
                BillRounding.NEAREST.name to getString(R.string.round_nearest),
                BillRounding.FLOOR.name to getString(R.string.round_floor),
                BillRounding.CEILING.name to getString(R.string.round_ceiling)
            ),
            d.rounding.name
        )
        col.addView(heading(getString(R.string.set_rounding)))
        col.addView(rounding.group)

        val language = choices(
            listOf(
                "en" to getString(R.string.lang_en),
                "hi" to getString(R.string.lang_hi),
                "bn" to getString(R.string.lang_bn)
            ),
            Prefs.language(this)
        )
        col.addView(heading(getString(R.string.set_language)))
        col.addView(language.group)

        col.addView(button(getString(R.string.set_save)) {
            val newRate = Money.parseRupees(rate.text.toString())
            if (newRate == null || newRate < 0) {
                toast(getString(R.string.err_rate))
                return@button
            }
            val memberCount = members.text.toString().toIntOrNull()
            if (memberCount == null || memberCount < 1) {
                toast(getString(R.string.err_members))
                return@button
            }
            val rebateDays = rebate.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            val basis = RateBasis.values().firstOrNull { it.name == basis.selected() } ?: d.basis
            val rounding = BillRounding.values().firstOrNull { it.name == rounding.selected() } ?: d.rounding
            val pickedLanguage = language.selected() ?: Prefs.language(this)
            Prefs.setLanguage(this, pickedLanguage)
            val languageSelectedAtOpen = Prefs.language(this)
            val active = LedgerHost.activePeriod(l)
            store.mutate { current ->
                var next = LedgerEdits.setDefaults(
                    current, Defaults(newRate, basis, rebateDays, rounding, memberCount, language)
                )
                if (active != null && !active.closed) {
                    next = next.copy(
                        periods = next.periods.map {
                            if (it.id == active.id) it.copy(
                                rateMinor = newRate, basis = basis, rebateMinDays = rebateDays,
                                rounding = rounding, memberCount = memberCount
                            ) else it
                        }
                    )
                }
                next
            }
            toast(getString(R.string.set_saved))
            // A locale change has to rebuild the whole activity, not just its content.
            if (pickedLanguage != languageSelectedAtOpen) recreate() else render()
        })

        col.addView(heading(getString(R.string.set_export_csv)))
        col.addView(button(getString(R.string.set_export_csv)) { export(ExportKind.CSV) })
        col.addView(button(getString(R.string.set_export_pdf)) { export(ExportKind.PDF) })
        col.addView(button(getString(R.string.set_backup)) { export(ExportKind.JSON) })
        col.addView(button(getString(R.string.set_restore)) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, pickFile)
        })
        col.addView(button(getString(R.string.set_licenses)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.set_licenses)
                .setMessage(R.string.about_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })
        setContentView(root(col))
    }

    private class Choice(val group: RadioGroup, private val tags: Map<Int, String>) {
        fun selected(): String? = tags[group.checkedRadioButtonId]
    }

    /** RadioGroup needs real view ids for mutual exclusivity; generateViewId keeps them unique. */
    private fun choices(options: List<Pair<String, String>>, selectedTag: String): Choice {
        val group = RadioGroup(this)
        val tags = LinkedHashMap<Int, String>()
        for ((tag, label) in options) {
            val rb = RadioButton(this).apply {
                text = label
                this.tag = tag
                id = View.generateViewId()
                isChecked = tag == selectedTag
                textSize = 16f
                minimumHeight = dp(48)
                contentDescription = label
            }
            tags[rb.id] = tag
            group.addView(rb)
        }
        return Choice(group, tags)
    }

    private enum class ExportKind { CSV, PDF, JSON }

    private fun export(kind: ExportKind) {
        val l = ledger()
        val period = LedgerHost.activePeriod(l)
        if (period == null) {
            toast(getString(R.string.err_no_period))
            return
        }
        val asOf = Cal.formatIso(Cal.today())
        val r = Billing.compute(l, period.id)
        val slug = period.label.replace(Regex("[^A-Za-z0-9.-]"), "-")
        val result = when (kind) {
            ExportKind.CSV -> SaveFiles.save(
                this, "mess-bill-$slug.csv", "text/csv", Export.csv(l, period.id, r, asOf).toByteArray()
            )

            ExportKind.PDF -> SaveFiles.save(
                this, "mess-bill-$slug.pdf", "application/pdf",
                SaveFiles.renderPdf(Export.pdfLines(l, period.id, r, asOf))
            )

            ExportKind.JSON -> SaveFiles.save(
                this, "mess-bill-check-backup-$asOf.json", "application/json",
                store.exportBackup(l).toByteArray()
            )
        }
        if (result == null) {
            toast(getString(R.string.export_failed))
        } else {
            toast(getString(R.string.export_saved, result.where))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFile || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val text = try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
        if (text.isNullOrBlank()) {
            toast(getString(R.string.restore_failed))
            return
        }
        try {
            val restored = store.importBackup(text)
            store.save(restored)
            toast(getString(R.string.restore_ok))
            render()
        } catch (e: StorageException) {
            toast(if (e.message?.contains("newer app") == true) getString(R.string.restore_newer) else getString(R.string.restore_failed))
        } catch (e: Exception) {
            toast(getString(R.string.restore_failed))
        }
    }
}
