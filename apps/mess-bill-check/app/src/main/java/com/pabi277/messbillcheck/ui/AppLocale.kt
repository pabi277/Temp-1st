package com.pabi277.messbillcheck.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language switch (EN / हिन्दी / বাংলা) with no settings-app hop and no restart prompt.
 * The tag comes from ui/Prefs, not from the ledger, so a corrupt data file can never make the
 * interface unreadable.
 */
object AppLocale {

    val supported = listOf("en", "hi", "bn")

    fun wrap(base: Context, tag: String): Context {
        val locale = if (tag in supported) Locale(tag) else Locale.getDefault()
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    fun currentTag(context: Context): String {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) "en" else locales[0].language
    }
}
