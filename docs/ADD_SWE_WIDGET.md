# Adding a SWE widget

SWE widgets are the items on the resting pill ("Shows when empty"), to the left or right of the
camera. Each widget is **one file** in `swe/widgets/` plus **one line** in the registry.

## 1. Create the file

`app/src/main/java/com/ekoehler/expressivecutout/swe/widgets/MyWidget.kt`

Fastest start: copy [`ExampleWidget.kt`](../app/src/main/java/com/ekoehler/expressivecutout/swe/widgets/ExampleWidget.kt)
(a compiling, unregistered template with every member declared), rename the class and file,
change `TAG` and `titleRes`, then fill in the drawing. The annotated version below explains each part.

> The `//` comments below are for this guide only. In real code, follow [AGENTS.md](../AGENTS.md):
> KDoc on declarations, no comments that say *what* a line does.

```kotlin
/** A short label in the chosen colour. */
data class MyWidget(
    // Unique per instance, so the same widget can be added twice with different settings
    override val id: String = SweWidgets.newId(),

    // Each setting is a `val` with a default; null colour means "follow the pill's ink"
    val color: CutoutColor? = null,
) : SweWidget {
    // Points at the companion below, which holds the tag, icon and title for this kind
    override val type: SweWidgetType get() = Companion

    // Saved to DataStore and to settings exports; must be readable by fromJson below
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", TAG)                // Tells SweWidgets which kind to rebuild on load
        put("id", id)
        putColor("color", color)        // Helper from swe/SweJson.kt; writes JSON null for the default
    }

    // Return false when there is nothing to show (e.g. a count of 0): the widget then takes no width
    override fun isVisible(data: SweData): Boolean = true

    // Called outside Compose to size the overlay window: measure the WIDEST text this widget can show
    override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
        measureDp(LABEL, heightDp * TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)

    // What the pill draws. Live values (counts, battery, time) come from `data`
    @Composable
    override fun Content(data: SweData, heightDp: Int) {
        Text(
            text = LABEL,
            // The user's colour, or the pill's default ink when none is picked
            color = color?.resolve() ?: LocalContentColor.current,
            // Sized from the pill height so it scales with the user's pill size
            fontSize = with(LocalDensity.current) { (heightDp * TEXT_FRACTION).dp.toSp() },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }

    // The settings shown in the widget's bottom sheet (the side picker is added for you)
    @Composable
    override fun Controls(onChange: (SweWidget) -> Unit) {
        ColorPickerCard(
            label = stringResource(R.string.my_widget_color),
            selected = color,
            // Send back an edited copy; it is saved and the pill redraws with it
            onSelect = { onChange(copy(color = it)) },
            defaultLabel = stringResource(R.string.label_default),
            defaultColor = MaterialTheme.colorScheme.onSurface,
            // Rounded corners top and bottom, since this card is alone in its group
            shape = groupedShape(isFirst = true, isLast = true),
        )
    }

    // One short line under the widget's name in the editor list
    @Composable
    override fun summary(): String = LABEL

    /** The "my widget" kind: its registry entry, defaults and JSON reader. */
    companion object : SweWidgetType {
        // Saved in user settings and backups: never rename it once shipped
        private const val TAG = "my_widget"
        private const val LABEL = "Hi"
        // Text height as a share of the pill height
        private const val TEXT_FRACTION = 0.4f

        override val tag: String = TAG
        // Icon and title shown in the "Add widget" sheet and the editor list
        override val icon: ImageVector = Icons.Rounded.Star
        override val titleRes: Int = R.string.my_widget_title

        // Called when the user picks this widget in the "Add widget" sheet
        override fun create(): SweWidget = MyWidget()

        // Rebuilds the widget from toJson's output; every missing field falls back to its default
        override fun fromJson(id: String, obj: JSONObject): SweWidget =
            MyWidget(id = id, color = obj.color("color"))
    }
}
```

[`ClockWidget`](../app/src/main/java/com/ekoehler/expressivecutout/swe/widgets/ClockWidget.kt),
[`UnreadWidget`](../app/src/main/java/com/ekoehler/expressivecutout/swe/widgets/UnreadWidget.kt) and
[`BatteryWidget`](../app/src/main/java/com/ekoehler/expressivecutout/swe/widgets/BatteryWidget.kt)
are full examples.

## Live data

`MyWidget` above is static. A widget that shows something that changes falls in one of three cases.

### A. The value is already in `SweData`

[`SweData`](../app/src/main/java/com/ekoehler/expressivecutout/swe/SweData.kt) holds the live
values the pill already tracks: `counts` (unread notifications) and `battery` (null until the first
reading). Read them from the `data` parameter; the pill redraws by itself when they change.

