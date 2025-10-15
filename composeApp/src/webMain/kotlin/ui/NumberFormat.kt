package ui


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt


actual fun Double.toFormattedString(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val roundedValue = (this * factor).roundToInt() / factor
    return roundedValue.toString()
}

private val emojiMap = mapOf(

            "add" to "+",
            "close" to "x",
            "back" to "<",
            "right" to "—>"

)



class EmojiPainter(private val emoji: String) : ImagePainter {
    @Composable
    override fun Draw(contentDescription: String?) {
        Text(text = emoji,
            style = TextStyle(
            fontSize = 24.sp,
            fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
        )


    }
}

@Composable
actual fun loadVectorPainter(name: String): ImagePainter {
    val emoji = emojiMap[name] ?: "?"
    return EmojiPainter(emoji)
}




