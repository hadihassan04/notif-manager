package com.tide.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * A slow tide: three sine bands drifting at different speeds, the nearest one
 * opaque and the furthest faint. Phase is driven by one infinite transition, so
 * the whole thing costs a single animation clock regardless of band count.
 */
@Composable
fun TideWaves(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "tide")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val swell by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swell",
    )

    val bands = listOf(
        Triple(0.62f, 0.18f, 1f),
        Triple(0.74f, 0.34f, -1f),
        Triple(0.86f, 0.62f, 2f),
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        bands.forEachIndexed { index, (depth, alpha, speed) ->
            val baseline = h * depth
            val amplitude = h * 0.10f * swell * (1f - index * 0.15f)
            val wavelength = w / (1.1f + index * 0.35f)
            val path = Path().apply {
                moveTo(0f, baseline)
                var x = 0f
                val step = 6f
                while (x <= w) {
                    val y = baseline + amplitude *
                        sin((x / wavelength) * 2f * Math.PI.toFloat() + phase * speed)
                    lineTo(x, y)
                    x += step
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path = path, color = color.copy(alpha = alpha))
        }
    }
}
