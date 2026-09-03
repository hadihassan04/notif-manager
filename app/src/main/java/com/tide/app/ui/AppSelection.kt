package com.tide.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tide.app.core.AppSelectionGroup
import com.tide.app.core.pickerSelectionGroup
import com.tide.app.data.InstalledApp
import com.tide.app.ui.theme.MdSpacing

/**
 * Shared Instant/Batch picker used by onboarding and the Apps tab.
 * Compact icon grid: tap toggles Instant. Long-press (Apps tab) opens channel exceptions.
 */
@Composable
fun AppSelectionPane(
    apps: List<InstalledApp>,
    isInstant: (InstalledApp) -> Boolean,
    onToggle: (InstalledApp, Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    onLongClick: ((InstalledApp) -> Unit)? = null,
    exceptionCount: (InstalledApp) -> Int = { 0 },
    searchPlaceholder: String = "Search apps",
    contentPadding: PaddingValues = PaddingValues(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
) {
    val visible = remember(apps, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) apps
        else apps.filter { app ->
            app.label.contains(normalized, ignoreCase = true) ||
                app.packageName.contains(normalized, ignoreCase = true)
        }
    }
    val grouped = remember(visible, visible.map { it.packageName to isInstant(it) }) {
        AppSelectionGroup.entries.map { group ->
            group to visible
                .filter { pickerSelectionGroup(it.role, isInstant(it)) == group }
                .sortedBy { it.label.lowercase() }
        }.filter { (_, rows) -> rows.isNotEmpty() }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.sm)
                .padding(top = MdSpacing.sm, bottom = MdSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
            content = header,
        )
        SearchField(
            value = query,
            onQueryChange = onQueryChange,
            placeholder = searchPlaceholder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.sm)
                .padding(bottom = MdSpacing.xs),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 76.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (query.isBlank()) {
                            "No apps are available yet. You can pick Instant apps later."
                        } else {
                            "No apps match this search."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(MdSpacing.sm),
                    )
                }
            }
            grouped.forEach { (group, rows) ->
                item(key = "section_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                    AppSelectionSectionLabel(group = group)
                }
                items(rows, key = { "${group.name}_${it.packageName}" }) { app ->
                    val instant = isInstant(app)
                    AppSelectionTile(
                        app = app,
                        instant = instant,
                        exceptionCount = exceptionCount(app),
                        onToggle = { onToggle(app, !instant) },
                        onLongClick = onLongClick?.let { click -> { click(app) } },
                    )
                }
            }
        }
    }
}

@Composable
fun AppSelectionSectionLabel(group: AppSelectionGroup) {
    Column(
        modifier = Modifier.padding(top = MdSpacing.xs, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            group.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (group.body.isNotBlank()) {
            Text(
                group.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSelectionTile(
    app: InstalledApp,
    instant: Boolean,
    exceptionCount: Int,
    onToggle: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val status = if (instant) "Instant" else "Waiting"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongClick,
            )
            .semantics(mergeDescendants = true) {
                selected = instant
                contentDescription = "${app.label}, $status"
            }
            .padding(vertical = MdSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            AppIcon(
                packageName = app.packageName,
                label = app.label,
                modifier = Modifier.fillMaxSize(),
            )
            if (instant) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, MaterialTheme.colorScheme.primary, AppIconShape),
                )
            }
            if (exceptionCount > 0) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "$exceptionCount channel exceptions",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp),
                )
            }
        }
        Text(
            app.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = CircleShape,
    )
}
