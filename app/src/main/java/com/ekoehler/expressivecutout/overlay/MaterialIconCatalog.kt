package com.ekoehler.expressivecutout.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

/** One selectable Material icon: a stable [key] persisted in settings, and the vector to draw. */
data class MaterialIconOption(val key: String, val icon: ImageVector)

/**
 * The fixed set of built-in Material (rounded) icons a user can pick as an event's icon override.
 * Icons are keyed by a stable string (persisted via [com.ekoehler.expressivecutout.data.IconSource])
 * rather than by their position, so reordering or extending this list never re-maps saved choices.
 * A curated list keeps the picker to icons we know are bundled and avoids runtime reflection over the
 * whole extended icon set.
 */
object MaterialIconCatalog {

    val options: List<MaterialIconOption> = listOf(
        MaterialIconOption("star", Icons.Rounded.Star),
        MaterialIconOption("favorite", Icons.Rounded.Favorite),
        MaterialIconOption("thumb_up", Icons.Rounded.ThumbUp),
        MaterialIconOption("bolt", Icons.Rounded.Bolt),
        MaterialIconOption("battery_charging", Icons.Rounded.BatteryChargingFull),
        MaterialIconOption("battery_full", Icons.Rounded.BatteryFull),
        MaterialIconOption("notifications", Icons.Rounded.Notifications),
        MaterialIconOption("alarm", Icons.Rounded.Alarm),
        MaterialIconOption("access_time", Icons.Rounded.AccessTime),
        MaterialIconOption("timer", Icons.Rounded.Timer),
        MaterialIconOption("event", Icons.Rounded.Event),
        MaterialIconOption("calendar", Icons.Rounded.CalendarMonth),
        MaterialIconOption("lock", Icons.Rounded.Lock),
        MaterialIconOption("lock_open", Icons.Rounded.LockOpen),
        MaterialIconOption("key", Icons.Rounded.Key),
        MaterialIconOption("shield", Icons.Rounded.Shield),
        MaterialIconOption("phone", Icons.Rounded.Phone),
        MaterialIconOption("call", Icons.Rounded.Call),
        MaterialIconOption("message", Icons.Rounded.Message),
        MaterialIconOption("chat", Icons.Rounded.Chat),
        MaterialIconOption("email", Icons.Rounded.Email),
        MaterialIconOption("music_note", Icons.Rounded.MusicNote),
        MaterialIconOption("headphones", Icons.Rounded.Headphones),
        MaterialIconOption("play_arrow", Icons.Rounded.PlayArrow),
        MaterialIconOption("volume_up", Icons.Rounded.VolumeUp),
        MaterialIconOption("mic", Icons.Rounded.Mic),
        MaterialIconOption("camera", Icons.Rounded.CameraAlt),
        MaterialIconOption("photo_camera", Icons.Rounded.PhotoCamera),
        MaterialIconOption("home", Icons.Rounded.Home),
        MaterialIconOption("settings", Icons.Rounded.Settings),
        MaterialIconOption("info", Icons.Rounded.Info),
        MaterialIconOption("warning", Icons.Rounded.Warning),
        MaterialIconOption("check_circle", Icons.Rounded.CheckCircle),
        MaterialIconOption("flag", Icons.Rounded.Flag),
        MaterialIconOption("bookmark", Icons.Rounded.Bookmark),
        MaterialIconOption("lightbulb", Icons.Rounded.Lightbulb),
        MaterialIconOption("wifi", Icons.Rounded.Wifi),
        MaterialIconOption("location", Icons.Rounded.LocationOn),
        MaterialIconOption("cloud", Icons.Rounded.Cloud),
        MaterialIconOption("wb_sunny", Icons.Rounded.WbSunny),
        MaterialIconOption("dark_mode", Icons.Rounded.DarkMode),
        MaterialIconOption("nightlight", Icons.Rounded.Nightlight),
        MaterialIconOption("whatshot", Icons.Rounded.Whatshot),
        MaterialIconOption("fire", Icons.Rounded.LocalFireDepartment),
        MaterialIconOption("rocket", Icons.Rounded.Rocket),
        MaterialIconOption("celebration", Icons.Rounded.Celebration),
        MaterialIconOption("cake", Icons.Rounded.Cake),
        MaterialIconOption("emoji", Icons.Rounded.EmojiEmotions),
        MaterialIconOption("face", Icons.Rounded.Face),
        MaterialIconOption("pets", Icons.Rounded.Pets),
        MaterialIconOption("coffee", Icons.Rounded.Coffee),
        MaterialIconOption("shopping_cart", Icons.Rounded.ShoppingCart),
        MaterialIconOption("work", Icons.Rounded.WorkOutline),
        MaterialIconOption("school", Icons.Rounded.School),
        MaterialIconOption("fitness", Icons.Rounded.FitnessCenter),
        MaterialIconOption("run", Icons.Rounded.DirectionsRun),
        MaterialIconOption("games", Icons.Rounded.SportsEsports),
        MaterialIconOption("car", Icons.Rounded.DirectionsCar),
        MaterialIconOption("flight", Icons.Rounded.FlightTakeoff),
        MaterialIconOption("anchor", Icons.Rounded.Anchor),
        MaterialIconOption("android", Icons.Rounded.Android),
    )

    private val byKey: Map<String, ImageVector> = options.associate { it.key to it.icon }

    /** The vector for a saved [key], or null if the key is unknown (e.g. an icon since removed). */
    fun iconFor(key: String): ImageVector? = byKey[key]
}
