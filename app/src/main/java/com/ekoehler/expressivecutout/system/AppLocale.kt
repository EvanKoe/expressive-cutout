package com.ekoehler.expressivecutout.system

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.ekoehler.expressivecutout.data.LanguagePreferences
import java.util.Locale

/**
 * Applies the language picked in the Profile tab to the process.
 *
 * Android 13 owns the choice itself: the tag goes to [LocaleManager], which restarts the activity,
 * localises every context in the app — services included — and keeps the system's per-app language
 * screen in step with ours. Below that there is no such API, so each of our own entry points
 * (the activity, the application, the two services) wraps its base context through [wrap] and the
 * caller restarts the activity by hand.
 */
object AppLocale {

    /** Whether the platform applies and restarts on its own, so callers don't have to. */
    val appliedBySystem: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Returns [base] with the stored language applied, for `attachBaseContext`. Pre-Tiramisu only:
     * above it the system has already localised the context we were handed.
     */
    fun wrap(base: Context): Context {
        val tag = LanguagePreferences(base).tag()
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        if (appliedBySystem) return base

        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    /**
     * Makes [tag] the language of the running process: the JVM default for anything formatting
     * dates or numbers, and on Tiramisu and up the platform's own per-app language.
     */
    fun applyToProcess(context: Context, tag: String) {
        Locale.setDefault(Locale.forLanguageTag(tag))
        if (!appliedBySystem) return
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags(tag)
    }
}
