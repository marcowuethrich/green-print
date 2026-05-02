package ch.wuethrich.greenprint

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import ch.wuethrich.greenprint.ui.theme.GreenPrintTheme
import ch.wuethrich.greenprint.welcome.WelcomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    GreenPrintTheme {
        Navigator(WelcomeScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
