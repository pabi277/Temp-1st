package com.pabi277.messbillcheck.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoreTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val start = Cal.epochDay(2026, 10, 1)
    private val end = Cal.epochDay(2026, 10, 31)
    private val id get() = "p$start"

    private fun seed(dir: File): Ledger {
        val store = LedgerStore(dir)
        var l = Ledger(schemaVersion = Ledger.SCHEMA_VERSION)
        l = LedgerEdits.newPeriod(l, "October 2026", start, end, 25_000, RateBasis.DAYS_PRESENT, 0, BillRounding.NEAREST, 5)
        l = LedgerEdits.setMeals(l, id, start, 2, false)
        l = LedgerEdits.setMeals(l, id, start + 1, 1, false)
        l = LedgerEdits.addCharge(l, id, "Maid", 100_000, SplitRule.PER_MEMBER)
        store.save(l)
        return l
    }

    @Test
    fun `load on a fresh install yields an empty ledger instead of failing`() {
        val l = LedgerStore(folder.root).load()
        assertEquals(Ledger.SCHEMA_VERSION, l.schemaVersion)
        assertTrue(l.periods.isEmpty())
    }

    @Test
    fun `an empty file is treated as a fresh install`() {
        val dir = folder.newFolder("blank")
        File(dir, LedgerStore.LEDGER_FILE).writeText("   ")
        assertTrue(LedgerStore(dir).load().periods.isEmpty())
    }

    @Test
    fun `save then load preserves every row and the computed bill`() {
        val dir = folder.newFolder("roundtrip")
        val before = seed(dir)
        val after = LedgerStore(dir).load()
        assertEquals(before.periods, after.periods)
        assertEquals(before.meals.size, after.meals.size)
        assertEquals(Billing.compute(before, id).computedMinor, Billing.compute(after, id).computedMinor)
        // 2 days present (1 full + 1 half-meal day) at DAYS_PRESENT = 2 x 250.00 = 500.00,
        // plus the maid charge split 5 ways = 200.00.
        assertEquals(70_000L, Billing.compute(after, id).computedMinor)
    }

    @Test
    fun `the previous ledger file is kept so a bad write is recoverable`() {
        val dir = folder.newFolder("prev")
        seed(dir)
        val store = LedgerStore(dir)
        store.mutate { LedgerEdits.addTxn(it, id, start, DepositKind.DEPOSIT, 10_000) }
        assertTrue(File(dir, "${LedgerStore.LEDGER_FILE}.prev").isFile)
        assertFalse(File(dir, "${LedgerStore.LEDGER_FILE}.tmp").exists())
    }

    @Test
    fun `a corrupt ledger surfaces as a storage error, not a silent wipe`() {
        val dir = folder.newFolder("corrupt")
        seed(dir)
        File(dir, LedgerStore.LEDGER_FILE).writeText("{\"schemaVersion\":1,\"periods\":[")
        assertThrows(JsonException::class.java) { LedgerStore(dir).load() }
    }

    @Test
    fun `close writes an immutable snapshot and refuses to overwrite it`() {
        val dir = folder.newFolder("snap")
        val store = LedgerStore(dir)
        val l = store.load()
        val payload = LedgerJson.write(l)
        store.writeSnapshot(id, payload)
        assertThrows(StorageException::class.java) { store.writeSnapshot(id, payload) }
        assertEquals(payload, store.readSnapshot(id))
    }

    @Test
    fun `backup restore keeps rows whose period was deleted out of the ledger`() {
        val dir = folder.newFolder("backup")
        val before = seed(dir)
        val store = LedgerStore(dir)
        val text = store.exportBackup(before)
        val restored = store.importBackup(text)
        assertEquals(Billing.compute(before, id).computedMinor, Billing.compute(restored, id).computedMinor)

        val orphan = text.replace("\"periods\":[", "\"periods\":[").replace(id, "pGONE")
        val cleaned = LedgerJson.read(orphan)
        assertTrue(cleaned.meals.isEmpty())
        assertTrue(cleaned.charges.isEmpty())
    }

    @Test
    fun `a backup from a newer app version is refused instead of truncated`() {
        val dir = folder.newFolder("newer")
        val store = LedgerStore(dir)
        val future = LedgerJson.write(Ledger(Ledger.SCHEMA_VERSION)).replace(
            "\"schemaVersion\":${Ledger.SCHEMA_VERSION}",
            "\"schemaVersion\":${Ledger.SCHEMA_VERSION + 1}"
        )
        val e = assertThrows(StorageException::class.java) { store.importBackup(future) }
        assertTrue(e.message!!, e.message!!.contains("newer app"))
    }

    @Test
    fun `mutate is serialized and reads back what it wrote`() {
        val dir = folder.newFolder("mutate")
        seed(dir)
        val store = LedgerStore(dir)
        var failure: Throwable? = null
        val threads = (1..4).map { n ->
            Thread {
                try {
                    store.mutate { LedgerEdits.addTxn(it, id, start + n, DepositKind.PAYMENT, 1_000L + n) }
                } catch (e: Throwable) {
                    failure = e
                }
            }.apply { start() }
        }
        threads.forEach { it.join() }
        // seed() records no transactions, so four serialized mutate calls mean exactly four.
        val loaded = store.load()
        assertEquals(4, loaded.txnsOf(id).size)
        assertEquals(4, loaded.txns.size)
        assertEquals(setOf(1_001L, 1_002L, 1_003L, 1_004L), loaded.txns.map { it.amountMinor }.toSet())
        assertTrue("worker failed: ${failure}", failure == null)
    }
}
