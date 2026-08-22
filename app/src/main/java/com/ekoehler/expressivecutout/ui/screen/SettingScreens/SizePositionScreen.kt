package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutMetrics
import com.ekoehler.expressivecutout.core.IslandPreviewBus
import com.ekoehler.expressivecutout.data.IslandDimensions
import com.ekoehler.expressivecutout.data.IslandLayout
import com.ekoehler.expressivecutout.overlay.IslandEvent
import com.ekoehler.expressivecutout.overlay.IslandIcon
import com.ekoehler.expressivecutout.permissions.Permissions
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

@Composable
internal fun SizePositionScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Cutout-aware defaults: centre the pill behind the physical camera and match the expanded
    // island's corners to the device's own rounded corners. Falls back to the static defaults when
    // the device reports no cutout / rounded corners (or predates the APIs).
    val view = LocalView.current
    val density = LocalDensity.current.density
    val cutoutOffsetXDp = remember(view, density) {
        CutoutMetrics.cutoutCenterPx(view)?.let { center ->
            val screenWidthPx = view.resources.displayMetrics.widthPixels
            ((center.x - screenWidthPx / 2f) / density).roundToInt()
                .coerceIn(IslandDimensions.MIN_OFFSET_X_DP, IslandDimensions.MAX_OFFSET_X_DP)
        }
    }
    val screenCornerDp = remember(view, density) {
        CutoutMetrics.screenCornerRadiusPx(view)?.let { radiusPx ->
            (radiusPx / density).roundToInt()
                .coerceIn(IslandDimensions.MIN_CORNER_DP, IslandDimensions.MAX_CORNER_DP)
        }
    }
    val collapsedDefaults = remember(cutoutOffsetXDp) {
        cutoutOffsetXDp?.let { IslandLayout.DEFAULT_COLLAPSED.copy(offsetXDp = it) }
            ?: IslandLayout.DEFAULT_COLLAPSED
    }
    val expandedDefaults = remember(cutoutOffsetXDp, screenCornerDp) {
        var d = IslandLayout.DEFAULT_EXPANDED
        cutoutOffsetXDp?.let { d = d.copy(offsetXDp = it) }
        screenCornerDp?.let { c ->
            d = d.copy(
                cornerTopLeftDp = c,
                cornerTopRightDp = c,
                cornerBottomLeftDp = c,
                cornerBottomRightDp = c,
            )
        }
        d
    }

    // Pin the real overlay open only on this screen, gated on accessibility. The pinned island
    // mirrors the tab being edited (collapsed vs expanded).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        fun refresh() = IslandPreviewBus.setActive(Permissions.isAccessibilityGranted(context))
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> refresh()
                Lifecycle.Event.ON_PAUSE -> IslandPreviewBus.setActive(false)
                else -> Unit
            }
        }
        refresh()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            IslandPreviewBus.setActive(false)
            IslandPreviewBus.setExpandedPreview(false)
        }
    }
    LaunchedEffect(tab) { IslandPreviewBus.setExpandedPreview(tab == 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpressiveSegmentedRow(
            options = listOf(
                stringResource(R.string.tab_normal),
                stringResource(R.string.tab_expanded),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp),
        )
        when (tab) {
            0 -> DimensionsEditor(
                dimensions = layout.collapsed,
                defaults = collapsedDefaults,
                expandedPreview = false,
                onChange = viewModel::setCollapsedDimensions,
            )

            else -> DimensionsEditor(
                dimensions = layout.expanded,
                defaults = expandedDefaults,
                expandedPreview = true,
                onChange = viewModel::setExpandedDimensions,
            )
        }
    }
}

