package com.ekoehler.expressivecutout.data

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/** Which Material You colour-scheme role a [CutoutColor.Dynamic] / [ColorSpec.Dynamic] follows. */
enum class DynamicRole { PRIMARY, SECONDARY, TERTIARY }

/** Fallback color strategy when AppIcon color is chosen but no app notification is active. */
enum class AppColorFallback {
    /** Material You primary dynamic accent. */
    DYNAMIC_THEME,
    /** OLED Pure Black (#000000). */
    OLED_BLACK,
    /** Adaptive (Media Art for music, Material You for system events). */
    ADAPTIVE;

    companion object {
        fun deserialize(value: String?): AppColorFallback =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: ADAPTIVE
    }
}

/** Direction a [CutoutFill.Gradient] runs across the island. */
enum class GradientDirection { VERTICAL, DIAGONAL, HORIZONTAL }

/**
 * A user-selectable colour for the island. Either a fixed ARGB value, [Dynamic] (Material You),
 * or [AppIcon] which extracts the primary color of the active app's default launcher icon.
 */
@Immutable
sealed interface CutoutColor {
    /**
     * A colour that follows the wallpaper: [role] is resolved against the live Material You scheme
     * at render time.
     */
    data class Dynamic(val role: DynamicRole = DynamicRole.PRIMARY) : CutoutColor

    /**
     * A fixed colour the user picked, held as a `Long` rather than a Compose `Color` so it fits one
     * DataStore key.
     */
    data class Solid(val argb: Long) : CutoutColor

    /**
     * A colour dynamically extracted from the active app's launcher icon, with a configurable [fallback].
     */
    data class AppIcon(val fallback: AppColorFallback = AppColorFallback.ADAPTIVE) : CutoutColor

    /**
     * Encodes to the single string held in preferences: `dynamic:ROLE`, `app_icon:FALLBACK`, or the bare ARGB number.
     * Read back by [deserialize].
     */
    fun serialize(): String = when (this) {
        is Dynamic -> "$DYNAMIC:${role.name}"
        is Solid -> argb.toString()
        is AppIcon -> "$APP_ICON:${fallback.name}"
    }

    companion object {
        private const val DYNAMIC = "dynamic"
        private const val APP_ICON = "app_icon"

        fun deserialize(value: String?): CutoutColor? = when {
            value == null -> null
            // Legacy bare "dynamic" (before roles) migrates to the primary accent.
            value == DYNAMIC -> Dynamic(DynamicRole.PRIMARY)
            value.startsWith("$DYNAMIC:") -> {
                val role = runCatching { DynamicRole.valueOf(value.substringAfter(':')) }
                    .getOrDefault(DynamicRole.PRIMARY)
                Dynamic(role)
            }
            value == APP_ICON -> AppIcon(AppColorFallback.ADAPTIVE)
            value.startsWith("$APP_ICON:") -> {
                val fallback = AppColorFallback.deserialize(value.substringAfter(':'))
                AppIcon(fallback)
            }
            else -> value.toLongOrNull()?.let(::Solid)
        }
    }
}

/**
 * A single resolvable colour used by a [CutoutFill] (as a solid fill or a gradient stop): either a
 * [Fixed] ARGB value, a Material You [Dynamic] role resolved at render time, or an [AppIcon] extracted color.
 * Both carry an opacity — [Fixed] in its ARGB alpha byte, [Dynamic]/[AppIcon] in [alpha] — so any colour can be
 * made translucent. Serialized without the `:` used by [Fixed] so gradients can delimit on `|`.
 */
@Immutable
sealed interface ColorSpec {
    /** A fixed ARGB colour, its opacity carried in the alpha byte. */
    data class Fixed(val argb: Long) : ColorSpec

    /**
     * A Material You role resolved at render time, with [alpha] holding the opacity separately
     * since the role supplies no alpha of its own.
     */
    data class Dynamic(val role: DynamicRole, val alpha: Float = 1f) : ColorSpec

    /** An app launcher icon extracted color, with [alpha] holding the opacity separately. */
    data class AppIcon(val fallback: AppColorFallback = AppColorFallback.ADAPTIVE, val alpha: Float = 1f) : ColorSpec

    /** 0f..1f opacity of this colour. */
    val opacity: Float
        get() = when (this) {
            is Fixed -> ((argb ushr 24) and 0xFF) / 255f
            is Dynamic -> alpha
            is AppIcon -> alpha
        }

    /** A copy of this colour at the given [opacity] (0f..1f). */
    fun withOpacity(opacity: Float): ColorSpec {
        val a = opacity.coerceIn(0f, 1f)
        return when (this) {
            is Fixed -> {
                val alphaByte = (a * 255f).roundToInt().toLong()
                Fixed((argb and 0x00FFFFFFL) or (alphaByte shl 24))
            }
            is Dynamic -> copy(alpha = a)
            is AppIcon -> copy(alpha = a)
        }
    }

