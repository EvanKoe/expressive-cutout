# Coding style — Expressive Cutout

This document describes the conventions used throughout the project. They are intended to
be boring, predictable, and enforceable by tooling. When in doubt, prefer the choice that
keeps a file readable top-to-bottom without cross-referencing.

## 1. Language & tooling

- **Kotlin**, targeting JVM 17, with the official Kotlin code style
  (`kotlin.code.style=official` in `gradle.properties`).
- **Jetpack Compose** for all in-app UI and for the overlay itself, with the Compose
  compiler applied via the `org.jetbrains.kotlin.plugin.compose` Gradle plugin.
- **Material 3** (`androidx.compose.material3`) is the only design system; colours come
  from Material You dynamic colour on Android 12+ and a fixed brand scheme below that.
- Dependencies are declared once in the **version catalog** (`gradle/libs.versions.toml`)
  and referenced as `libs.*`. No hardcoded versions in `build.gradle.kts`.

Formatting is expected to match the default IntelliJ/Android Studio Kotlin formatter
(4-space indent, trailing commas on multi-line argument lists, imports not wildcarded).

## 2. Project layout

Source is grouped by **feature/responsibility**, not by Android component type:

```
com.ekoehler.expressivecutout
├── core/         Pure model + the event bus (no Android UI, no framework services)
├── data/         Persistence (DataStore preferences)
├── events/       System-event detection (broadcasts, callbacks)
├── overlay/      The island: rendering, lifecycle host, icon resolution
├── notifications/ Self-posted test notification
├── permissions/  Permission checks and settings-intent helpers
├── service/      The two OS-instantiated services (thin entry points only)
└── ui/           Activity, ViewModel, MainScreen, theme
    ├── components/ Reusable widgets (e.g. the expressive nav bar)
    └── screen/     One file per tab (SettingsTab, PermissionsTab, ProfileTab)
```

The Material 3 "expressive" bottom nav bar is a hand-rolled composable
(`ui/components/ExpressiveNavBar`) rather than a library component, so the app can follow
the expressive motion guidelines while staying on the stable Material 3 release. Each tab
is a stateless content composable that receives a single `contentPadding` from
`MainScreen`, which owns the one `Scaffold`, app bar, and floating nav bar.

Rationale: opening `overlay/` shows you *everything* about how the island works, rather
than hunting across `services/`, `views/`, `models/` for the pieces.

## 3. Naming

- **Classes / objects**: `PascalCase`, named after their single responsibility
  (`IslandOverlayController`, `SystemEventMonitor`, `IconResolver`).
- **Functions / properties**: `camelCase`, verbs for actions (`resolve`, `scheduleDismiss`),
  nouns/adjectives for values (`isHeadphone`, `customIcons`).
- **Compose composables**: `PascalCase` nouns describing what they render
  (`DynamicIsland`, `SetupScreen`, `PermissionCard`). Private helper composables live in
  the same file as their only caller.
- **Constants**: `UPPER_SNAKE_CASE` inside a `private companion object` at the bottom of
  the class that owns them; never loose top-level magic numbers.
- **Framework services** keep the `Service` suffix so their role is obvious in the
  manifest (`CutoutAccessibilityService`, `CutoutNotificationListenerService`).

## 4. Visibility & structure

- Default to the **narrowest visibility**. Public API is deliberate; helpers are `private`.
- **One top-level type per file**, named after that type. Small, tightly-coupled types
  (e.g. `IslandIcon` + `IslandEvent`) may share a file when they are always used together.
- Keep classes small and single-purpose. "Impure" work that touches the framework
  (`PackageManager`, `ContentResolver`) is concentrated in one place (`IconResolver`,
  `Permissions`, `BitmapUtils`) so the rest of the code stays testable and declarative.
- Lifecycle is explicit and symmetric: everything with a `start()` has a `stop()`, and
  every registration in `start()` is undone in `stop()`.

## 5. Concurrency

- Coroutines only; no raw threads. Each long-lived owner creates its own scope
  (`CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())`) and cancels it in
  `stop()`.
- Cross-component communication uses a cold-to-hot **`SharedFlow`** bus
  (`IslandEventBus`) with `DROP_OLDEST` buffering, so a producer can never block or crash
  a consumer.
- ViewModels expose state as `StateFlow` via `stateIn(..., WhileSubscribed(5_000))` and
  collect with `collectAsStateWithLifecycle()`.

## 6. Compose conventions

- Composables are **stateless where possible**: they take data in and hand events out via
  lambdas (`onOpenIconSettings`, `onChoose`, `onReset`). State is hoisted to the caller or
  the ViewModel.
- Side-effecting reads (permission status) use lifecycle-aware effects
  (`DisposableEffect` + `LifecycleEventObserver`) so the UI refreshes on resume.
- No hardcoded colours in feature UI — pull from `MaterialTheme.colorScheme`. The overlay
  pill is the one intentional exception: it uses fixed near-black/white values because it
  must read correctly over the camera area regardless of the app theme.
- Dimensions are always `.dp` / `.sp`; spacing uses `Arrangement.spacedBy` rather than
  manual padding between siblings.

## 7. Android specifics

- **Permissions**: request nothing silently. Every grant has a check function and a
  matching "open the right settings screen" helper in `Permissions`, and the UI reflects
  live status.
- **No `SYSTEM_ALERT_WINDOW`.** The overlay is drawn from an `AccessibilityService` using
  `TYPE_ACCESSIBILITY_OVERLAY`; the service sets `canRetrieveWindowContent="false"` and
  never inspects screen content. This is a deliberate privacy choice and is documented at
  the call site.
- **Privacy by default**: the notification listener keeps only package + title, never the
  body; the accessibility service reads nothing.
- `runCatching` guards genuinely fallible framework calls (missing package, revoked URI,
  OEM-blocked intent) and always logs or falls back — it is never used to swallow
  programming errors.

## 8. Comments

- Comments explain **why**, not **what**. Every non-obvious class has a short KDoc-style
  block stating its single responsibility and any surprising constraint (e.g. why an
  accessibility service hosts the overlay).
- No commented-out code, no TODOs left in `main`.