@Composable
private fun DimensionsEditor(
    dimensions: IslandDimensions,
    defaults: IslandDimensions,
    expandedPreview: Boolean,
    onChange: (IslandDimensions) -> Unit,
) {
    var width by remember(dimensions.widthPercent) { mutableStateOf(dimensions.widthPercent.toFloat()) }
    var height by remember(dimensions.heightDp) { mutableStateOf(dimensions.heightDp.toFloat()) }
    var topMargin by remember(dimensions.topMarginDp) { mutableStateOf(dimensions.topMarginDp.toFloat()) }
    var offsetX by remember(dimensions.offsetXDp) { mutableStateOf(dimensions.offsetXDp.toFloat()) }
    var offsetY by remember(dimensions.offsetYDp) { mutableStateOf(dimensions.offsetYDp.toFloat()) }
    var cornerTl by remember(dimensions.cornerTopLeftDp) { mutableStateOf(dimensions.cornerTopLeftDp.toFloat()) }
    var cornerTr by remember(dimensions.cornerTopRightDp) { mutableStateOf(dimensions.cornerTopRightDp.toFloat()) }
    var cornerBl by remember(dimensions.cornerBottomLeftDp) { mutableStateOf(dimensions.cornerBottomLeftDp.toFloat()) }
    var cornerBr by remember(dimensions.cornerBottomRightDp) { mutableStateOf(dimensions.cornerBottomRightDp.toFloat()) }
    // Start on the mode that matches the saved radii (re-derived when the persisted dimensions
    // load in or the tab switches), so opening the screen reflects the current shape.
    var cornerMode by remember(dimensions) { mutableStateOf(cornerModeFor(dimensions)) }

    fun commit() = onChange(
        IslandDimensions.of(
            widthPercent = width.roundToInt(),
            heightDp = height.roundToInt(),
            offsetXDp = offsetX.roundToInt(),
            offsetYDp = offsetY.roundToInt(),
            cornerTopLeftDp = cornerTl.roundToInt(),
            cornerTopRightDp = cornerTr.roundToInt(),
            cornerBottomLeftDp = cornerBl.roundToInt(),
            cornerBottomRightDp = cornerBr.roundToInt(),
            topMarginDp = topMargin.roundToInt(),
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdjustableSlider(
            label = stringResource(R.string.appearance_width),
            valueText = "${width.roundToInt()}%",
            value = width,
            valueRange = IslandDimensions.MIN_WIDTH_PERCENT.toFloat()..IslandDimensions.MAX_WIDTH_PERCENT.toFloat(),
            step = 1f,
            onValueChange = { width = it },
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_height),
            valueText = "${height.roundToInt()} dp",
            value = height,
            valueRange = IslandDimensions.MIN_HEIGHT_DP.toFloat()..IslandDimensions.MAX_HEIGHT_DP.toFloat(),
            step = 2f,
            onValueChange = { height = it },
            onCommit = { commit() },
        )
        if (expandedPreview) {
            AdjustableSlider(
                label = stringResource(R.string.appearance_top_margin),
                valueText = "${topMargin.roundToInt()} dp",
                value = topMargin,
                valueRange = IslandDimensions.MIN_TOP_MARGIN_DP.toFloat()..IslandDimensions.MAX_TOP_MARGIN_DP.toFloat(),
                step = 2f,
                onValueChange = { topMargin = it },
                onCommit = { commit() },
            )
        }
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
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_vertical),
            valueText = "${offsetY.roundToInt()} dp",
            value = offsetY,
            valueRange = IslandDimensions.MIN_OFFSET_Y_DP.toFloat()..IslandDimensions.MAX_OFFSET_Y_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetY = it },
            onCommit = { commit() },
        )
        AdjustableSlider(
            label = stringResource(R.string.appearance_horizontal),
            valueText = "${offsetX.roundToInt()} dp",
            value = offsetX,
            valueRange = IslandDimensions.MIN_OFFSET_X_DP.toFloat()..IslandDimensions.MAX_OFFSET_X_DP.toFloat(),
            step = 2f,
            onValueChange = { offsetX = it },
            onCommit = { commit() },
        )

        Spacer(Modifier.size(4.dp))
        Button(
            onClick = {
                width = defaults.widthPercent.toFloat()
                height = defaults.heightDp.toFloat()
                topMargin = defaults.topMarginDp.toFloat()
                offsetX = defaults.offsetXDp.toFloat()
                offsetY = defaults.offsetYDp.toFloat()
                cornerTl = defaults.cornerTopLeftDp.toFloat()
                cornerTr = defaults.cornerTopRightDp.toFloat()
                cornerBl = defaults.cornerBottomLeftDp.toFloat()
                cornerBr = defaults.cornerBottomRightDp.toFloat()
                onChange(defaults)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_reset_layout))
        }
    }
}

