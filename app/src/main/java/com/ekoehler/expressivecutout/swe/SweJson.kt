package com.ekoehler.expressivecutout.swe

import com.ekoehler.expressivecutout.data.CutoutColor
import org.json.JSONObject

/** Writes [color] under [name], or JSON null for the default. */
internal fun JSONObject.putColor(name: String, color: CutoutColor?) {
    put(name, color?.serialize() ?: JSONObject.NULL)
}

/** Reads the colour under [name]; absent, null or unparseable means the default. */
internal fun JSONObject.color(name: String): CutoutColor? =
    if (isNull(name)) null else CutoutColor.deserialize(optString(name))

/** Reads the enum under [name], or null when absent or no longer a valid name. */
internal inline fun <reified E : Enum<E>> JSONObject.enum(name: String): E? =
    optString(name).takeIf { it.isNotEmpty() }?.let { raw -> enumValues<E>().firstOrNull { it.name == raw } }
