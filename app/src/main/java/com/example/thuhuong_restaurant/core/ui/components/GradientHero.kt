package com.example.thuhuong_restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thuhuong_restaurant.core.ui.theme.ThPrimary
import com.example.thuhuong_restaurant.core.ui.theme.ThPrimaryDark

/** Matches the web's `linear-gradient(135deg, #a9583e 0%, #cc785c 100%)` compact hero banner. */
@Composable
fun GradientHero(
    title: String,
    subtitle: String,
    eyebrow: String = "Nhà hàng Thu Hương",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(ThPrimaryDark, ThPrimary),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = eyebrow.uppercase(),
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
