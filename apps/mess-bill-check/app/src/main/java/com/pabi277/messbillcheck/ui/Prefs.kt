package com.pabi277.messbillcheck.ui

import android.content.Context

/**
 * Only the interface language lives here. Everything about money, meals and months is in the
 * ledger file, so a settings reset can never destroy the user's records.
 */
object Prefs {

    private const val FILE = "prefs"
    private const val KEY_LANGUAGE = "language"

    fun language(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            ?.takeIf { it in AppLocale.supported } ?: "en"

    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, if (tag in AppLocale.supported) tag else "en")
            .apply()
    }
}
