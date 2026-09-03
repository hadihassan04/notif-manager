package com.tide.app.ui.theme

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}

private val FallbackLightColors = lightColorScheme(
    primary = Color(0xFF1F6493),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE5F8),
    onPrimaryContainer = Color(0xFF042C48),
    secondary = Color(0xFF4C7793),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5F3),
    onSecondaryContainer = Color(0xFF0D2A3D),
    tertiary = Color(0xFF0F6B68),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA8F0EB),
    onTertiaryContainer = Color(0xFF00201F),
    background = Color(0xFFF6FAFE),
    onBackground = Color(0xFF171C21),
    surface = Color(0xFFF6FAFE),
    onSurface = Color(0xFF171C21),
    surfaceVariant = Color(0xFFDCE7F1),
    onSurfaceVariant = Color(0xFF414B55),
    outline = Color(0xFF71808C),
    outlineVariant = Color(0xFFC1CDD8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFF98CBF2),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF0F4A72),
    onPrimaryContainer = Color(0xFFCFE5F8),
    secondary = Color(0xFFB3C9DC),
    onSecondary = Color(0xFF1D3242),
    secondaryContainer = Color(0xFF334959),
    onSecondaryContainer = Color(0xFFD3E5F3),
    tertiary = Color(0xFF7CD4CF),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF00504D),
    onTertiaryContainer = Color(0xFFA8F0EB),
    background = Color(0xFF0E1519),
    onBackground = Color(0xFFDEE4EA),
    surface = Color(0xFF0E1519),
    onSurface = Color(0xFFDEE4EA),
    surfaceVariant = Color(0xFF414B55),
    onSurfaceVariant = Color(0xFFC1CDD8),
    outline = Color(0xFF8B98A4),
    outlineVariant = Color(0xFF414B55),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val TideTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val TideShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun TideTheme(
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
        typography = TideTypography,
        shapes = TideShapes,
        content = content,
    )
}
