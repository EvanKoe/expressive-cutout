package com.ekoehler.expressivecutout.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.AppearanceSettings
import com.ekoehler.expressivecutout.data.CutoutColor
import com.ekoehler.expressivecutout.data.DynamicRole
import com.ekoehler.expressivecutout.data.IconSource
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.overlay.DismissBackdrop
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.overlay.IslandPreview
import com.ekoehler.expressivecutout.overlay.badgeIconSizeFor
import com.ekoehler.expressivecutout.overlay.outlineStroke
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ColorPickerCard
import com.ekoehler.expressivecutout.ui.components.MaterialCard
import com.ekoehler.expressivecutout.ui.components.PageTitle
import com.ekoehler.expressivecutout.ui.components.groupedShape
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Accent the preview notification carries, matching the other appearance previews. */
private val PREVIEW_ACCENT = Color(0xFF60A5FA)

/** The preview island sits closer to the top than the real one, which clears a camera. */
private const val PREVIEW_TOP_MARGIN_DP = 18

/** How far the preview island is slid aside, as a share of its own width. */
private const val PREVIEW_SLIDE_FRACTION = 0.34f

/** How long the preview island takes to come back to rest before it swipes out again. */
private const val PREVIEW_RETURN_MS = 240

/** How long the island takes to swipe aside, matching the unhurried feel of a real dismiss. */
private const val PREVIEW_SWIPE_MS = 420

/** A beat at rest, long enough to read the new design before the island leaves again. */
private const val PREVIEW_HOLD_MS = 140L

/** How long the options and the preview take to settle after the switch is flipped. */
private const val REVEAL_MS = 280

/** The content fades over this, offset from the resize so it never shows up squashed. */
private const val REVEAL_FADE_MS = 180

/**
 * The dynamic roles the dismiss backdrop offers: the three accents, then the neutral surface tiers
 * from lowest to highest — first the ones that follow the phone's theme, then the always-dark ones,
 * which is what a panel sitting behind a dark island usually wants on a light home screen.
 */
private val BACKDROP_DYNAMIC_ROLES = listOf(
    DynamicRole.PRIMARY,
    DynamicRole.SECONDARY,
    DynamicRole.TERTIARY,
    DynamicRole.SURFACE_CONTAINER_LOWEST,
    DynamicRole.SURFACE_CONTAINER_LOW,
    DynamicRole.SURFACE_CONTAINER,
    DynamicRole.SURFACE_CONTAINER_HIGH,
    DynamicRole.SURFACE_CONTAINER_HIGHEST,
    DynamicRole.SURFACE_CONTAINER_LOWEST_DARK,
    DynamicRole.SURFACE_CONTAINER_LOW_DARK,
    DynamicRole.SURFACE_CONTAINER_DARK,
    DynamicRole.SURFACE_CONTAINER_HIGH_DARK,
    DynamicRole.SURFACE_CONTAINER_HIGHEST_DARK,
)

/**
 * "Dismiss inside a window" screen (reached from the Appearance screen). A swipe can either carry
 * the cutout away and fade it out, or slide it inside a fixed window that truncates it and uncovers
 * a backdrop. The preview at the top holds a dismiss mid-swipe so both readings are visible, and
 * everything below it styles that window.
 */
