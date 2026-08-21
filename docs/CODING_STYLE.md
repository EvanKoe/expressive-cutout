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
├── ExpressiveCutoutApp.kt  Application: process-lifetime singletons + hidden-API exemption
├── MainActivity.kt         The single activity, hosting MainScreen
├── core/          Pure model, the event buses, and the shortcut executor (no Android UI,
│                  no framework services)
├── data/          Persistence: one DataStore-backed `*Preferences` class per settings area,
│                  each next to the settings types it stores
├── events/        System-event detection (broadcasts, callbacks) and notification parsers
├── notifications/ The self-posted test notification, test call, and their receivers
├── overlay/       The island: rendering (DynamicIsland), lifecycle host, icon resolution,
│                  motion constants, the Material icon catalogue
├── permissions/   Permission checks and settings-intent helpers
├── service/       The two OS-instantiated services, plus NotificationAlerter, which is the
│                  listener's own helper and has no meaning outside it
├── system/        Privileged work borrowed through Shizuku: the bridge state and the
│                  status-bar icon controller
└── ui/            AppViewModel, MainScreen, and everything the user sees in-app
    ├── components/    Reusable widgets (the expressive nav bar, the colour picker card)
    ├── screen/        The three home tabs (SettingsTab, PermissionsTab, ProfileTab) plus
    │   │              the standalone screens they push to
    │   ├── SettingScreens/    One file per settings screen; `Appearance/` nests the
    │   │                      sub-screens of AppearanceScreen
    │   └── tiles/             One file per dynamic-tile settings screen
    └── theme/         Colour scheme, typography, and the app theme
