package com.pabi277.messbillcheck.ui

import android.content.Context
import com.pabi277.messbillcheck.core.Ledger
import com.pabi277.messbillcheck.core.LedgerStore
import com.pabi277.messbillcheck.core.Period
import java.io.File

/** One store per process; app-private files, so no runtime permission is ever requested. */
object LedgerHost {

    @Volatile
    private var store: LedgerStore? = null

    fun store(context: Context): LedgerStore {
        val dir = context.applicationContext.filesDir
        return store ?: synchronized(this) {
            store ?: LedgerStore(File(dir, "data")).also { store = it }
        }
    }

    fun resetForTests() {
        synchronized(this) { store = null }
    }

    /** The period the user is working in: the newest open one, else the newest of any. */
    fun activePeriod(l: Ledger): Period? =
        l.periods.filterNot { it.closed }.maxByOrNull { it.endDay } ?: l.periods.maxByOrNull { it.endDay }
}
