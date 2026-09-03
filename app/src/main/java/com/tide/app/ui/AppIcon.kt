package com.tide.app.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal object AppIconCache {
    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    fun cached(packageName: String): ImageBitmap? = icons[packageName]

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        cached(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            icons[packageName] ?: runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = 96, height = 96)
                    .asImageBitmap()
            }.getOrNull()?.also { icons[packageName] = it }
        }
    }
}

@Composable
fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // `produceState` restarts its producer for a key change but retains the underlying
    // state object. A keyed composition boundary recreates it with an empty/new-package
    // value, preventing a recycled row from briefly or permanently showing the prior icon.
    key(packageName) {
        val bitmap by produceState<ImageBitmap?>(initialValue = AppIconCache.cached(packageName)) {
            value = AppIconCache.cached(packageName) ?: AppIconCache.load(context.applicationContext, packageName)
        }
        Surface(modifier = modifier.clip(MaterialTheme.shapes.medium), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                val icon = bitmap
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = "$label icon", modifier = Modifier.fillMaxSize())
                } else {
                    Text(label.take(1), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
