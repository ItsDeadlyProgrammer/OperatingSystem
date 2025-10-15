package org.example.process_scheduling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import ui.Home

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            AppTheme(darkTheme = darkTheme) {
                Home()
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppAndroidPreview() {
    AppTheme(darkTheme = true) {
        Home()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Example purple
    secondary = Color(0xFF03DAC5), // Example teal
    background = Color(0xFF121212), // Dark background
    surface = Color(0xFF1E1E1E),   // Dark surface
    onBackground = Color.White,
    onSurface = Color.White,
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Example purple
    secondary = Color(0xFF03DAC5), // Example teal
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)


@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
