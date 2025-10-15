package org.example.process_scheduling

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.Deadlock
import ui.Home

fun main() = application {
    // Defines the initial state and constraints for the desktop window
    val windowState = rememberWindowState(
        // Set a reasonable initial size for the desktop app
        size = DpSize(1200.dp,800.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "OS Scheduling Simulator"
    ) {
        // NOTE: The previous attempt to set minWidth/minHeight here caused an error
        // because DpSize does not have those properties.
        // We removed the invalid code block to allow compilation.

        // The main content of your application

        Home()

    }
}
