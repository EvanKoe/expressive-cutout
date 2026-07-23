# Expressive Cutout

A small **dynamic island** that lives on the camera cutout and briefly lights up for
notifications and system events.

- **Notifications** → shows the posting app's icon (icon-only, so it never crowds the cutout).
- **Tap to expand** → tapping the island grows it into a larger card showing the label and
  detail (e.g. app name + notification title); tapping again collapses it.
- **System events** → shows a user-selectable icon (with a sensible default):
  charging started/stopped, low-battery warning, Wi‑Fi connect/disconnect,
  headphones connect/disconnect, USB device mount/unmount, and device unlock.

## How it works

| Concern | Where | Notes |
| --- | --- | --- |
| Draw the island | `service/CutoutAccessibilityService` + `overlay/` | Uses `TYPE_ACCESSIBILITY_OVERLAY` — **no** `SYSTEM_ALERT_WINDOW`. Reads no screen content. The window wraps the pill so it only intercepts touches over the island (for tap-to-expand); the rest of the screen stays interactive. |
| Capture notifications | `service/CutoutNotificationListenerService` | Keeps only package + title. |
| Detect system events | `events/SystemEventMonitor` | Broadcast receivers, an `AudioDeviceCallback`, and a Wi‑Fi `NetworkCallback`. |
| Decouple producers/consumer | `core/IslandEventBus` | A buffered `SharedFlow`. |
| Persist custom icons / layout / theme | `data/*Preferences` | DataStore; per-event icon URIs, island size & position, and theme choice. |
| App UI | `ui/` | Compose + Material 3, single activity. |

The in-app UI is a single screen with a floating, animated **expressive bottom nav bar**
(`ui/components/ExpressiveNavBar`) and three tabs:

- **Settings** — a list that opens two focused sub-screens:
  - **Size & position** — island width/height & position with stepper sliders and a live
    preview (switchable light/dark background) that draws the device's real camera cutout
    to scale. Only this screen runs the live preview / pins the on-device overlay.
  - **Event icons** — a per-event icon override that can be an **image file or an installed
    app's icon**.
- **Permissions** — notification access, overlay (accessibility) and battery-optimisation,
  each deep-linking to the correct system screen with status that refreshes on resume,
  plus a **Send a test notification** button.
- **Profile** — app version and a theme selector (System / Light / Dark), persisted and
  applied app-wide.

## Requirements

- Android Studio (Ladybug or newer) with the Android SDK for `compileSdk 35`.
- `minSdk 29`, `targetSdk 35`.

## Build

```bash
./gradlew :app:assembleDebug
```

> The Gradle wrapper JAR is not checked in. If `gradlew` is missing its jar, run
> `gradle wrapper` once (or open the project in Android Studio, which regenerates it).

## Coding style

See [`docs/CODING_STYLE.md`](docs/CODING_STYLE.md) for the conventions used across the
project.