@Composable
internal fun DismissWindowScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    var showIconSheet by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // Without the persisted grant the overlay loses the image the next time it starts.
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setDismissImageIcon(uri.toString())
        }
    }

    val previewAppName = stringResource(R.string.app_name)
    val previewLabel = stringResource(R.string.preview_label)
    val previewDetail = stringResource(R.string.preview_detail)
    val previewEvent = remember(previewAppName, previewLabel, previewDetail) {
        IslandEvent(
            id = 0L,
            icon = IslandIcon.Vector(Icons.Rounded.Notifications),
            label = previewLabel,
            detail = previewDetail,
            appName = previewAppName,
            postTimeMs = System.currentTimeMillis(),
            accent = PREVIEW_ACCENT,
        )
    }

    // Flipping the switch replays the gesture: the island comes back to rest, takes on the new
    // design while it is sitting still, then swipes aside again. Entering the screen plays the
    // second half only, so the preview arrives mid-demonstration rather than frozen.
    val slide = remember { Animatable(0f) }
    var previewWindowed by remember { mutableStateOf(appearance.windowDismiss) }
    LaunchedEffect(appearance.windowDismiss) {
        if (previewWindowed != appearance.windowDismiss) {
            slide.animateTo(0f, tween(PREVIEW_RETURN_MS, easing = FastOutSlowInEasing))
            previewWindowed = appearance.windowDismiss
            delay(PREVIEW_HOLD_MS)
        }
        slide.animateTo(PREVIEW_SLIDE_FRACTION, tween(PREVIEW_SWIPE_MS, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PageTitle(text = stringResource(R.string.appearance_window_dismiss_title))

        DismissWindowPreview(
            appearance = appearance,
            expanded = layout.expanded,
            collapsedHeightDp = layout.collapsed.heightDp,
            event = previewEvent,
            windowed = previewWindowed,
            slide = slide.value,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // The switch the whole screen hangs off.
        SettingsToggleCard(
            shape = groupedShape(isFirst = true, isLast = true),
            title = stringResource(R.string.appearance_window_dismiss_enable),
            description = stringResource(R.string.appearance_window_dismiss_desc),
            checked = appearance.windowDismiss,
            onCheckedChange = viewModel::setWindowDismiss,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = appearance.windowDismiss,
            enter = expandVertically(tween(REVEAL_MS, easing = FastOutSlowInEasing)) +
                fadeIn(tween(REVEAL_FADE_MS, delayMillis = REVEAL_MS - REVEAL_FADE_MS)),
            exit = fadeOut(tween(REVEAL_FADE_MS / 2)) +
                shrinkVertically(tween(REVEAL_MS, delayMillis = REVEAL_FADE_MS / 2, easing = FastOutSlowInEasing)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Dismiss icon picker
                DismissIconCard(
                    icon = appearance.dismissIcon,
                    backdropColor = appearance.dismissBackdropColor,
                    iconColor = appearance.dismissIconColor,
                    shape = groupedShape(isFirst = true),
                    onClick = { showIconSheet = true },
                )

                // Dismiss icon color selector
                ColorPickerCard(
                    label = stringResource(R.string.appearance_dismiss_icon_color),
                    selected = appearance.dismissIconColor,
                    onSelect = viewModel::setDismissIconColor,
                    defaultLabel = stringResource(R.string.appearance_text_color_auto),
                    shape = groupedShape(),
                    allowAppIcon = true,
                )

                // Dismiss backdrop color selector
                ColorPickerCard(
                    label = stringResource(R.string.appearance_dismiss_backdrop_color),
                    selected = appearance.dismissBackdropColor,
                    onSelect = viewModel::setDismissBackdropColor,
                    defaultLabel = stringResource(R.string.label_default),
                    dynamicRoles = BACKDROP_DYNAMIC_ROLES,
                    shape = groupedShape(),
                    allowAppIcon = true,
                )

                // Sliding island corner radii
                DismissCornerCard(
                    appearance = appearance,
                    expanded = layout.expanded,
                    onCommit = viewModel::setDismissCorners,
                    onReset = viewModel::resetDismissCorners,
                )
            }
        }
    }

    if (showIconSheet) {
        IconChooserSheet(
            hasOverride = appearance.dismissIcon != null,
            onChooseImage = {
                showIconSheet = false
                imagePicker.launch(arrayOf("image/*"))
            },
            onChooseMaterial = {
                showIconSheet = false
                showIconPicker = true
            },
            onUseDefault = {
                showIconSheet = false
                viewModel.resetDismissIcon()
            },
            onDismiss = { showIconSheet = false },
        )
    }

    if (showIconPicker) {
        MaterialIconPickerSheet(
            onPick = { iconName ->
                showIconPicker = false
                viewModel.setDismissMaterialIcon(iconName)
            },
            onDismiss = { showIconPicker = false },
        )
    }
}

/**
 * A dismiss held mid-swipe. With the mode on, the island is truncated by the window and the
 * backdrop shows in the gap it left; with it off, the same swipe simply carries the island aside
 * and fades it, so the switch above can be read as a before/after.
 */
@Composable
private fun DismissWindowPreview(
    appearance: AppearanceSettings,
    expanded: IslandDimensions,
    collapsedHeightDp: Int,
    event: IslandEvent,
    windowed: Boolean,
    slide: Float,
) {
    val windowShape = RoundedCornerShape(
        topStart = expanded.cornerTopLeftDp.dp,
        topEnd = expanded.cornerTopRightDp.dp,
        bottomStart = expanded.cornerBottomLeftDp.dp,
        bottomEnd = expanded.cornerBottomRightDp.dp,
    )
    val stroke = appearance.outlineStroke(null, null)
    // Off, the island is just the cutout, so it wears the radius Size & position gave it; the
    // sliding island's own radius only exists inside the window.
    fun innerCorner(override: Int?, own: Int) = if (windowed) override ?: own else own

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val previewWidth = maxWidth * (expanded.widthPercent / 100f)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .then(
                        if (windowed && stroke != null) Modifier.border(stroke, windowShape)
                        else Modifier
                    )
                    .then(if (windowed) Modifier.clip(windowShape) else Modifier),
            ) {
                if (windowed) {
                    DismissBackdrop(
                        appearance = appearance,
                        appColor = null,
                        iconSize = badgeIconSizeFor(collapsedHeightDp),
                        gapOnStart = true,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                Box(
                    modifier = Modifier.graphicsLayer {
                        translationX = size.width * slide
                        // Off, the island fades as it travels, exactly as the old dismiss does.
                        alpha = if (windowed) 1f else (1f - slide).coerceIn(0.25f, 1f)
                    },
                ) {
                    IslandPreview(
                        event = event,
                        width = previewWidth,
                        heightDp = expanded.heightDp,
                        cornerTopLeftDp = innerCorner(appearance.dismissCornerTopLeftDp, expanded.cornerTopLeftDp),
                        cornerTopRightDp = innerCorner(appearance.dismissCornerTopRightDp, expanded.cornerTopRightDp),
                        cornerBottomLeftDp = innerCorner(appearance.dismissCornerBottomLeftDp, expanded.cornerBottomLeftDp),
                        cornerBottomRightDp = innerCorner(appearance.dismissCornerBottomRightDp, expanded.cornerBottomRightDp),
                        topMarginDp = PREVIEW_TOP_MARGIN_DP,
                        expanded = true,
                        // The window draws the outline in this mode, so the island must not.
                        appearance = if (windowed) appearance.copy(strokeEnabled = false) else appearance,
                        showActions = false,
                        collapsedHeightDp = 12,
                    )
                }
            }
        }
    }
}

/**
 * The corner radii of the island that slides inside the dismiss window, with the same all /
 * top-bottom / each control the cutout's own corners get in Size & position. Until the user touches
 * it, each radius follows [expanded]'s matching corner, and [onReset] hands them back to it.
 */
@Composable
private fun DismissCornerCard(
    appearance: AppearanceSettings,
    expanded: IslandDimensions,
    onCommit: (Int, Int, Int, Int) -> Unit,
    onReset: () -> Unit,
) {
    val tlDp = appearance.dismissCornerTopLeftDp ?: expanded.cornerTopLeftDp
    val trDp = appearance.dismissCornerTopRightDp ?: expanded.cornerTopRightDp
    val blDp = appearance.dismissCornerBottomLeftDp ?: expanded.cornerBottomLeftDp
    val brDp = appearance.dismissCornerBottomRightDp ?: expanded.cornerBottomRightDp

    var cornerTl by remember(tlDp) { mutableStateOf(tlDp.toFloat()) }
    var cornerTr by remember(trDp) { mutableStateOf(trDp.toFloat()) }
    var cornerBl by remember(blDp) { mutableStateOf(blDp.toFloat()) }
    var cornerBr by remember(brDp) { mutableStateOf(brDp.toFloat()) }
    var cornerMode by remember(tlDp, trDp, blDp, brDp) {
        mutableStateOf(cornerModeFor(tlDp, trDp, blDp, brDp))
    }

    val overridden = appearance.dismissCornerTopLeftDp != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = groupedShape(isLast = true),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_dismiss_corners),
                style = MaterialTheme.typography.titleMedium,
            )

            CornerRadiusControls(
                cornerTl = cornerTl,
                cornerTr = cornerTr,
                cornerBl = cornerBl,
                cornerBr = cornerBr,
                mode = cornerMode,
                onModeChange = { cornerMode = it },
                onTlChange = { cornerTl = it },
                onTrChange = { cornerTr = it },
                onBlChange = { cornerBl = it },
                onBrChange = { cornerBr = it },
                onCommit = {
                    onCommit(
                        cornerTl.roundToInt(),
                        cornerTr.roundToInt(),
                        cornerBl.roundToInt(),
                        cornerBr.roundToInt(),
                    )
                },
            )

            AnimatedVisibility(visible = overridden) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.appearance_dismiss_corners_follow))
                }
            }
        }
    }
}

/** A clickable card opening the picker for the glyph drawn on the dismiss backdrop. */
@Composable
private fun DismissIconCard(
    icon: IconSource?,
    backdropColor: CutoutColor?,
    iconColor: CutoutColor?,
    onClick: () -> Unit,
    shape: RoundedCornerShape = groupedShape(),
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmptyIconThumbnail(
                source = icon,
                containerColor = backdropColor,
                size = 40.dp,
                glyphColor = iconColor,
                placeholder = Icons.Rounded.Delete,
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.appearance_dismiss_icon),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.appearance_dismiss_icon_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
