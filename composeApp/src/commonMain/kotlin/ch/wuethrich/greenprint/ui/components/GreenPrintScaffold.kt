package ch.wuethrich.greenprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import ch.wuethrich.greenprint.ui.theme.DesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenPrintScaffold(
    title: String,
    titleIcon: Painter? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (titleIcon != null) {
                            Icon(
                                painter = titleIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(DesignSystem.Space5)
                            )
                            Spacer(modifier = Modifier.width(DesignSystem.Space2))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brush.verticalGradient(DesignSystem.BackgroundGradient))
    ) { innerPadding ->
        content(innerPadding)
    }
}
