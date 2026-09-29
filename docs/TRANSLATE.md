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

Those strings `%1$d%%` and `%1$s` look like a cat walked on your keyboard, but they actually mean something. For example, let's take the first one, who is the placeholder for `assistant_max_height_value`:
- `%1$` to take the first argument (can be `%2$`, `%3$`...)
- `d` for **d**ecimal integer. It represents the type of variable (`s` for string...)
- `%%` for a literal `%`. Percent needs to be escaped as well
In this example, the first argument is `45`. So it will display `45%`.
