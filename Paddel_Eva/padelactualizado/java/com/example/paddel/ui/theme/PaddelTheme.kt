package com.example.paddel.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Colores personalizados para modo claro
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color.Black,

    secondary = Color(0xFF9C27B0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1BEE7),
    onSecondaryContainer = Color.Black,

    tertiary = Color(0xFF03A9F4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB3E5FC),
    onTertiaryContainer = Color.Black,

    background = Color.White,
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,
)

// Colores personalizados para modo oscuro
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1), // Azul claro
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0097A7), // Azul intenso
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF90CAF9), // Azul pastel
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0D47A1), // Azul profundo
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFCE93D8), // Violeta espacial
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5E35B1), // Morado oscuro
    onTertiaryContainer = Color.White,

    background = Color(0xFF121212), // Negro profundo
    onBackground = Color.White,

    surface = Color(0xFF1E1E1E), // Gris muy oscuro
    onSurface = Color.White,
)

/*
@Composable
fun PaddelTheme(
    darkTheme: Boolean = false, // Ahora por defecto es falso
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Cambiar colores de barra de estado y navegación
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colorScheme.background.copy(alpha = 0.8f),
            darkIcons = !darkTheme
        )
    }

    // Aquí se define el tema completo
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
 */