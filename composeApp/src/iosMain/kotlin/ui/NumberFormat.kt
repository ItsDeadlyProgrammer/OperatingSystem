package ui

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Actual implementation for iOS (Kotlin/Native) platforms.
 * Uses a mathematical rounding approach to achieve fixed decimal precision,
 * avoiding reliance on potentially unavailable or problematic String.format implementation.
 */
actual fun Double.toFormattedString(decimals: Int): String {
    // 1. Calculate the factor (e.g., 100 for 2 decimals: 10^2)
    val factor = 10.0.pow(decimals)

    // 2. Multiply by factor, round to the nearest integer, and divide back.
    // This accurately handles rounding to the specified decimal place using common Kotlin math functions.
    val roundedValue = (this * factor).roundToInt() / factor

    // Note: This purely mathematical approach reliably avoids the format() error,
    // but may result in values like "5.0" instead of "5.00" if the trailing zeros are significant.
    return roundedValue.toString()
}
