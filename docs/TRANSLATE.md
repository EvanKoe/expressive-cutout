# How to add a new language to the app

If you read those lines, it's most probably because you want to contribute to that project by translating the app in your language. Thanks for taking the time to do it!
  
The good news is that you only need two files to add a new language to the app: one new file, and add a single line in an already existing file.

## Step 1: create the new file

Create the new locale folder and file. It must be named `values-<BCP-47 tag>` (ie. `values-de` or `values-fr-rFR` with region qualifications). Any wrongly-named folder will get its PR rejected.
```bash
mkdir "app/src/main/res/values-de" && cp app/src/main/res/values/strings.xml "app/src/main/res/values-de/strings.xml"
```
Then, the translating work starts. You need to translate each line that is inside the tags (yeah, it's a lot, I know).

> Never translate the `name` attribute. This is the ID the app uses to identify the string!

Example:
```xml
// Example from english to french
<string name="select_all">
    Select all
</string>

// Becomes
<string name="select_all">
    Selectionner tout
</string>
```

## Step 2: update the locale config

In `res/xml/locales_config.xml`, you will find a list of already-available languages for this app. Add yours at the end:
```xml
<locale android:name="{BCP-47 tag}" />

// Example for german
<locale android:name="de" />
```


That's it! Rebuild the app and verify that your language appears in `Profile > Language`.

## Issues you may face
- **Apostrophes and quotes must be escaped**. Replace `'` and `"` with `\'` and `\"`, or the build will fail.
- **Placeholders must survive translation**. Use `%1$d%%` or `%1$s`. How to build them is explained in the next section
- **`&` must be `&amp`**.

### Build your placeholder

Those strings `%1$d%%` and `%1$s` look like a cat walked on your keyboard, but they actually mean something. For example, let's take the second one, which is the placeholder for `test_notification_reply_sent` (`Reply sent: %1$s`):
- `%1$` to take the first argument (can be `%2$`, `%3$`...)
- `s` for **s**tring. It represents the type of variable (`d` for decimal integer...)
In this example, the first argument is `Hello`. So it will display `Reply sent: Hello`.

## Script-specific locales and complete coverage

Android uses `b+` qualifiers for script-specific BCP-47 resource folders. For example,
Simplified Chinese is `values-b+zh+Hans`, paired with `zh-Hans` in `locales_config.xml`.
The picker name comes from the platform's display name for the locale, and includes the script when the tag has one.

In addition to `strings.xml`, translate `additional_strings.xml` and `permission_docs.xml`.
These hold formerly hardcoded event subtitles, permission explanations, accessibility labels,
and searchable icon names. Keep persisted icon IDs and manifest permission names unchanged.
French and Turkish don't ship `additional_strings.xml`/`permission_docs.xml`, so Android falls back to `values/` until a translator adds them. A translated release history is `app/src/main/assets/changelog-<tag>.json` (e.g. `changelog-zh-Hans.json`), with the same versions and list structure as `changelog.json`, picked automatically by language tag.

Run the dependency-free checks from the repository root:

```sh
python3 tools/check_zh_translation.py
python3 tools/check_utf8_and_apk.py
```

To verify that packaging preserved every Chinese string, including Android escapes and embedded
newlines, compare a built APK's compiled resource table against the sources:

```sh
python3 tools/check_utf8_and_apk.py --apk /path/to/app.apk \
  --aapt2 "$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt2"
```

The checks enforce UTF-8 without a BOM or replacement characters, consistent XML encoding,
complete resource coverage, matching format placeholders, stable icon keys, and release-history
structure. They do not replace native-language review or UI testing on different devices.

For a compile-only validation, the same comparison accepts AAPT2's linked `.ap_` file via
`--resources /path/to/linked-resources-binary-format-debug.ap_`; it skips APK-only asset checks.
