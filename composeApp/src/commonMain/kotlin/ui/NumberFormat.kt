package ui

import androidx.compose.runtime.Composable

/**
 * Expected function for formatting a Double to a string with a specified number of decimal places.
 * This function must have an 'actual' implementation in each platform-specific module
 * (e.g., desktopMain, androidMain, iosMain).
 */
expect fun Double.toFormattedString(decimals: Int = 2): String

interface ImagePainter {
    @Composable
    fun Draw(contentDescription: String? = null)
}

@Composable
expect fun loadVectorPainter(name: String): ImagePainter