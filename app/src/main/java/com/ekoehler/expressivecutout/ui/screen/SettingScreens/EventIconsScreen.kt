package com.ekoehler.expressivecutout.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.overlay.animatedIcon
import com.ekoehler.expressivecutout.overlay.animationLoopsByDefault
import com.ekoehler.expressivecutout.overlay.forRole
import com.ekoehler.expressivecutout.overlay.loadImageBitmapOrNull
import com.ekoehler.expressivecutout.overlay.onForRole
import com.ekoehler.expressivecutout.overlay.toImageBitmap
import com.ekoehler.expressivecutout.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun EventIconsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenEvent: (SystemEventType) -> Unit,
) {
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()
    val eventEnabled by viewModel.eventEnabled.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.eventDynamicColor.collectAsStateWithLifecycle()
    val dynamicColorRole by viewModel.eventDynamicColorRole.collectAsStateWithLifecycle()
    val dynamicColorOpacity by viewModel.eventDynamicColorOpacity.collectAsStateWithLifecycle()
    val animatedIcons by viewModel.eventAnimatedIcons.collectAsStateWithLifecycle()
    val animatedIconLoops by viewModel.eventAnimatedIconLoops.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            contentPadding = contentPadding
        ) {
            val lastIndex = SystemEventType.entries.lastIndex
            // The dynamic-colour toggle is the top row of the same grouped list, so it carries the
            // group's rounded top corners; the events below flow on beneath it.
            item(key = "dynamic_container") {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                        .clip(shape = RoundedCornerShape(32.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsToggleCard(
                        shape = RoundedCornerShape(4.dp),
                        title = stringResource(R.string.dynamic_event_color),
                        description = stringResource(R.string.dynamic_event_color_desc),
                        checked = dynamicColor,
                        onCheckedChange = { viewModel.setEventDynamicColor(it) },
                    )

                    // The role picker + opacity slider only make sense while dynamic colour is on.
                    AnimatedVisibility(visible = dynamicColor) {
                        DynamicColorOptionsCard(
                            shape = RoundedCornerShape(4.dp),
                            role = dynamicColorRole,
                            opacity = dynamicColorOpacity,
                            onRoleChange = { viewModel.setEventDynamicColorRole(it) },
                            onOpacityChange = { viewModel.setEventDynamicColorOpacity(it) },
                        )
                    }
                }
            }

            itemsIndexed(SystemEventType.entries, key = { _, type -> type.name }) { index, type ->
                // Grouped list: 4dp between, and the last event carries the group's rounded bottom
                // corners (the toggle above holds the top ones).
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) 32.dp else 4.dp,
                    topEnd = if (index == 0) 32.dp else 4.dp,
                    bottomStart = if (index == lastIndex) 32.dp else 4.dp,
                    bottomEnd = if (index == lastIndex) 32.dp else 4.dp,
                )
                EventIconCard(
                    type = type,
                    source = customIcons[type],
                    shape = shape,
                    enabled = eventEnabled[type] != false,
                    dynamicColor = dynamicColor,
                    dynamicColorRole = dynamicColorRole,
                    dynamicColorOpacity = dynamicColorOpacity,
                    animate = animatedIcons[type] ?: true,
                    loop = animatedIconLoops[type] ?: type.animationLoopsByDefault(),
                    onEnabledChange = { viewModel.setEventEnabled(type, it) },
                    onClick = { onOpenEvent(type) },
                )
            }
        }
    }
}

/**
 * The controls shown beneath the toggle while "Dynamic color for all events" is on: a row of
 * Material You role swatches (primary / secondary / tertiary) and an opacity slider for the
 * role-coloured badge background.
 */
@Composable
private fun DynamicColorOptionsCard(
    shape: Shape,
    role: DynamicRole,
    opacity: Float,
    onRoleChange: (DynamicRole) -> Unit,
    onOpacityChange: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.dynamic_event_color_role_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RoleSwatch(DynamicRole.PRIMARY, R.string.role_primary, role, onRoleChange)
                RoleSwatch(DynamicRole.SECONDARY, R.string.role_secondary, role, onRoleChange)
                RoleSwatch(DynamicRole.TERTIARY, R.string.role_tertiary, role, onRoleChange)
            }
            AdjustableSlider(
                label = stringResource(R.string.opacity),
                valueText = "${(opacity * 100).roundToInt()}%",
                value = opacity,
                valueRange = 0f..1f,
                step = 0.05f,
                onValueChange = onOpacityChange,
                onCommit = {},
            )
        }
    }
}

