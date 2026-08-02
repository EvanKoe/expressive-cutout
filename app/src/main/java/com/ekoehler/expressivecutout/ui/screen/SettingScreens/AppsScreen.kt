package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists every launchable app on the device with a switch per app. Turning an app off mutes it on
 * the cutout entirely: neither its notifications nor its media reach the island. Enabled is the
 * default, so only the opt-outs are stored (see [com.ekoehler.expressivecutout.data.AppPreferences]).
 *
 * The list is read off the main thread once; each row loads its own launcher icon lazily, so
 * scrolling a few hundred apps never holds every icon bitmap in memory at once.
 */
@Composable
internal fun AppsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val disabled by viewModel.disabledApps.collectAsStateWithLifecycle()
    val apps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    var query by rememberSaveable { mutableStateOf("") }

    val loaded = apps
    if (loaded == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val filtered = remember(loaded, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            loaded
        } else {
            loaded.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
    }
    val lastIndex = filtered.lastIndex

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = stringResource(R.string.apps_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.apps_search_hint)) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.apps_none_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                shape = appGroupShape(index = index, lastIndex = lastIndex),
                enabled = app.packageName !in disabled,
                onEnabledChange = { viewModel.setAppEnabled(app.packageName, it) },
            )
        }
    }
}

/** Grouped-list corners: the group's outer corners (first top, last bottom) are 32dp, rest 4dp. */
private fun appGroupShape(index: Int, lastIndex: Int): Shape = RoundedCornerShape(
    topStart = if (index == 0) 32.dp else 4.dp,
    topEnd = if (index == 0) 32.dp else 4.dp,
    bottomStart = if (index == lastIndex) 32.dp else 4.dp,
    bottomEnd = if (index == lastIndex) 32.dp else 4.dp,
)

@Composable
private fun AppCard(
    app: InstalledApp,
    shape: Shape,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = app.packageName)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

/**
 * The app's launcher icon, rasterised off the main thread the first time this row is composed and
 * dropped again once it scrolls out. A neutral placeholder stands in while it loads, or for good if
 * the icon can't be read.
 */
@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) { loadAppIcon(context, packageName) }
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** An installed app the user can allow or mute. Icons are loaded per row, not held here. */
internal data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * Every app with a launcher entry, minus this one, sorted by name. Resolving the launcher intent
 * (rather than asking for every installed package) keeps the list to apps the user recognises and
 * avoids the restricted QUERY_ALL_PACKAGES permission — the manifest's <queries> element is what
 * makes these visible on Android 11+.
 */
private fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }.getOrDefault(emptyList())

    return resolved.asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}

/** The app's launcher icon at list size, or null if the package went away while we scrolled. */
private fun loadAppIcon(context: Context, packageName: String): ImageBitmap? = runCatching {
    context.packageManager.getApplicationIcon(packageName).toListIconBitmap()
}.getOrNull()

/**
 * Rasterises a launcher icon at a fixed small size. Adaptive icons report an intrinsic size well
 * above what a 44dp badge needs, so drawing them at their natural size would waste several
 * megabytes across a long list.
 */
private fun Drawable.toListIconBitmap(): ImageBitmap {
    val bitmap = createBitmap(ICON_PX, ICON_PX)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, ICON_PX, ICON_PX)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private const val ICON_PX = 96
