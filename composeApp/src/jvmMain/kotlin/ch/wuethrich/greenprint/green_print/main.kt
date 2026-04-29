package ch.wuethrich.greenprint.green_print

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ch.wuethrich.greenprint.green_print.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "green_print",
        ) {
            App()
        }
    }
}
