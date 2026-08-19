# Contributing

As an open source project, your PRs and issues are very welcome! 

> Warning! As stated in the README, this project uses AI as coding agents and helpers. I am a newbie in Kotlin so I might have done mistakes (and AI is not always very good at seeing them).

## Propose new features

To propose new features, you can open an issue on Github, but the easiest is to go to [the discord server](https://discord.gg/uG5XWj2N5v) in the `#sugestions` channel. This discord server is also the easiest way to reach me and the community.

## Pull request template
When you are making a pull request, I suggest you come from `dev` and merge to `dev`. `main` is secured and is only used for releases.
There is no specific template, but for PRs, I like lists that go straight to the point.

```
In this PR, I did:
- add this feature,
- fix that bug,
- refactor this part of the code
```

Feel free to add pictures if you want

## Roadmap
This is a simple open-source project that I work on in my free time. It has no intent to be monetized, so it shouldn't be locked under a paywall.

What happened so far:
- [X] Beginning of the project
A few basic features are available. They work for my usecase. Nothing is online yet. 
- [X] Open the repo on Github, post it on social media
This app gets posted on Reddit mostly (in r/FOSS or whatever). A Discord server is created to welcome the community and its ideas. Starting to work on features other people think interesting and fix bugs they encounter.

Wishlist:
- [ ] Improve distribution (F-Droid, IzzyOnDroid, Google Play Store if possible)
- [ ] Reproducible builds,
- [ ] Architecture map section,
- [ ] Improve compatibility (foldables, left camera holes...),
- [ ] Community-maintained tested devices list
- [ ] Community-maintained settings gallery (so user can share their configs using JSON import/export)
- [ ] Translation

Open to:
- New or improved dynamic tiles: if you think it would be nice to have a dedicated dynamic tile for this or that, go for it!
- New or improved system events: make better system events, with more informations is very welcomed,
- Improve UI: following strict Material Expressive rules, if you think I broke one, or have an idea to make it better, go ahead!
- Bug fixes: I can't fix all those bugs. If you want to do it, go for it
- Translation: is not yet a thing, but at some point, I will need to make the app available in many languages. If you can help with that, you are very welcome,

## Coding style

As I am quite new to Kotlin, I don't have many preferences, but here are the few I'd like you to respect.

### Comments

#### Above declarations
Every function, class and top-level property should have a KDoc block describing what it is for. Keep it prose — short and readable — and link related symbols with brackets. Example:

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

Add `@param` / `@return` only when the name doesn't already say it. Simple declarations can use a one-liner:

```kotlin
/** The Material You dynamic roles offered by default, in display order. */
private val DefaultDynamicRoles = listOf(DynamicRole.PRIMARY, DynamicRole.SECONDARY)
```

One exemption: a **one-line pass-through** whose name already says everything needs no KDoc.
`AppViewModel` is full of these — they take a value and hand it straight to a preferences
class, and a comment could only repeat the name:

```kotlin
fun setMusicShowAlbumArt(enabled: Boolean) = viewModelScope.launch {
    musicTilePreferences.setShowAlbumArt(enabled)
}
```

Document it the moment it does anything more than forward its argument — validates, converts,
picks a default, touches two stores. The test is whether a reader learns something.

The same goes for a `Preferences` setter that writes one key and nothing else, and for a
function declared *inside* another function — a local helper in a Composable body belongs to
its caller, and KDoc on it would never be read.

Two more things that count as documented:

- **A run of related declarations** can share one block on the first of them, as long as the
  block speaks for the whole run. `CALL_MIN_WIDTH_PERCENT` / `CALL_MAX_WIDTH_PERCENT` do this,
  and splitting them would say the same thing twice.
- **Members of a documented `sealed interface`** whose parent block already explains the
  variants. Add a block to a variant only when it carries something of its own.

#### Inside Composables
Composable bodies are the one place where `//` comments inside a function are welcome: one short marker per UI block, so the layout can be read at a glance. Example:

```kotlin
Row {
    // Cancel button
    Button(
        param1 = value1,
        param2 = value2,
    ) {
        ...
    }

    // Submit button
    Button(
        param1 = value1,
        param2 = value2,
    ) {
        ...
    }
}
```

This doesn't take much space, but makes it so much easier to read.

#### Explaining why
A `//` comment inside a function body is also welcome when it records **why** something non-obvious is done: a platform quirk, a deliberate workaround, a value that looks arbitrary but isn't. That reasoning is worth keeping next to the code, because it is the one thing a reader can't recover by reading the code itself. Example:

```kotlin
// The listener silently unbinds after an app update, so ask the system to bind it again
requestRebind(componentName)
```

Keep it to one line where you can, and never use `//` to restate what the code already says.

#### Forbidden comments
These comments are forbidden in the code because they break readability:
- `//` comments inside a function body, other than the two cases above (Composable block markers and *why* comments),
- `//` comments that describe *what* the code does — if a line needs one of those, a better name or an extracted function is the real fix,
- `TODO` / `FIXME` comments: if your PR is ready to be merged, it shouldn't contain any.

Please respect the format as well: `/** */` above declarations, `//` only for Composable block markers and *why* comments.
