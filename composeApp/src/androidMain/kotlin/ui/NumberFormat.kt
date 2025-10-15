package ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import org.example.process_scheduling.shared.R
import java.util.Locale

// Actual implementation for JVM platforms (Android)
actual fun Double.toFormattedString(decimals: Int): String {
    // We use Locale.ROOT to guarantee a period (.) as the decimal separator for consistency.
    val formatString = "%.${decimals}f"
    return String.format(Locale.ROOT, formatString, this)
}


class AndroidImagePainter(private val painter: Painter) : ImagePainter {
    @Composable
    override fun Draw(contentDescription: String?) {
        Image(painter = painter, contentDescription = contentDescription)
    }
}
@Composable
actual fun loadVectorPainter(name: String): ImagePainter {
    val painter = when(name){
        "add" -> painterResource(org.example.process_scheduling.R.drawable.baseline_add_24)
        "close" -> painterResource(org.example.process_scheduling.R.drawable.baseline_close_24)
        "back" -> painterResource(org.example.process_scheduling.R.drawable.baseline_arrow_back_ios_24)
        "right" -> painterResource(org.example.process_scheduling.R.drawable.outline_arrow_right_alt_24)
        else -> painterResource(org.example.process_scheduling.R.drawable.ic_launcher_foreground)
    }

    return AndroidImagePainter(painter)
}




//@Composable
//actual fun loadVectorPainter(name: String): Painter {
//
//    val resId = when (name) {
//        "add" -> org.example.process_scheduling.R.drawable.baseline_add_24
//        "close" -> org.example.process_scheduling.R.drawable.baseline_close_24
//        "back" -> org.example.process_scheduling.R.drawable.baseline_arrow_back_ios_24
//        else -> error("Unknown icon name: $name")
//    }
//    return painterResource(id = resId)
//}