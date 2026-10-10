package com.pabi277.messbillcheck.core

import java.io.File
import java.io.IOException

/**
 * Persistence for [Ledger]: one JSON file in app-private storage, written atomically, plus
 * immutable close snapshots that a manager cannot quietly rewrite. No database, no network.
 */
class LedgerStore(private val dir: File) {

    private val ledgerFile get() = File(dir, LEDGER_FILE)
    private val snapshotDir get() = File(dir, SNAPSHOT_DIR)

    fun load(): Ledger {
        if (!ledgerFile.isFile) return Ledger(schemaVersion = Ledger.SCHEMA_VERSION)
        val text = try {
            ledgerFile.readText()
        } catch (e: IOException) {
            throw StorageException("could not read ${ledgerFile.name}", e)
        }
        if (text.isBlank()) return Ledger(schemaVersion = Ledger.SCHEMA_VERSION)
        return LedgerJson.read(text)
    }

    fun save(ledger: Ledger) {
        if (!dir.isDirectory && !dir.mkdirs()) throw StorageException("could not create $dir", null)
        val tmp = File(dir, "$LEDGER_FILE.tmp")
        tmp.writeText(LedgerJson.write(ledger))
        val backup = File(dir, "$LEDGER_FILE.prev")
        if (ledgerFile.isFile) {
            if (backup.exists()) backup.delete()
            if (!ledgerFile.renameTo(backup)) ledgerFile.delete()
        }
        if (!tmp.renameTo(ledgerFile)) {
            tmp.copyTo(ledgerFile, overwrite = true)
            tmp.delete()
        }
    }

    fun mutate(block: (Ledger) -> Ledger): Ledger = synchronized(this) {
        val next = block(load())
        save(next)
        next
    }

    /** Snapshots are write-once: a second close of the same period is an error, not an overwrite. */
    fun writeSnapshot(periodId: String, payload: String): File {
        if (!snapshotDir.isDirectory && !snapshotDir.mkdirs()) throw StorageException("could not create $snapshotDir", null)
        val target = File(snapshotDir, "$periodId.json")
        if (target.exists()) throw StorageException("snapshot for $periodId already exists; closed months are immutable", null)
        target.writeText(payload)
        return target
    }

    fun snapshotExists(periodId: String): Boolean = File(snapshotDir, "$periodId.json").isFile

    fun readSnapshot(periodId: String): String? = File(snapshotDir, "$periodId.json").takeIf { it.isFile }?.readText()

    fun exportBackup(ledger: Ledger): String = LedgerJson.write(ledger)

    /** @throws StorageException when the file came from a newer app version. */
    fun importBackup(text: String): Ledger {
        val version = LedgerJson.schemaVersion(text)
        if (!Ledger.canRestoreFrom(version)) {
            throw StorageException(
                "this backup was written by a newer app (schema $version > ${Ledger.SCHEMA_VERSION}); install the update first",
                null
            )
        }
        return LedgerJson.read(text)
    }

    companion object {
        const val LEDGER_FILE = "ledger.json"
        const val SNAPSHOT_DIR = "snapshots"
    }
}

class StorageException(message: String, cause: Throwable?) : RuntimeException(message, cause)

/** Serialisation of [Ledger]. Public so tests and the export layer share one format. */
object LedgerJson {

    fun schemaVersion(text: String): Int = JsonIo.parse(text).int("schemaVersion", 0)

    fun write(l: Ledger): String = JsonIo.write(toJson(l))

    fun read(text: String): Ledger = fromJson(JsonIo.parse(text))

