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
    primary = Color(0xFF3559C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE2FF),
    onPrimaryContainer = Color(0xFF0A1B5B),
    secondary = Color(0xFF535E7E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2FF),
    onSecondaryContainer = Color(0xFF101B3C),
    tertiary = Color(0xFF006B5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA6F2E4),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191B20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191B20),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF757680),
    outlineVariant = Color(0xFFC5C6D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF002A77),
    primaryContainer = Color(0xFF1A408F),
    onPrimaryContainer = Color(0xFFDDE2FF),
    secondary = Color(0xFFBBC6EA),
    onSecondary = Color(0xFF25304D),
    secondaryContainer = Color(0xFF3B4665),
    onSecondaryContainer = Color(0xFFDCE2FF),
    tertiary = Color(0xFF8AD5C8),
    onTertiary = Color(0xFF00372F),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFFA6F2E4),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF45464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val NotifTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val NotifShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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
        typography = NotifTypography,
        shapes = NotifShapes,
        content = content,
    )
}
