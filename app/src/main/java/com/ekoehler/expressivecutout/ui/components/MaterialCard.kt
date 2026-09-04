package com.ekoehler.expressivecutout.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Grouped-list item shape: rounded at the group's outer edges, tight between stacked items. */
fun groupedShape(isFirst: Boolean = false, isLast: Boolean = false) = RoundedCornerShape(
    topStart = if (isFirst) 24.dp else 4.dp,
    topEnd = if (isFirst) 24.dp else 4.dp,
    bottomStart = if (isLast) 24.dp else 4.dp,
    bottomEnd = if (isLast) 24.dp else 4.dp,
)

@Composable
fun MaterialCard(
    shape: Shape = groupedShape(isFirst = false, isLast = false),
    children: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            children()
        }
    }
}