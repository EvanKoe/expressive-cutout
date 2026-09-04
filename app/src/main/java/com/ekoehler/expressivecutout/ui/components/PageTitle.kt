@file:OptIn(ExperimentalTextApi::class)

package com.ekoehler.expressivecutout.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.R

private val RobotoFlexDisplay = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.W700,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(125f),
            FontVariation.opticalSizing(96.sp)
        )
    )
)

@Composable
fun PageTitle(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: TextUnit = MaterialTheme.typography.headlineLarge.fontSize
) {
    val style: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontFamily = RobotoFlexDisplay,
        fontWeight = FontWeight.W700,
    )

    Text(
        text = text,
        style = style,
        color = color,
        fontSize = size,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
