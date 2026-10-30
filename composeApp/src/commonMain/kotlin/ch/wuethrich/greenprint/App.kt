package ch.wuethrich.greenprint

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import ch.wuethrich.greenprint.ui.theme.GreenPrintTheme
import ch.wuethrich.greenprint.welcome.WelcomeScreen

@Composable
fun App() {
    GreenPrintTheme {
        Navigator(WelcomeScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
