package ch.wuethrich.greenprint.plant.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlantFormModal(
    plant: Plant? = null,
    onConfirm: (name: String, species: String?, intervalDays: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var nameValue by remember(plant) {
        mutableStateOf(
            TextFieldValue(
                text = plant?.name ?: "",
                selection = if (plant != null) TextRange(0, plant.name.length) else TextRange.Zero
            )
        )
    }
    var speciesValue by remember(plant) {
        mutableStateOf(
            TextFieldValue(
                text = plant?.species ?: "",
                selection = TextRange(plant?.species?.length ?: 0)
            )
        )
    }
    var intervalDays by remember(plant) { mutableStateOf(plant?.wateringIntervalDays?.toString() ?: "7") }

    val initialName = plant?.name ?: ""
    val initialSpecies = plant?.species ?: ""
    val initialInterval = plant?.wateringIntervalDays?.toString() ?: "7"
    
    val isModified = nameValue.text != initialName || speciesValue.text != initialSpecies || intervalDays != initialInterval
    val canSave = nameValue.text.isNotBlank() && intervalDays.isNotBlank() && (plant == null || isModified)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = stringResource(if (plant == null) Res.string.plant_add_title else Res.string.plant_edit_title), 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    label = { Text(stringResource(Res.string.plant_name_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focusRequester)
                )
                OutlinedTextField(
                    value = speciesValue,
                    onValueChange = { speciesValue = it },
                    label = { Text(stringResource(Res.string.plant_species_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = intervalDays,
                    onValueChange = { if (it.all { char -> char.isDigit() }) intervalDays = it },
                    label = { Text(stringResource(Res.string.plant_interval_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        val current = intervalDays.toIntOrNull() ?: 0
                                        intervalDays = (current + 1).toString()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        val current = intervalDays.toIntOrNull() ?: 0
                                        if (current > 1) {
                                            intervalDays = (current - 1).toString()
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystem.Space2)) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.common_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        onConfirm(
                            nameValue.text, 
                            speciesValue.text.takeIf { it.isNotBlank() }, 
                            intervalDays.toIntOrNull() ?: 7
                        )
                    },
                    enabled = canSave,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(Res.string.common_save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = null
    )
}