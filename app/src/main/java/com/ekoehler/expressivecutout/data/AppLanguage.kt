package com.ekoehler.expressivecutout.data

import android.content.Context
import com.ekoehler.expressivecutout.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/** One language the app ships strings for: its BCP-47 [tag] and the [locale] it resolves to. */
data class AppLanguage(val tag: String, val locale: Locale) {
    /** The language's own name for itself — "English", "Türkçe" — as shown in the picker. */
    val displayName: String
        get() = locale.getDisplayLanguage(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/**
 * The languages the app can be shown in, read from `res/xml/locales_config.xml`. That file is also
 * what the manifest points `android:localeConfig` at, so the in-app picker, the system's per-app
 * language screen and the `values-<tag>` folders can't drift apart: adding a translation is one new
 * folder plus one line in the config.
 */
object AppLanguages {
    /** The base `res/values` language, which the app falls back to until another one is picked. */
    const val DEFAULT_TAG = "en"

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    @Volatile
    private var cached: List<AppLanguage>? = null

    /**
     * The picker's entries: [DEFAULT_TAG] first, the rest by their own name. Parsed once per
     * process, since the list only changes with a new build.
     */
    fun supported(context: Context): List<AppLanguage> = cached ?: parse(context).also { cached = it }

    /**
     * Narrows [tag] to one of the [supported] tags, matching on language alone so a regional tag
     * ("en-GB", handed to us by the system's own language screen) still lands on its translation.
     * Anything we don't ship strings for falls back to [DEFAULT_TAG].
     */
    fun normalise(context: Context, tag: String): String {
        val language = Locale.forLanguageTag(tag).language.ifEmpty { return DEFAULT_TAG }
        return supported(context).firstOrNull { it.locale.language == language }?.tag ?: DEFAULT_TAG
    }

    /** Reads the `<locale>` entries out of the locale config, in file order. */
    private fun parse(context: Context): List<AppLanguage> {
        val tags = mutableListOf<String>()
        val parser = context.resources.getXml(R.xml.locales_config)
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.name != "locale") continue
                parser.getAttributeValue(ANDROID_NS, "name")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(tags::add)
            }
        } finally {
            parser.close()
        }

        val languages = tags.distinct().map { AppLanguage(it, Locale.forLanguageTag(it)) }
        val default = languages.firstOrNull { it.tag == DEFAULT_TAG }
            ?: AppLanguage(DEFAULT_TAG, Locale.forLanguageTag(DEFAULT_TAG))
        return listOf(default) + languages.filter { it != default }.sortedBy { it.displayName }
    }
}
