package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import java.util.Locale
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp


// Actual implementation for JVM platforms (Desktop)
actual fun Double.toFormattedString(decimals: Int): String {
    // We use Locale.ROOT to guarantee a period (.) as the decimal separator,
    // avoiding issues where local settings might use a comma (,) which breaks display consistency.
    val formatString = "%.${decimals}f"
    return String.format(Locale.ROOT, formatString, this)
}


class DesktopVectorPainter(private val painter: Painter) : ImagePainter {
    @Composable
    override fun Draw(contentDescription: String?) {
        Image(painter = painter, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Composable
actual fun loadVectorPainter(name: String): ImagePainter {
    val bitmap = useResource("icons/$name.png", ::loadImageBitmap)
    return DesktopVectorPainter(BitmapPainter(bitmap))

}