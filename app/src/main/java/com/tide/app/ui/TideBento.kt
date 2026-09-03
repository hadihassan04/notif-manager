package com.tide.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tide.app.ui.theme.MdSpacing
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Electron-style bento pieces, in Tide's Material You palette: a tall hero with
 * a huge number and a wave fill, supporting metric tiles (one of which can take
 * the accent), and fat function cards with a toggle plus a slider.
 */
@Composable
fun TideHeroCard(
    eyebrow: String,
    value: String,
    caption: String,
    fill: Float,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val container = if (accent) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = if (accent) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val wave = if (accent) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .clip(MaterialTheme.shapes.extraLarge),
        ) {
            val waveHeight = (0.28f + 0.62f * fill.coerceIn(0f, 1f)).coerceIn(0.28f, 0.9f)
            TideWaves(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(waveHeight),
                color = wave,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = MdSpacing.md, vertical = MdSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = onContainer.copy(alpha = 0.78f))
                Text(
                    value,
                    style = MaterialTheme.typography.displayLarge,
                    color = onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainer.copy(alpha = 0.86f),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun TideGaugeHero(
    value: String,
    label: String,
    progress: Float,
    marker: Float?,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = MaterialTheme.colorScheme.primary
    val needle = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.width - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val start = 180f
                    val sweep = 180f
                    drawArc(
                        color = track,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = fill,
                        startAngle = start,
                        sweepAngle = sweep * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    marker?.let { mark ->
                        val angle = Math.toRadians((180.0 + 180.0 * mark.coerceIn(0f, 1f)))
                        val radius = (arcSize.width / 2f)
                        val cx = topLeft.x + radius
                        val cy = topLeft.y + radius
                        val mx = cx + radius * cos(angle).toFloat()
                        val my = cy + radius * sin(angle).toFloat()
                        drawCircle(color = needle, radius = 7.dp.toPx(), center = Offset(mx, my))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.displaySmall, color = fill)
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TideMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val container = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val onMuted = if (accent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.heightIn(min = 132.dp),
        color = container,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = onMuted)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TideActionCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val onMuted = if (primary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        color = container,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = onContainer)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = onMuted)
        }
    }
}

@Composable
fun TideFunctionCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sliderLabel: String,
    sliderValue: Int,
    sliderRange: IntRange,
    onSliderFinished: (Int) -> Unit,
    formatSlider: (Int) -> String,
    modifier: Modifier = Modifier,
    sliderStep: Int = 5,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    var sliding by remember(sliderValue) { mutableIntStateOf(sliderValue) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
            Text(
                "$sliderLabel ${formatSlider(sliding)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = sliding.toFloat(),
                onValueChange = { next ->
                    val snapped = ((next / sliderStep).roundToInt() * sliderStep)
                        .coerceIn(sliderRange.first, sliderRange.last)
                    if (snapped != sliding) {
                        sliding = snapped
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                valueRange = sliderRange.first.toFloat()..sliderRange.last.toFloat(),
                onValueChangeFinished = { onSliderFinished(sliding) },
            )
            extra()
        }
    }
}
