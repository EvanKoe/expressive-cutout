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
import com.ekoehler.expressivecutout.R

/** One selectable Material icon: a stable [key] persisted in settings, and the vector to draw. */
data class MaterialIconOption(val key: String, val icon: ImageVector, val labelRes: Int)

/**
 * The fixed set of built-in Material (rounded) icons a user can pick as an event's icon override.
 * Icons are keyed by a stable string (persisted via [com.ekoehler.expressivecutout.data.IconSource])
 * rather than by their position, so reordering or extending this list never re-maps saved choices.
 * A curated list keeps the picker to icons we know are bundled and avoids runtime reflection over the
 * whole extended icon set.
 */
object MaterialIconCatalog {

    val options: List<MaterialIconOption> = listOf(
        MaterialIconOption("star", Icons.Rounded.Star, R.string.icon_label_star),
        MaterialIconOption("favorite", Icons.Rounded.Favorite, R.string.icon_label_favorite),
        MaterialIconOption("thumb_up", Icons.Rounded.ThumbUp, R.string.icon_label_thumb_up),
        MaterialIconOption("bolt", Icons.Rounded.Bolt, R.string.icon_label_bolt),
        MaterialIconOption("battery_charging", Icons.Rounded.BatteryChargingFull, R.string.icon_label_battery_charging),
        MaterialIconOption("battery_full", Icons.Rounded.BatteryFull, R.string.icon_label_battery_full),
        MaterialIconOption("notifications", Icons.Rounded.Notifications, R.string.icon_label_notifications),
        MaterialIconOption("alarm", Icons.Rounded.Alarm, R.string.icon_label_alarm),
        MaterialIconOption("access_time", Icons.Rounded.AccessTime, R.string.icon_label_access_time),
        MaterialIconOption("timer", Icons.Rounded.Timer, R.string.icon_label_timer),
        MaterialIconOption("event", Icons.Rounded.Event, R.string.icon_label_event),
        MaterialIconOption("calendar", Icons.Rounded.CalendarMonth, R.string.icon_label_calendar),
        MaterialIconOption("lock", Icons.Rounded.Lock, R.string.icon_label_lock),
        MaterialIconOption("lock_open", Icons.Rounded.LockOpen, R.string.icon_label_lock_open),
        MaterialIconOption("key", Icons.Rounded.Key, R.string.icon_label_key),
        MaterialIconOption("shield", Icons.Rounded.Shield, R.string.icon_label_shield),
        MaterialIconOption("phone", Icons.Rounded.Phone, R.string.icon_label_phone),
        MaterialIconOption("call", Icons.Rounded.Call, R.string.icon_label_call),
        MaterialIconOption("message", Icons.Rounded.Message, R.string.icon_label_message),
        MaterialIconOption("chat", Icons.Rounded.Chat, R.string.icon_label_chat),
        MaterialIconOption("email", Icons.Rounded.Email, R.string.icon_label_email),
        MaterialIconOption("music_note", Icons.Rounded.MusicNote, R.string.icon_label_music_note),
        MaterialIconOption("headphones", Icons.Rounded.Headphones, R.string.icon_label_headphones),
        MaterialIconOption("play_arrow", Icons.Rounded.PlayArrow, R.string.icon_label_play_arrow),
        MaterialIconOption("volume_up", Icons.Rounded.VolumeUp, R.string.icon_label_volume_up),
        MaterialIconOption("mic", Icons.Rounded.Mic, R.string.icon_label_mic),
        MaterialIconOption("camera", Icons.Rounded.CameraAlt, R.string.icon_label_camera),
        MaterialIconOption("photo_camera", Icons.Rounded.PhotoCamera, R.string.icon_label_photo_camera),
        MaterialIconOption("home", Icons.Rounded.Home, R.string.icon_label_home),
        MaterialIconOption("settings", Icons.Rounded.Settings, R.string.icon_label_settings),
        MaterialIconOption("info", Icons.Rounded.Info, R.string.icon_label_info),
        MaterialIconOption("warning", Icons.Rounded.Warning, R.string.icon_label_warning),
        MaterialIconOption("check_circle", Icons.Rounded.CheckCircle, R.string.icon_label_check_circle),
        MaterialIconOption("flag", Icons.Rounded.Flag, R.string.icon_label_flag),
        MaterialIconOption("bookmark", Icons.Rounded.Bookmark, R.string.icon_label_bookmark),
        MaterialIconOption("lightbulb", Icons.Rounded.Lightbulb, R.string.icon_label_lightbulb),
        MaterialIconOption("wifi", Icons.Rounded.Wifi, R.string.icon_label_wifi),
        MaterialIconOption("location", Icons.Rounded.LocationOn, R.string.icon_label_location),
        MaterialIconOption("cloud", Icons.Rounded.Cloud, R.string.icon_label_cloud),
        MaterialIconOption("wb_sunny", Icons.Rounded.WbSunny, R.string.icon_label_wb_sunny),
        MaterialIconOption("dark_mode", Icons.Rounded.DarkMode, R.string.icon_label_dark_mode),
        MaterialIconOption("nightlight", Icons.Rounded.Nightlight, R.string.icon_label_nightlight),
        MaterialIconOption("whatshot", Icons.Rounded.Whatshot, R.string.icon_label_whatshot),
        MaterialIconOption("fire", Icons.Rounded.LocalFireDepartment, R.string.icon_label_fire),
        MaterialIconOption("rocket", Icons.Rounded.Rocket, R.string.icon_label_rocket),
        MaterialIconOption("celebration", Icons.Rounded.Celebration, R.string.icon_label_celebration),
        MaterialIconOption("cake", Icons.Rounded.Cake, R.string.icon_label_cake),
        MaterialIconOption("emoji", Icons.Rounded.EmojiEmotions, R.string.icon_label_emoji),
        MaterialIconOption("face", Icons.Rounded.Face, R.string.icon_label_face),
        MaterialIconOption("pets", Icons.Rounded.Pets, R.string.icon_label_pets),
        MaterialIconOption("coffee", Icons.Rounded.Coffee, R.string.icon_label_coffee),
        MaterialIconOption("shopping_cart", Icons.Rounded.ShoppingCart, R.string.icon_label_shopping_cart),
        MaterialIconOption("work", Icons.Rounded.WorkOutline, R.string.icon_label_work),
        MaterialIconOption("school", Icons.Rounded.School, R.string.icon_label_school),
        MaterialIconOption("fitness", Icons.Rounded.FitnessCenter, R.string.icon_label_fitness),
        MaterialIconOption("run", Icons.Rounded.DirectionsRun, R.string.icon_label_run),
        MaterialIconOption("games", Icons.Rounded.SportsEsports, R.string.icon_label_games),
        MaterialIconOption("car", Icons.Rounded.DirectionsCar, R.string.icon_label_car),
        MaterialIconOption("flight", Icons.Rounded.FlightTakeoff, R.string.icon_label_flight),
        MaterialIconOption("anchor", Icons.Rounded.Anchor, R.string.icon_label_anchor),
        MaterialIconOption("android", Icons.Rounded.Android, R.string.icon_label_android),
    )

    private val byKey: Map<String, ImageVector> = options.associate { it.key to it.icon }

    /** The vector for a saved [key], or null if the key is unknown (e.g. an icon since removed). */
    fun iconFor(key: String): ImageVector? = byKey[key]
}
