package org.example.process_scheduling

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import ui.Home

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        Home()
    }
}