```kotlin
// Hidden (and zero width) until there is a reading
override fun isVisible(data: SweData): Boolean = data.battery != null

// Sized for "100", never for the current level, so the pill doesn't resize as the level drops
override fun widthDp(heightDp: Int, density: Float, data: SweData): Float =
    measureDp("100", heightDp * TEXT_FRACTION, density, Typeface.DEFAULT_BOLD)

@Composable
override fun Content(data: SweData, heightDp: Int) {
    // Keep the last reading: when isVisible turns false the widget fades out, and it should
    // fade out showing its old value rather than nothing
    var last by remember { mutableStateOf(data.battery) }
    if (data.battery != null) last = data.battery
    val battery = last ?: return

    Text(text = "${battery.level}", /* ... */)
}
```

See `BatteryWidget` and `UnreadWidget` for the real thing.

### B. The value can be produced inside `Content`

Anything a composable can compute or poll on its own (the time, a date, a countdown) needs no
plumbing: produce it in `Content` with `remember` / `produceState`. `ClockWidget` does this with
`rememberMinuteClock()` (in `overlay/EmptyPillInfo.kt`), which ticks once a minute.

```kotlin
@Composable
override fun Content(data: SweData, heightDp: Int) {
    val time = rememberMinuteClock()
    Text(text = time, /* ... */)
}
```

`widthDp` still sizes for the worst case (`ClockWidget` measures `"88:88"`), since it can't see
the value `Content` produces.

### C. A new system source (weather, steps, …)

This is the one case that touches shared code, once per source, not per widget:

1. **Publish it.** Expose the value as a `StateFlow`, like
   [`BatteryBus`](../app/src/main/java/com/ekoehler/expressivecutout/events/BatteryBus.kt) or
   `CutoutNotificationListenerService.unreadCounts`. Follow the privacy rules in
   [AGENTS.md](../AGENTS.md): no screen content, no notification bodies.
2. **Add a field to `SweData`**, nullable if it can be missing.
3. **Feed it everywhere `SweData` is built:**
   - `overlay/IslandOverlayController.kt`: collect it next to `unreadCounts` / `battery` in the
     composition, pass it to `DynamicIsland`, and add it to `emptyPillCutout()`.
   - `overlay/DynamicIsland.kt`: a new parameter, then both `SweData(...)` calls (the cutout size
     and `EmptyPillInfoContent`).
   - `ui/screen/SettingScreens/SweWidgetEditor.kt`: a fixed sample value in `PreviewData`, so the
     editor preview shows something.
4. **Resize when it matters.** If the new value can change a widget's visibility or width, add it
   to the `combine(...)` in `IslandOverlayController.observeEmptyPillInfo()`, which re-sizes the
   overlay window. Map it to only what affects size (like `battery != null`), so the window isn't
   re-sized on every tick. Add the same key to the `remember(...)` around the cutout in
   `DynamicIsland.kt`.

## 2. Register it

In [`swe/SweWidgets.kt`](../app/src/main/java/com/ekoehler/expressivecutout/swe/SweWidgets.kt):

```kotlin
val all: List<SweWidgetType> = listOf(ClockWidget, UnreadWidget, BatteryWidget, MyWidget)
```

The order here is the order in the "Add widget" sheet.

## 3. Add its strings

Add the title (and any control labels) to `values/strings.xml`, `values-fr-rFR/strings.xml` and
`values-tr/strings.xml`. See [TRANSLATE.md](TRANSLATE.md).

## Rules

- **Immutable.** `val` fields only. Settings change through `onChange(copy(...))`, never a setter.
- **`widthDp` is an estimate.** It runs *before* composition to size the overlay window, so measure
  the widest possible text with `measureDp` (a `Paint`), never with Compose. Too small clips the
  widget. Don't depend on values that tick (the minute, the battery level), or the pill twitches.
- **`isVisible` false = no width.** Use it to hide a widget with nothing to show (like a count of 0).
- **Fading out.** If `Content` shows a live value, `remember` the last one so the fade-out
  doesn't flash an empty or zero state. See [Live data](#a-the-value-is-already-in-swedata).
- **JSON tag is forever.** `tag` is stored in user settings and backups; never rename it. Give each
  field a default in `fromJson` and clamp ranged values, so old or hand-edited backups still load.
- **Live data** comes from `SweData` or is produced in `Content`; see [Live data](#live-data).
- **Colours.** The pill is the one place hardcoded colours are allowed; default to
  `LocalContentColor.current` and let the user override with a `CutoutColor`.
- Follow [AGENTS.md](../AGENTS.md) for KDoc and comments.

## Check it

```bash
./gradlew assembleDebug
```

Then on a device: add the widget on each side, change every control, watch the pill for clipping,
and export → import settings to check it round-trips.
