cat > ui/LedgerHost.kt <<'EOF'
package com.pabi277.messbillcheck.ui

import android.content.Context
import com.pabi277.messbillcheck.core.Ledger
import com.pabi277.messbillcheck.core.LedgerStore
import java.io.File

/** One store per process. Files live in app-private storage, so no permission is needed. */
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
    fun activePeriod(l: Ledger) = l.periods.filterNot { it.closed }.maxByOrNull { it.endDay }
        ?: l.periods.maxByOrNull { it.endDay }
}