    /**
     * Encodes to `dynamic:ROLE:ALPHA`, `app_icon:FALLBACK:ALPHA`, or the bare ARGB number. Never contains a `|`, which is what
     * lets [CutoutFill.Gradient] delimit its stops on one.
     */
    fun serialize(): String = when (this) {
        is Fixed -> argb.toString()
        is Dynamic -> "$DYNAMIC:${role.name}:$alpha"
        is AppIcon -> "$APP_ICON:${fallback.name}:$alpha"
    }

    companion object {
        private const val DYNAMIC = "dynamic"
        private const val APP_ICON = "app_icon"

        fun deserialize(value: String?): ColorSpec? = when {
            value == null -> null
            // Legacy bare "dynamic" (from the old CutoutColor default).
            value == DYNAMIC -> Dynamic(DynamicRole.PRIMARY)
            value.startsWith("$DYNAMIC:") -> {
                val parts = value.split(':')
                val role = runCatching { DynamicRole.valueOf(parts[1]) }.getOrDefault(DynamicRole.PRIMARY)
                val alpha = parts.getOrNull(2)?.toFloatOrNull() ?: 1f
                Dynamic(role, alpha.coerceIn(0f, 1f))
            }
            value == APP_ICON -> AppIcon(AppColorFallback.ADAPTIVE)
            value.startsWith("$APP_ICON:") -> {
                val parts = value.split(':')
                val fallback = AppColorFallback.deserialize(parts.getOrNull(1))
                val alpha = parts.getOrNull(2)?.toFloatOrNull() ?: 1f
                AppIcon(fallback, alpha.coerceIn(0f, 1f))
            }
            // A bare ARGB number.
            else -> value.toLongOrNull()?.let(::Fixed)
        }
    }
}

/**
 * The fill painted behind the island. Richer than [CutoutColor] (it also allows a two-colour
 * [Gradient]) and used only for the background, which has an independent value for the collapsed
 * ([AppearanceSettings.backgroundNormal]) and expanded ([AppearanceSettings.backgroundExpanded])
 * states. Serialized to a single string so it fits one preference key.
 *
 * [deserialize] also accepts the legacy [CutoutColor] encoding (`"dynamic"` or a bare ARGB number)
 * so an existing single background colour migrates into both states with no data loss.
 */
@Immutable
sealed interface CutoutFill {
    /** A single flat colour. */
    data class Solid(val color: ColorSpec) : CutoutFill
    /** A two-stop linear gradient running along [direction]. */
    data class Gradient(
        val start: ColorSpec,
        val end: ColorSpec,
        val direction: GradientDirection,
        val opacity: Float = 1f,
    ) : CutoutFill

    /**
     * Encodes to the stop-delimited `gradient|start|end|DIRECTION|opacity` or delegates to the single
     * colour for [Solid]. Read back by [deserialize].
     */
    fun serialize(): String = when (this) {
        is Solid -> color.serialize()
        is Gradient -> listOf(GRADIENT, start.serialize(), end.serialize(), direction.name, opacity.toString()).joinToString("|")
    }

    companion object {
        private const val GRADIENT = "gradient"
        private const val APP_FADE = "app_fade"

        fun deserialize(value: String?): CutoutFill? = when {
            value == null -> null
            value.startsWith("$GRADIENT|") -> {
                val parts = value.split('|')
                val start = ColorSpec.deserialize(parts.getOrNull(1))
                val end = ColorSpec.deserialize(parts.getOrNull(2))
                val direction = runCatching { GradientDirection.valueOf(parts[3]) }
                    .getOrDefault(GradientDirection.VERTICAL)
                val opacity = parts.getOrNull(4)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
                if (start != null && end != null) Gradient(start, end, direction, opacity) else null
            }
            value.startsWith("$APP_FADE|") -> {
                // Migrate legacy app_fade serialization into a horizontal gradient
                val parts = value.split('|')
                val appColor = (ColorSpec.deserialize(parts.getOrNull(1)) as? ColorSpec.AppIcon) ?: ColorSpec.AppIcon()
                val baseColor = ColorSpec.deserialize(parts.getOrNull(2)) ?: ColorSpec.Fixed(0xFF000000L)
                val opacity = parts.getOrNull(6)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
                Gradient(start = appColor, end = baseColor, direction = GradientDirection.HORIZONTAL, opacity = opacity)
            }
            // Anything else is a single colour (incl. the legacy "dynamic" / bare-ARGB encodings).
            else -> ColorSpec.deserialize(value)?.let(::Solid)
        }
    }
}
