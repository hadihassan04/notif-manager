package com.tide.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tide.app.ui.theme.MdSpacing

/**
 * Inbox and Schedule bento pieces: a wave-filled hero, supporting tiles, and a
 * full-width action card. Material You supplies the hue; Waiting and Open
 * stay distinct through container roles.
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
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$eyebrow. $value. $caption" },
        color = container,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
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
                    .padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TideMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val container = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val onMuted = if (accent) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .heightIn(min = 104.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = "$label. $value" },
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

@OptIn(ExperimentalFoundationApi::class)
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
    val onMuted = if (primary) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(onClick = onClick)
            .semantics { contentDescription = "$title. $body" },
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
