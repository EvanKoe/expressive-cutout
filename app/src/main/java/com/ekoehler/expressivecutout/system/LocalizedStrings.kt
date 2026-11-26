package com.ekoehler.expressivecutout.system

import android.content.Context
import androidx.annotation.StringRes
import com.ekoehler.expressivecutout.data.LanguagePreferences

/**
 * Resolves strings in the language picked in the app, for long-lived service contexts that may
 * keep the system locale. The localized [Context] is cached and rebuilt only when the stored
 * language tag changes.
 */
internal class LocalizedStrings(private val base: Context) {

    private val languagePreferences = LanguagePreferences(base)

    // Callbacks arrive on several threads; a duplicate rebuild is harmless
    @Volatile
    private var cached: Pair<String, Context>? = null

    /** Returns string [id] in the selected language, formatted with [args] when given. */
    fun get(@StringRes id: Int, vararg args: Any): String {
        val context = localizedContext()
        return if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
    }

    /** The cached localized context, rebuilt when the selected tag no longer matches it. */
    private fun localizedContext(): Context {
        val tag = languagePreferences.tag()
        cached?.takeIf { it.first == tag }?.let { return it.second }
        return AppLocale.localizedContext(base, tag).also { cached = tag to it }
    }
}
