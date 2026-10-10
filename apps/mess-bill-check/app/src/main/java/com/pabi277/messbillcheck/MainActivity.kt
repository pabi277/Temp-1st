package com.pabi277.messbillcheck

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView

/**
 * P2 hello screen: proves the toolchain, the manifest and CI wiring. It renders no product
 * behaviour; the real screens come in P3 against the approved plan.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                text = getString(R.string.skeleton_notice)
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
        )
    }
}
