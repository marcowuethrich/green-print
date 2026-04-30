package ch.wuethrich.greenprint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(Res.string.app_name))
        }
    }
}
