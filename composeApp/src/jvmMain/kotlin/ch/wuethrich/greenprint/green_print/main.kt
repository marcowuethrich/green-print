package ch.wuethrich.greenprint.green_print

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "green_print",
    ) {
        App()
    }
}