/** A circular swatch filled with a Material You [role] colour; a ring + check marks the selection. */
@Composable
private fun RoleSwatch(
    role: DynamicRole,
    labelRes: Int,
    selected: DynamicRole,
    onClick: (DynamicRole) -> Unit,
) {
    val isSelected = role == selected
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.forRole(role))
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clickable { onClick(role) },
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onForRole(role),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun EventIconCard(
    type: SystemEventType,
    source: IconSource?,
    shape: Shape,
    enabled: Boolean,
    dynamicColor: Boolean,
    dynamicColorRole: DynamicRole,
    dynamicColorOpacity: Float,
    animate: Boolean,
    loop: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(R.string.label_default)
    val imageLabel = stringResource(R.string.label_custom)
    val appFallback = stringResource(R.string.label_app)
    // Resolve the app label once per package (a PackageManager binder call), not on every
    // recomposition/scroll frame.
    val appPackage = (source as? IconSource.App)?.packageName
    val resolvedAppName = remember(appPackage) { appPackage?.let { appLabelOf(context, it) } }
    val sourceLabel = when (source) {
        null -> defaultLabel
        is IconSource.Image -> imageLabel
        is IconSource.App -> resolvedAppName ?: appFallback
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tapping the card opens the event's detail screen; dimmed when the event is disabled.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.4f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventIconThumbnail(
                    type = type,
                    source = source,
                    dynamicColor = dynamicColor,
                    dynamicColorRole = dynamicColorRole,
                    dynamicColorOpacity = dynamicColorOpacity,
                    animate = animate,
                    loop = loop,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(type.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Thin divider between the (tappable) row and the switch, matching the dynamic tiles list.
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
internal fun EventIconThumbnail(
    type: SystemEventType,
    source: IconSource?,
    dynamicColor: Boolean,
    dynamicColorRole: DynamicRole,
    dynamicColorOpacity: Float,
    size: Dp = 48.dp,
    // Whether the event's Lottie preview is shown at all, and whether it loops — mirrors the
    // per-event "Animated icon" / "Loop" toggles so the badge matches what the cutout will do.
    animate: Boolean = true,
    loop: Boolean = true,
) {
    val context = LocalContext.current
    // Inner glyph/image/animation sizes are defined against the 48dp list badge; scale them with the
    // requested badge size so the detail screen can show a larger hero without new magic numbers.
    val sizeScale = size / 48.dp
    // Mirror the overlay's IconBadge: with "Dynamic color" on, a role-coloured disc (at the chosen
    // opacity) + its matching "on" glyph replaces the event's own accent tint. Both colours are
    // animated so toggling the setting (or dragging the opacity slider) crossfades rather than snaps
    // — the opacity rides along in the badge colour's alpha.
    val targetBadge = if (dynamicColor) {
        MaterialTheme.colorScheme.forRole(dynamicColorRole).copy(alpha = dynamicColorOpacity)
    } else {
        Color(type.accent).copy(alpha = 0.18f)
    }
    val badgeColor by animateColorAsState(targetBadge, label = "eventBadgeColor")
    val targetGlyph = if (dynamicColor) {
        MaterialTheme.colorScheme.onForRole(dynamicColorRole)
    } else {
        Color(type.accent)
    }
    val glyphColor by animateColorAsState(targetGlyph, label = "eventGlyphColor")
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = when (val current = source) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(current.uri).loadImageBitmapOrNull(context)
            }

            is IconSource.App -> withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(current.packageName).toImageBitmap()
                }.getOrNull()
            }

            null -> null
        }
    }

    // Events that render as motion on the cutout (e.g. the charging bolt) preview their animation here
    // so it's visible at a glance — unless the user turned the animated icon off. A custom image/app
    // override still wins over the animation.
    val animated = remember(type) { type.animatedIcon() }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        when {
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(32.dp * sizeScale).clip(CircleShape),
            )

            animate && animated != null -> {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(animated.resId),
                )
                // Recolour to the glyph colour when the animation follows the theme (as on the cutout).
                val dynamicProperties = if (animated.tint) {
                    rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.COLOR_FILTER,
                            value = SimpleColorFilter(glyphColor.toArgb()),
                            keyPath = arrayOf("**"),
                        ),
                    )
                } else {
                    null
                }
                LottieAnimation(
                    composition = composition,
                    iterations = if (loop) LottieConstants.IterateForever else 1,
                    dynamicProperties = dynamicProperties,
                    // Match the overlay's scaling; the oversized art is clipped to the badge circle.
                    modifier = Modifier.requiredSize(24.dp * animated.scale * sizeScale),
                )
            }

            else -> Icon(
                imageVector = type.defaultIcon,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier.size(24.dp * sizeScale),
            )
        }
    }
}

internal fun appLabelOf(context: Context, packageName: String): String? = runCatching {
    val info = context.packageManager.getApplicationInfo(packageName, 0)
    context.packageManager.getApplicationLabel(info).toString()
}.getOrNull()
