package com.tide.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tide.app.core.AppSelectionGroup
import com.tide.app.data.InstalledApp
import com.tide.app.ui.theme.MdSpacing

/**
 * Shared Instant/Batch picker used by onboarding and the Priority screen.
 * The header stays put; only the grouped list scrolls, so rows are not clipped
 * into titles, waves, or navigation.
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
    mediaNotice: (@Composable () -> Unit)? = null,
    onRowClick: ((InstalledApp) -> Unit)? = null,
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
    val grouped = remember(visible) {
        AppSelectionGroup.entries.map { group ->
            group to visible
                .filter { it.role.selectionGroup == group }
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
        if (mediaNotice != null) {
            Column(Modifier.padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xxs)) {
                mediaNotice()
            }
        }
        SearchField(
            value = query,
            onQueryChange = onQueryChange,
            placeholder = searchPlaceholder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.sm)
                .padding(bottom = MdSpacing.xs),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            if (visible.isEmpty()) {
                item(key = "empty") {
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
                item(key = "section_${group.name}") {
                    AppSelectionSectionLabel(group = group)
                }
                items(rows, key = { "${group.name}_${it.packageName}" }) { app ->
                    AppSelectionRow(
                        app = app,
                        instant = app.role.lockedInstant || isInstant(app),
                        locked = app.role.lockedInstant,
                        exceptionCount = exceptionCount(app),
                        onToggle = { onToggle(app, it) },
                        onRowClick = onRowClick?.let { click -> { click(app) } },
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
        Text(
            group.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AppSelectionRow(
    app: InstalledApp,
    instant: Boolean,
    locked: Boolean,
    exceptionCount: Int,
    onToggle: (Boolean) -> Unit,
    onRowClick: (() -> Unit)?,
) {
    val status = when {
        locked -> "Instant · keeps playback working"
        instant -> "Instant · always gets through"
        else -> "Batch · waits for delivery"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onRowClick != null) {
                    Modifier.clickable(onClick = onRowClick)
                } else {
                    Modifier.clickable(enabled = !locked) { onToggle(!instant) }
                },
            ),
        color = if (instant) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            AppIcon(packageName = app.packageName, label = app.label, modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (instant) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (exceptionCount > 0) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "$exceptionCount channel exceptions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Switch(
                checked = instant,
                enabled = !locked,
                onCheckedChange = onToggle,
            )
        }
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
