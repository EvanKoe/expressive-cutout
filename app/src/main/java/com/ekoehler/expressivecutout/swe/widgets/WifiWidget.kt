package com.ekoehler.expressivecutout.swe.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material.icons.rounded.SignalWifi0Bar
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.overlay.badgeIconSizeFor
import com.ekoehler.expressivecutout.overlay.resolve
import com.ekoehler.expressivecutout.swe.DEFAULT_BADGE_CORNER_PERCENT
import com.ekoehler.expressivecutout.swe.DEFAULT_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.MAX_BADGE_CORNER_PERCENT
import com.ekoehler.expressivecutout.swe.MAX_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.MIN_BADGE_WIDTH_PERCENT
import com.ekoehler.expressivecutout.swe.SweData
import com.ekoehler.expressivecutout.swe.SweIconBadge
import com.ekoehler.expressivecutout.swe.SweIconBadgeControls
import com.ekoehler.expressivecutout.swe.SweWidget
import com.ekoehler.expressivecutout.swe.SweWidgetType
import com.ekoehler.expressivecutout.swe.SweWidgets
import com.ekoehler.expressivecutout.swe.color
import com.ekoehler.expressivecutout.swe.inkOn
import com.ekoehler.expressivecutout.swe.putColor
import com.ekoehler.expressivecutout.swe.sweIconBadgeWidthDp
import org.json.JSONObject

/** The Wi-Fi signal strength as an icon, on an optional rounded-square background. Hidden while not on Wi-Fi. */
data class WifiWidget(
    override val id: String = SweWidgets.newId(),
    val iconColor: CutoutColor? = null,
    val backgroundColor: CutoutColor? = null,
    val backgroundWidthPercent: Int = DEFAULT_BADGE_WIDTH_PERCENT,
    val backgroundCornerPercent: Int = DEFAULT_BADGE_CORNER_PERCENT,
) : SweWidget {
    override val type: SweWidgetType get() = Companion

    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)
        put("id", id)
        putColor("iconColor", iconColor)
        putColor("backgroundColor", backgroundColor)
        put("backgroundWidthPercent", backgroundWidthPercent)
        put("backgroundCornerPercent", backgroundCornerPercent)
    }

    override fun isVisible(data: SweData): Boolean = data.wifi != null

    override val hasBackground: Boolean get() = backgroundColor != null

    /** Depends on the background settings only, never on the signal level, so the pill does not twitch. */
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
        sweIconBadgeWidthDp(backgroundColor != null, backgroundWidthPercent, heightDp)

    /** The icon for the current signal level, crossfading as it changes; keeps the last level while fading out. */
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        val current = data.wifi?.level
        var lastLevel by remember { mutableIntStateOf(current ?: 0) }
        LaunchedEffect(current) { if (current != null) lastLevel = current }
        val level = (current ?: lastLevel).coerceIn(0, WIFI_LEVEL_ICONS.lastIndex)

        SweIconBadge(backgroundColor, backgroundWidthPercent, backgroundCornerPercent, heightDp) { fill ->
            val tint = iconColor?.resolve() ?: if (fill != null) inkOn(fill) else LocalContentColor.current

            // Signal icon
            AnimatedContent(
                targetState = level,
                transitionSpec = { (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) },
                label = "wifiIcon",
            ) { target ->
                Icon(
                    imageVector = WIFI_LEVEL_ICONS[target],
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(badgeIconSizeFor(heightDp)),
                )
            }
        }
    }

    /** The icon colour and background colour pickers, and the background's width and corner radius when it is shown. */
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) = SweIconBadgeControls(
        iconColor = iconColor,
        backgroundColor = backgroundColor,
        backgroundWidthPercent = backgroundWidthPercent,
        backgroundCornerPercent = backgroundCornerPercent,
        onIconColor = { onChange(copy(iconColor = it)) },
        onBackgroundColor = { onChange(copy(backgroundColor = it)) },
        onWidth = { onChange(copy(backgroundWidthPercent = it)) },
        onCorner = { onChange(copy(backgroundCornerPercent = it)) },
    )

    @Composable
    override fun summary(): String = stringResource(
        if (backgroundColor != null) R.string.widgets_soundMode_background_color else R.string.label_default,
    )

    /** The Wi-Fi kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        /** Stored in user settings and backups; never change it once shipped. */
        private const val TAG = "wifi"

        /** One icon per signal level, indexed by level 0 (none) to 4 (full). */
        private val WIFI_LEVEL_ICONS: List<ImageVector> = listOf(
            Icons.Rounded.SignalWifi0Bar,
            Icons.Rounded.NetworkWifi1Bar,
            Icons.Rounded.NetworkWifi2Bar,
            Icons.Rounded.NetworkWifi3Bar,
            Icons.Rounded.SignalWifi4Bar,
        )

        override val tag: String = TAG
        override val icon: ImageVector get() = Icons.Rounded.Wifi
        override val titleRes: Int = R.string.widgets_wifi_title

        override fun create(): SweWidget = WifiWidget()

        override fun fromJson(id: String, obj: JSONObject): SweWidget = WifiWidget(
            id = id,
            iconColor = obj.color("iconColor"),
            backgroundColor = obj.color("backgroundColor"),
            backgroundWidthPercent = obj.optInt("backgroundWidthPercent", DEFAULT_BADGE_WIDTH_PERCENT)
                .coerceIn(MIN_BADGE_WIDTH_PERCENT, MAX_BADGE_WIDTH_PERCENT),
            backgroundCornerPercent = obj.optInt("backgroundCornerPercent", DEFAULT_BADGE_CORNER_PERCENT)
                .coerceIn(0, MAX_BADGE_CORNER_PERCENT),
        )
    }
}
