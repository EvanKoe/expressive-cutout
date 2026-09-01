package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.SystemEventType
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.pageTransition
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The screens reachable from the Integrations tab. */
enum class IntegrationsRoute { List, EventIcons, EventDetail }

/** The screen that back navigation returns to from an integrations sub-screen. */
val IntegrationsRoute.parent: IntegrationsRoute
    get() = when (this) {
        IntegrationsRoute.EventDetail -> IntegrationsRoute.EventIcons
        else -> IntegrationsRoute.List
    }

/** How far down the navigation stack a route sits, used to pick the slide direction. */
val IntegrationsRoute.depth: Int
    get() = when (this) {
        IntegrationsRoute.List -> 0
        IntegrationsRoute.EventIcons -> 1
        IntegrationsRoute.EventDetail -> 2
    }

/**
 * "Integrations" destination: surfaces external integrations such as System Events.
 */
@Composable
fun IntegrationsTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: IntegrationsRoute,
    selectedEvent: SystemEventType?,
    onOpenEventIcons: () -> Unit,
    onOpenEvent: (SystemEventType) -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val dir = if (forward) 1 else -1
            pageTransition(appearance.pageTransitionStyle, dir)
        },
        label = "integrationsRoute",
    ) { current ->
        when (current) {
            IntegrationsRoute.List -> IntegrationsList(
                contentPadding = contentPadding,
                onOpenEventIcons = onOpenEventIcons,
            )
            IntegrationsRoute.EventIcons -> EventIconsScreen(viewModel, contentPadding, onOpenEvent)
            IntegrationsRoute.EventDetail ->
                selectedEvent?.let { EventDetailScreen(it, viewModel, contentPadding) }
        }
    }
}

@Composable
private fun IntegrationsList(
    contentPadding: PaddingValues,
    onOpenEventIcons: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsListItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.integrations_system_events_title),
                subtitle = stringResource(R.string.settings_icons_subtitle),
                onClick = onOpenEventIcons,
            )
        }
    }
}
