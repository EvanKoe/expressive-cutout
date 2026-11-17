package com.ekoehler.expressivecutout.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.ekoehler.expressivecutout.system.AppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Persists the language the app is shown in, as one of the tags in [AppLanguages].
 *
 * This is the one store not backed by DataStore: [AppLocale] needs the tag synchronously from
 * `attachBaseContext`, before any coroutine of ours can run, which SharedPreferences can answer and
 * DataStore can't. On Android 13+ the platform is the source of truth — a change made from the
 * system's per-app language screen wins over what we wrote — and the file below only mirrors it so
 * the picker and the settings export have something to read. The file is opened on whichever
 * context is handed in rather than the application one, which doesn't exist yet at the point the
 * Application attaches its own base context.
 */
class LanguagePreferences(private val context: Context) : JsonSerializable {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The selected tag, shared by every instance in the process so the picker follows a change. */
    val language: StateFlow<String> = state.asStateFlow()

    init {
        state.value = read()
    }

    /** The selected tag, for callers that can't wait on [language]. */
    fun tag(): String = state.value

    /** Stores [tag] and applies it to the process; unknown tags fall back to the base language. */
    fun setLanguage(tag: String) {
        val resolved = AppLanguages.normalise(context, tag)
        prefs.edit { putString(TAG, resolved) }
        state.value = resolved
        AppLocale.applyToProcess(context, resolved)
    }

    /**
     * Re-applies the stored tag at process start, so the first launch is in the base language even
     * on a device whose own locale we happen to ship a translation for.
     */
    fun applyStored() = AppLocale.applyToProcess(context, tag())

    /** The platform's per-app language on Tiramisu and up, else what we last wrote. */
    private fun read(): String {
        val tag = systemTag() ?: prefs.getString(TAG, null) ?: AppLanguages.DEFAULT_TAG
        return AppLanguages.normalise(context, tag)
    }

    /** The tag Android 13+ holds for us, or null below it and when the user never chose one. */
    private fun systemTag(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        return locales?.takeUnless { it.isEmpty }?.get(0)?.toLanguageTag()
    }

    /** Exports settings to JSON { language: string } */
    override suspend fun toJson(): String = JSONObject().apply {
        put("language", tag())
    }.toString()

    /** Applies { language: string } exported by [toJson]; an unshipped language is ignored. */
    override suspend fun fromJson(json: String) {
        val tag = JSONObject(json).optString("language").takeIf { it.isNotEmpty() } ?: return
        if (AppLanguages.supported(context).none { it.tag == tag }) return
        setLanguage(tag)
    }

    private companion object {
        const val FILE = "language_prefs"
        const val TAG = "app_language"

        val state = MutableStateFlow(AppLanguages.DEFAULT_TAG)
    }
}
