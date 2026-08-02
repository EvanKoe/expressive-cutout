package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.DynamicTile
import com.ekoehler.expressivecutout.ui.AppViewModel

/**
 * Settings for the assistant dynamic tile: whether to display the text answer in the cutout,
 * the max cutout height as a percentage of the screen height, and the tile's icon container colour.
 */
@Composable
internal fun AssistantScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.assistantTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            title = stringResource(R.string.assistant_display_answer_title),
            description = stringResource(R.string.assistant_display_answer_desc),
            checked = settings.displayAnswerInCutout,
            onCheckedChange = viewModel::setAssistantDisplayAnswerInCutout,
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(4.dp),
            title = stringResource(R.string.assistant_animated_icon_title),
            description = stringResource(R.string.assistant_animated_icon_desc),
            checked = settings.useAnimatedIcon,
            onCheckedChange = viewModel::setAssistantUseAnimatedIcon,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var sliderValue by remember(settings.maxCutoutHeightPercent) {
                    mutableFloatStateOf(settings.maxCutoutHeightPercent.toFloat())
                }
                AdjustableSlider(
                    label = stringResource(R.string.assistant_max_height_title),
                    valueText = stringResource(R.string.assistant_max_height_desc, sliderValue.toInt()),
                    value = sliderValue,
                    valueRange = 10f..80f,
                    step = 5f,
                    onValueChange = { sliderValue = it },
                    onCommit = { viewModel.setAssistantMaxCutoutHeightPercent(sliderValue.toInt()) },
                )
            }
        }

        Text(
            text = stringResource(R.string.tile_icon_container_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        )
        ColorPickerCard(
            label = stringResource(R.string.tile_icon_container_label),
            selected = settings.iconContainerColor,
            onSelect = viewModel::setAssistantIconContainerColor,
            defaultLabel = stringResource(R.string.music_default_accent),
            defaultColor = Color(DynamicTile.ASSISTANT.accent),
        )

        Text(
            text = stringResource(R.string.assistant_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