/**
 * How many corner radii the user is editing at once: one for all four, one per side pair, or each
 * corner on its own.
 */
private enum class CornerMode { All, TopBottom, Each }

/**
 * The narrowest [CornerMode] that can represent [d]'s radii: [CornerMode.All] when all four match,
 * [CornerMode.TopBottom] when the top pair and bottom pair each match, otherwise [CornerMode.Each].
 */
private fun cornerModeFor(d: IslandDimensions): CornerMode = when {
    d.cornerTopLeftDp == d.cornerTopRightDp &&
        d.cornerBottomLeftDp == d.cornerBottomRightDp &&
        d.cornerTopLeftDp == d.cornerBottomLeftDp -> CornerMode.All

    d.cornerTopLeftDp == d.cornerTopRightDp &&
        d.cornerBottomLeftDp == d.cornerBottomRightDp -> CornerMode.TopBottom

    else -> CornerMode.Each
}

@Composable
private fun CornerRadiusControls(
    cornerTl: Float,
    cornerTr: Float,
    cornerBl: Float,
    cornerBr: Float,
    mode: CornerMode,
    onModeChange: (CornerMode) -> Unit,
    onTlChange: (Float) -> Unit,
    onTrChange: (Float) -> Unit,
    onBlChange: (Float) -> Unit,
    onBrChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    val range = IslandDimensions.MIN_CORNER_DP.toFloat()..IslandDimensions.MAX_CORNER_DP.toFloat()
    val modes = CornerMode.entries

    fun onCornerChanged(callback: (Float) -> Unit, newCorner: Float) {
        if (newCorner >= 1) {
            callback(newCorner)
        }
    }

    fun onAllChanged(newCorner: Float) {
        if (newCorner >= 1) {
            onTlChange(newCorner)
            onBlChange(newCorner)
            onBrChange(newCorner)
            onTrChange(newCorner)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.appearance_corner),
            style = MaterialTheme.typography.bodyMedium,
        )
        ExpressiveSegmentedRow(
            options = modes.map { cornerMode ->
                when (cornerMode) {
                    CornerMode.All -> stringResource(R.string.corner_mode_all)
                    CornerMode.TopBottom -> stringResource(R.string.corner_mode_split)
                    CornerMode.Each -> stringResource(R.string.corner_mode_each)
                }
            },
            selectedIndex = mode.ordinal,
            onSelect = { onModeChange(modes[it]) },
            modifier = Modifier.fillMaxWidth(),
        )

        when (mode) {
            CornerMode.All -> CornerSlider(
                label = stringResource(R.string.appearance_corner_all),
                value = cornerTl,
                range = range,
                onValueChange = { onAllChanged(it) },
                onCommit = onCommit,
            )

            CornerMode.TopBottom -> {
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_top),
                    value = cornerTl,
                    range = range,
                    onValueChange = { onCornerChanged(onTlChange, it); onCornerChanged(onTrChange, it) },
                    onCommit = onCommit,
                )
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_bottom),
                    value = cornerBl,
                    range = range,
                    onValueChange = { onCornerChanged(onBlChange, it); onCornerChanged(onBrChange, it) },
                    onCommit = onCommit,
                )
            }

            CornerMode.Each -> {
                CornerSlider(stringResource(R.string.appearance_corner_tl),cornerTl, range, { onCornerChanged(onTlChange, it) }, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_tr), cornerTr, range, { onCornerChanged(onTrChange, it) }, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_bl), cornerBl, range, { onCornerChanged(onBlChange, it) }, onCommit)
                CornerSlider(stringResource(R.string.appearance_corner_br), cornerBr, range, { onCornerChanged(onBrChange, it) }, onCommit)
            }
        }
    }
}

@Composable
private fun CornerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    AdjustableSlider(
        label = label,
        valueText = "${value.roundToInt()} dp",
        value = value,
        valueRange = range,
        step = 1f,
        onValueChange = onValueChange,
        onCommit = onCommit,
    )
}
