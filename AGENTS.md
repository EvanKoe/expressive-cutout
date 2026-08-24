# AGENTS.md

Instructions for AI coding agents working in this repository. Human contributors should read
[CONTRIBUTING.md](CONTRIBUTING.md) — this file is the same rules, written to be followed.

This project openly uses AI agents as helpers. That is exactly why these rules are strict:
the maintainer is new to Kotlin, so code that is hard to read is code that can't be reviewed.

## The project

- **Expressive Cutout** — an Android dynamic island overlay drawn over the camera cutout.
- Kotlin, Jetpack Compose, Material 3 ("expressive"). JVM target 17.
- Package `com.ekoehler.expressivecutout`, `minSdk 29`, `targetSdk 35`. Single `app` module.
- Source is grouped by responsibility, not by Android component type: `core/`, `data/`,
  `events/`, `notifications/`, `overlay/`, `permissions/`, `service/`, `system/`, `ui/`.
- Dependencies live once in `gradle/libs.versions.toml` and are referenced as `libs.*`.
  Never hardcode a version in `build.gradle.kts`.

## Build and verify

```bash
./gradlew assembleDebug
```

Build before you claim a change works. There is no unit or instrumentation test suite yet
(`app/src/main` only), so a successful build plus — for anything visible — a check on a real
device is the whole verification story. Say plainly which of the two you actually did.

## Comments

This is the part agents get wrong most often. Read all four rules.

### KDoc above every declaration

Every function, class and top-level property gets a `/** */` block saying what it is for.
Prose, short, readable. Link related symbols as `[Symbol]`.

```kotlin
/**
 * A Material 3 "expressive" bottom bar: a floating rounded container whose selected item
 * animates into a filled pill. Presentational — selection is hoisted to the caller.
 */
@Composable
fun ExpressiveNavBar(items: List<NavItem>, selectedIndex: Int) {
    ...
}
```

Add `@param` / `@return` only when the name doesn't already say it. Simple declarations take
a one-liner:

```kotlin
/** The Material You dynamic roles offered by default, in display order. */
private val DefaultDynamicRoles = listOf(DynamicRole.PRIMARY, DynamicRole.SECONDARY)
```

**Exemptions.** No KDoc is needed for:

- A one-line pass-through whose name already says everything — an `AppViewModel` setter that
  forwards its argument, or a `Preferences` setter that writes one key. Document it as soon as
  it validates, converts, picks a default, or touches two stores.
- A function declared inside another function. A local helper in a Composable body belongs to
  its caller.
- The rest of a run of related declarations whose first member carries a block covering all of
  them (`CALL_MIN_WIDTH_PERCENT` / `CALL_MAX_WIDTH_PERCENT`).
- A `sealed interface` variant whose parent block already explains it.

Never write a KDoc that only restates the declaration's name.

### `//` inside a function body: exactly two allowed cases

**1. Block markers in Composable bodies** — one short marker per UI block, so the layout
reads at a glance:

```kotlin
Row {
    // Cancel button
    Button(...) { ... }

    // Submit button
    Button(...) { ... }
}
```

**2. Why-comments** — recording *why* something non-obvious is done: a platform quirk, a
deliberate workaround, a value that looks arbitrary but isn't. Keep it to one line:

```kotlin
// The listener silently unbinds after an app update, so ask the system to bind it again
requestRebind(componentName)
```

### Never

- A `//` comment that restates **what** the code does. If a line seems to need one, the real
  fix is a better name or an extracted function.
- Any other in-body comment beyond the two cases above.
- `TODO` / `FIXME` in code you propose for merge. Open an issue instead.
- Commented-out code.

## Code conventions

[docs/CODING_STYLE.md](docs/CODING_STYLE.md) is the full reference — read it before a
non-trivial change. The rules most often broken:

- Default to the **narrowest visibility**; one top-level type per file, named after it.
- Constants go in `UPPER_SNAKE_CASE` in a `private companion object` at the bottom of the
  owning class. No loose top-level magic numbers.
- Coroutines only, no raw threads. Lifecycle is symmetric: everything with `start()` has a
  `stop()`, and every registration is undone there.
- Composables are stateless where possible — data in, events out via lambdas, state hoisted
  to the caller or the ViewModel.
- No hardcoded colours in feature UI; pull from `MaterialTheme.colorScheme`. The overlay pill
  is the one deliberate exception and is documented at its call site.
- `runCatching` guards genuinely fallible framework calls and always logs or falls back.
  Never use it to swallow a programming error.

## Privacy constraints — do not weaken these

- No `SYSTEM_ALERT_WINDOW`. The overlay is drawn from an `AccessibilityService` using
  `TYPE_ACCESSIBILITY_OVERLAY`, with `canRetrieveWindowContent="false"`.
- The accessibility service reads no screen content. The notification listener keeps package
  and title only, never the body.

If a feature seems to need one of these relaxed, stop and say so instead of doing it.

## Git and pull requests

- Branch from `dev` and open the PR against `dev`. `main` is protected and used for releases
  only.
- Don't commit or push unless the human asks. When asked: one feature per commit, subject
  prefixed `[FEAT]` or `[FIX]`.
- PR body is a short bullet list that goes straight to the point:

```
In this PR, I did:
- add this feature,
- fix that bug,
- refactor this part of the code
```

- Don't touch `versionCode` / `versionName`; releases are the maintainer's call.
- Ask before adding a dependency.
