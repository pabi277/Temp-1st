package com.pabi277.messbillcheck.core

/** CSV and the PDF text model. Kept free of Android so both are unit-testable. */
object Export {

    fun csv(l: Ledger, periodId: String, r: Billing.Result, asOfIso: String): String {
        val period = l.period(periodId) ?: throw IllegalArgumentException("unknown period: $periodId")
        val sb = StringBuilder()
        sb.append("mess-bill-check,1\n")
        sb.append(row("kind", "section", "date", "label", "meals", "amount_rupees", "rule", "note"))
        sb.append(row("meta", "period", "", period.label, "", "", "", ""))
        sb.append(row("meta", "as_of", asOfIso, "", "", "", "", ""))
        sb.append(row("meta", "basis", "", period.basis.name, "", "", "${period.rebateMinDays}_day_rebate_minimum", ""))
        for (e in l.mealsOf(periodId)) {
            sb.append(
                row(
                    "meal",
                    if (e.isLeave) "leave" else "meals",
                    Cal.formatIso(e.epochDay),
                    e.note,
                    decimal(e.halves.toLong()),
                    "",
                    "",
                    ""
                )
            )
        }
        for (c in l.chargesOf(periodId)) {
            sb.append(
                row(
                    "charge",
                    c.split.name.lowercase(),
                    "",
                    c.name,
                    "",
                    decimal(c.amountMinor),
                    c.split.name,
                    c.note
                )
            )
        }
        for (t in l.txnsOf(periodId)) {
            sb.append(
                row(
                    "txn",
                    t.kind.name.lowercase(),
                    Cal.formatIso(t.epochDay),
                    t.note,
                    "",
                    decimal(t.amountMinor),
                    "",
                    ""
                )
            )
        }
        sb.append(row("bill", "computed", "", "meals_and_charges", "", decimal(r.computedMinor), r.basis.name, ""))
        r.statedMinor?.let {
            sb.append(row("bill", "stated_by_manager", "", "manager_claim", "", decimal(it), r.compare.name, ""))
        }
        sb.append(row("bill", "delta", "", "computed_minus_stated", "", decimal(r.deltaMinor), r.compare.name, ""))
        sb.append(
            row(
                "bill",
                "balance",
                "",
                r.settle.name.lowercase(),
                "",
                decimal(r.balanceMinor),
                "deposits=" + decimal(r.depositsMinor) + ";payments=" + decimal(r.paymentsMinor),
                ""
            )
        )
        return sb.toString()
    }

    /** One place that guarantees every row has the header's 8 columns. */
    private fun row(vararg fields: String): String = fields.joinToString(",") { csv(it) }

    /** Lines the Android layer draws into a PDF; the "as of" stamp is mandatory. */
    fun pdfLines(l: Ledger, periodId: String, r: Billing.Result, asOfIso: String): List<String> {
        val period = l.period(periodId) ?: throw IllegalArgumentException("unknown period: $periodId")
        val out = mutableListOf<String>()
        out.add(period.label)
        out.add(
            "${Cal.formatIso(period.startDay)} to ${Cal.formatIso(period.endDay)}  |  " +
                "rate ${Money.formatRupees(period.rateMinor)}  |  ${basisLabel(period.basis)}"
        )
        out.add("Prepared ${asOfIso} - computed on this device, not sent anywhere")
        out.add("")
        for (line in r.lines) {
            out.add(pad(line.label) + Money.formatRupees(line.amountMinor))
            if (line.detail.isNotBlank()) out.add("    ${line.detail}")
        }
        out.add("")
        out.add(pad("My computed bill") + Money.formatRupees(r.computedMinor))
        r.statedMinor?.let { out.add(pad("Manager's bill") + Money.formatRupees(it)) }
        if (r.statedMinor != null) {
            val word = when (r.compare) {
                Compare.MATCH -> "matches my count"
                Compare.MANAGER_HIGHER -> "is ${Money.formatRupees(-r.deltaMinor)} above my count"
                Compare.MANAGER_LOWER -> "is ${Money.formatRupees(-r.deltaMinor)} below my count"
            }
            out.add(pad("Difference") + word)
        }
        out.add("")
        out.add(pad("Deposits") + Money.formatRupees(r.depositsMinor))
        out.add(pad("Payments") + Money.formatRupees(r.paymentsMinor))
        out.add(pad(if (r.settle == Settle.ADVANCE) "Advance" else if (r.settle == Settle.SETTLED) "Settled" else "Due") +
            Money.formatRupees(if (r.settle == Settle.ADVANCE) -r.balanceMinor else r.balanceMinor))
        if (r.warnings.isNotEmpty()) {
            out.add("")
            for (w in r.warnings) out.add("! $w")
        }
        return out
    }

    private fun basisLabel(b: RateBasis) = when (b) {
        RateBasis.MEALS -> "per meal logged"
        RateBasis.DAYS_PRESENT -> "per day present"
        RateBasis.FIXED_MONTH -> "fixed month, rebate days removed"
    }

    private fun pad(label: String): String = if (label.length >= 22) "$label  " else label + " ".repeat(22 - label.length)

    /** Plain `1234.50` - no grouping separators, so spreadsheets parse it. */
    private fun decimal(minor: Minor): String {
        val negative = minor < 0
        val abs = if (negative) -minor else minor
        val text = "${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
        return if (negative) "-$text" else text
    }

    private fun csv(value: String): String {
        if (value.isEmpty()) return ""
        return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