    fun toJson(l: Ledger): Json {
        val root = JsonIo.obj { m ->
            JsonIo.put(m, "schemaVersion", l.schemaVersion)
            m["periods"] = JsonIo.arr(l.periods.map { p ->
                JsonIo.obj { q ->
                    JsonIo.put(q, "id", p.id)
                    JsonIo.put(q, "label", p.label)
                    JsonIo.put(q, "startDay", p.startDay)
                    JsonIo.put(q, "endDay", p.endDay)
                    JsonIo.put(q, "rateMinor", p.rateMinor)
                    JsonIo.put(q, "basis", p.basis.name)
                    JsonIo.put(q, "rebateMinDays", p.rebateMinDays)
                    JsonIo.put(q, "rounding", p.rounding.name)
                    JsonIo.put(q, "memberCount", p.memberCount)
                    JsonIo.put(q, "closed", p.closed)
                }
            })
            m["meals"] = JsonIo.arr(l.meals.map { e ->
                JsonIo.obj { q ->
                    JsonIo.put(q, "periodId", e.periodId)
                    JsonIo.put(q, "epochDay", e.epochDay)
                    JsonIo.put(q, "halves", e.halves)
                    JsonIo.put(q, "isLeave", e.isLeave)
                    JsonIo.put(q, "note", e.note)
                }
            })
            m["charges"] = JsonIo.arr(l.charges.map { c ->
                JsonIo.obj { q ->
                    JsonIo.put(q, "periodId", c.periodId)
                    JsonIo.put(q, "id", c.id)
                    JsonIo.put(q, "name", c.name)
                    JsonIo.put(q, "amountMinor", c.amountMinor)
                    JsonIo.put(q, "split", c.split.name)
                    JsonIo.put(q, "note", c.note)
                }
            })
            m["txns"] = JsonIo.arr(l.txns.map { t ->
                JsonIo.obj { q ->
                    JsonIo.put(q, "periodId", t.periodId)
                    JsonIo.put(q, "id", t.id)
                    JsonIo.put(q, "epochDay", t.epochDay)
                    JsonIo.put(q, "kind", t.kind.name)
                    JsonIo.put(q, "amountMinor", t.amountMinor)
                    JsonIo.put(q, "note", t.note)
                }
            })
            m["stated"] = JsonIo.arr(l.stated.map { s ->
                JsonIo.obj { q ->
                    JsonIo.put(q, "periodId", s.periodId)
                    JsonIo.put(q, "amountMinor", s.amountMinor)
                    JsonIo.put(q, "sourceNote", s.sourceNote)
                }
            })
            m["defaults"] = JsonIo.obj { q ->
                JsonIo.put(q, "rateMinor", l.defaults.rateMinor)
                JsonIo.put(q, "basis", l.defaults.basis.name)
                JsonIo.put(q, "rebateMinDays", l.defaults.rebateMinDays)
                JsonIo.put(q, "rounding", l.defaults.rounding.name)
                JsonIo.put(q, "memberCount", l.defaults.memberCount)
                JsonIo.put(q, "languageTag", l.defaults.languageTag)
            }
        }
        return root
    }

    fun fromJson(root: Json): Ledger {
        val periods = root.arr("periods").map { p ->
            Period(
                id = p.str("id"),
                label = p.str("label"),
                startDay = p.long("startDay"),
                endDay = p.long("endDay"),
                rateMinor = p.long("rateMinor"),
                basis = enumOrDefault(p.str("basis"), RateBasis.DAYS_PRESENT),
                rebateMinDays = p.int("rebateMinDays"),
                rounding = enumOrDefault(p.str("rounding"), BillRounding.NEAREST),
                memberCount = p.int("memberCount", 1).coerceAtLeast(1),
                closed = p.bool("closed")
            )
        }
        val known = periods.map { it.id }.toSet()
        fun <T> List<T>.forKnownPeriods(idOf: (T) -> String): List<T> = filter { known.contains(idOf(it)) }

        val meals = root.arr("meals").map { e ->
            MealEntry(e.str("periodId"), e.long("epochDay"), e.int("halves"), e.bool("isLeave"), e.str("note"))
        }.forKnownPeriods { it.periodId }
        val charges = root.arr("charges").map { c ->
            ChargeLine(c.str("periodId"), c.str("id"), c.str("name"), c.long("amountMinor"),
                enumOrDefault(c.str("split"), SplitRule.FULL), c.str("note"))
        }.forKnownPeriods { it.periodId }
        val txns = root.arr("txns").map { t ->
            Txn(t.str("periodId"), t.str("id"), t.long("epochDay"),
                enumOrDefault(t.str("kind"), DepositKind.DEPOSIT), t.long("amountMinor"), t.str("note"))
        }.forKnownPeriods { it.periodId }
        val stated = root.arr("stated").map { s ->
            StatedBill(s.str("periodId"), s.long("amountMinor"), s.str("sourceNote"))
        }.forKnownPeriods { it.periodId }
        val d = root.obj("defaults") ?: Json.Obj()
        val defaults = Defaults(
            rateMinor = d.long("rateMinor", 25_000L),
            basis = enumOrDefault(d.str("basis"), RateBasis.DAYS_PRESENT),
            rebateMinDays = d.int("rebateMinDays"),
            rounding = enumOrDefault(d.str("rounding"), BillRounding.NEAREST),
            memberCount = d.int("memberCount", 5).coerceAtLeast(1),
            languageTag = d.str("languageTag", "en").ifBlank { "en" }
        )
        return Ledger(
            schemaVersion = root.int("schemaVersion", Ledger.SCHEMA_VERSION),
            periods = periods,
            meals = meals,
            charges = charges,
            txns = txns,
            stated = stated,
            defaults = defaults
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
        if (name.isBlank()) fallback else enumValues<T>().firstOrNull { it.name == name } ?: fallback
}
