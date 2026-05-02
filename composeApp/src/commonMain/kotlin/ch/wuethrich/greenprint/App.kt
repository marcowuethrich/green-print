package ch.wuethrich.greenprint

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.wuethrich.greenprint.space.SpaceScreen
import ch.wuethrich.greenprint.ui.theme.GreenPrintTheme
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    GreenPrintTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                )
            }
        ) { innerPadding ->
            SpaceScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
