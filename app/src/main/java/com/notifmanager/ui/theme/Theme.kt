package com.notifmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}

private val FallbackLightColors = lightColorScheme(
    primary = Color(0xFF386A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCECE0),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4B635D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8E0),
    onSecondaryContainer = Color(0xFF07201B),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBE6FF),
    onTertiaryContainer = Color(0xFF001E31),
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFFA0D0C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F5147),
    onPrimaryContainer = Color(0xFFBCECE0),
    secondary = Color(0xFFB1CCC4),
    onSecondary = Color(0xFF1D352F),
    secondaryContainer = Color(0xFF344C46),
    onSecondaryContainer = Color(0xFFCDE8E0),
    tertiary = Color(0xFFACCBE5),
    onTertiary = Color(0xFF123349),
    tertiaryContainer = Color(0xFF2D4960),
    onTertiaryContainer = Color(0xFFCBE6FF),
)

@Composable
fun NotifManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