```

The Material 3 "expressive" bottom nav bar is a hand-rolled composable
(`ui/components/ExpressiveNavBar`) rather than a library component, so the app can follow
the expressive motion guidelines while staying on the stable Material 3 release. Each tab
is a stateless content composable that receives a single `contentPadding` from
`MainScreen`, which owns the one `Scaffold` and the floating nav bar. There is deliberately
no `TopAppBar` anywhere: each screen draws its own title, so the island preview can sit at
the top of the window where the real cutout is.

Rationale: opening `overlay/` shows you *everything* about how the island works, rather
than hunting across `services/`, `views/`, `models/` for the pieces.

**This tree is part of the style guide.** A PR that adds, removes, or renames a package
updates it in the same commit — see the *Project layout* rule in
[CONTRIBUTING.md](../CONTRIBUTING.md).

## 3. Naming

- **Classes / objects**: `PascalCase`, named after their single responsibility
  (`IslandOverlayController`, `SystemEventMonitor`, `IconResolver`).
- **Functions / properties**: `camelCase`, verbs for actions (`resolve`, `scheduleDismiss`),
  nouns/adjectives for values (`isHeadphone`, `customIcons`).
- **Compose composables**: `PascalCase` nouns describing what they render
  (`DynamicIsland`, `SetupScreen`, `PermissionCard`). Private helper composables live in
  the same file as their only caller.
- **Constants**: `UPPER_SNAKE_CASE`, using the Android style guide's definition — a `val`
  with no custom `get`, holding deeply immutable data. Scalars and strings also take
  `const`. Anything that isn't deeply immutable stays `camelCase` even when it's a `val`:
  `iconCache` (an `LruCache`) and `iconLoadLimit` (a `Semaphore`) are `val`s but not
  constants, and a **non-empty array is mutable**, so `defaultPattern` in
  `NotificationAlerter` is camelCase despite never being written to.
  Two things keep their own naming and aren't constants: a `CompositionLocal` follows the
  Compose convention (`LocalActionButtonAnimation`), and a `Context` extension property
  backed by a delegate (`Context.appearanceDataStore`) has a getter, so it can't be one.
- **Where constants live**: either top-level in the file that uses them, or in a
  `companion object` when they belong to one type. Both are correct; Google's rule is that
  a constant "can only be defined inside of an `object` or as a top-level declaration", so
  a file-local `private const val TAG` at the top of the file is idiomatic, not a magic
  number. What is *not* allowed is an unnamed literal buried in an expression — every
  tuning value gets a name, wherever it lives.
- **Constant visibility**: the narrowest that works, which is usually not `private`.
  `private` for a value only its own file or class reads (`TAG`, a layout gap). Public for
  anything another file legitimately needs — the `DEFAULT_*` values the UI shows as
  defaults, and the `MIN_*` / `MAX_*` ranges the sliders and the JSON importer both clamp
  against. Making those private would force the caller to hardcode the number, which is
  the actual thing worth avoiding.
- **Framework services** keep the `Service` suffix so their role is obvious in the
  manifest (`CutoutAccessibilityService`, `CutoutNotificationListenerService`).

## 4. Visibility & structure

- Default to the **narrowest visibility**. Public API is deliberate; helpers are `private`.
- **One top-level type per file**, named after that type. Small, tightly-coupled types
  (e.g. `IslandIcon` + `IslandEvent`) may share a file when they are always used together.
- Keep classes small and single-purpose. "Impure" work that touches the framework
  (`PackageManager`, `ContentResolver`) is concentrated in one place (`IconResolver`,
  `Permissions`, `BitmapUtils`) so the rest of the code stays testable and declarative.
- Lifecycle is explicit and symmetric: anything owned by a service, an activity, or a
  composable has a `stop()` for its `start()`, and every registration in `start()` is
  undone in `stop()`. `IslandOverlayController`, `SystemEventMonitor` and
  `MediaPlaybackMonitor` are the pattern — the accessibility service starts all three on
  bind and tears them down on unbind.
- **A `start()` with no `stop()` must say why, in a comment.** Not stopping something is
  a real decision, and the next reader can't tell a deliberate one from a forgotten one by
  looking. Put the reason in the KDoc on `start()` (or on the type, if it's the type's
  whole nature) — one sentence naming what owns the lifetime instead. Without that
  sentence, a missing `stop()` is treated as a bug in review.
- **Process-lifetime singletons are the exception, deliberately.** `ShizukuState` and
  `StatusBarIconController` are started once from `Application.onCreate()` and have no
  `stop()`, because there is no moment at which stopping them would be correct: Android
  never calls a reliable teardown on `Application` (`onTerminate` doesn't fire on real
  devices), so the pair would be dead code. For `StatusBarIconController` it would be
  worse than dead — the disable flags are held against a `Binder` token *on purpose*, so
  that a killed process restores the status bar by itself. A `stop()` that dropped the
  token would hand callers a way to silently undo the user's setting.
- A method named `stop()` isn't always a lifecycle pair. `NotificationAlerter.stop()`
  stops the *ringtone* that `alert()` started; its partner is `alert()`, not a `start()`.

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
- **No hardcoded colours in UI components — the whole app is Material You compliant.**
  Every colour a settings screen paints comes from `MaterialTheme.colorScheme`, so it
  follows the wallpaper on Android 12+ and flips with light/dark. A `Text` with a literal
  black `color` is a bug: it stays black when the user switches to dark mode and becomes
  invisible. If a colour needs to contrast against something, derive it
  (`contentColorFor`, `onSurface`, a `luminance()` test against the actual background) —
  never assume which way round the theme is.
  Three narrow exceptions, and nothing else:
  - **The overlay pill.** It sits over the camera area on top of whatever app is running,
    so it can't take its colours from the app theme. Its defaults are fixed
    near-black/white, and everything above that is the user's own saved `CutoutColor` /
    `CutoutFill` — which may itself be a Material You `DynamicRole`.
  - **Island previews inside settings.** A preview of the pill has to show the pill's real
    colours, not the screen's, or it would preview a lie.
  - **A colour the user is picking.** Swatch palettes and the colour picker render the
    value itself; that value is the content, not the theme.
- Dimensions are always `.dp` / `.sp`; spacing uses `Arrangement.spacedBy` rather than
  manual padding between siblings.
- Every `items` / `itemsIndexed` block in a `LazyColumn`, `LazyRow`, or lazy grid **must**
  pass a stable `key`. Without it Compose falls back to positional keys, so an insertion,
  removal, or reorder throws away composition state and re-composes every row below the
  change; with it, only the affected items move and `Modifier.animateItem()` can track them.
  Standalone `item` blocks in the same list take a literal key (`item(key = "header")`) so
  they stay stable when the keyed items around them change.

## 7. Android specifics

- **Permissions**: request nothing silently. Every grant has a check function and a
  matching "open the right settings screen" helper in `Permissions`, and the UI reflects
  live status.
- **No `SYSTEM_ALERT_WINDOW`.** The overlay is drawn from an `AccessibilityService` using
  `TYPE_ACCESSIBILITY_OVERLAY`, which is the whole reason the service exists.
- **Screen reads are feature-scoped and never leave the device.** The service does set
  `canRetrieveWindowContent="true"`, because the assistant tile has to read the answer text
  out of an assistant window to display it. That read is gated on
  `isAssistantPackage()` — a fixed list of known assistant packages — and for every other
  app the service takes the package name and nothing else. Text that is read goes straight
  onto `IslandEventBus`, is rendered, and is dropped; it is never written to DataStore, to
  a log, or to a file. The app holds no `INTERNET` permission, so it *cannot* be sent
  anywhere. Any change that widens what is read updates the README, the manifest comment,
  and `accessibility_service_description` in the same commit.
- **Privacy by default**: the notification listener keeps only package + title, never the
  body.
- `runCatching` guards genuinely fallible framework calls (missing package, revoked URI,
  OEM-blocked intent) and always logs or falls back — it is never used to swallow
  programming errors.

## 8. Comments

- Comments explain **why**, not **what**. Every non-obvious class has a short KDoc-style
  block stating its single responsibility and any surprising constraint (e.g. why an
  accessibility service hosts the overlay).
- No commented-out code, no TODOs left in `main`.
