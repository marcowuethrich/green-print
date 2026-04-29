package ch.wuethrich.greenprint

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ch.wuethrich.greenprint.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GreenPrint",
        ) {
            App()
        }
    }
}
