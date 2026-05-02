package ch.wuethrich.greenprint

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.space.SpaceScreen
import ch.wuethrich.greenprint.ui.theme.GreenPrintTheme
import ch.wuethrich.greenprint.welcome.WelcomeScreen
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

sealed interface Screen {
    data object Welcome : Screen
    data class SpaceDetail(val space: Space) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    GreenPrintTheme {
        Scaffold { innerPadding ->
            when (val screen = currentScreen) {
                Screen.Welcome -> WelcomeScreen(
                    onEnterSpace = { currentScreen = Screen.SpaceDetail(it) },
                    modifier = Modifier.padding(innerPadding)
                )
                is Screen.SpaceDetail -> SpaceScreen(
                    onLeave = { currentScreen = Screen.Welcome },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
