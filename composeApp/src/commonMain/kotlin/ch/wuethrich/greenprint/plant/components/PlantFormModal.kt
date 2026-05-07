package ch.wuethrich.greenprint.plant.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Plant
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlantFormModal(
    plant: Plant? = null,
    onConfirm: (name: String, species: String?, intervalDays: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(plant?.name ?: "") }
    var species by remember { mutableStateOf(plant?.species ?: "") }
    var intervalDays by remember { mutableStateOf(plant?.wateringIntervalDays?.toString() ?: "7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(if (plant == null) Res.string.plant_add_title else Res.string.plant_edit_title), 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.plant_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text(stringResource(Res.string.plant_species_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = intervalDays,
                    onValueChange = { if (it.all { char -> char.isDigit() }) intervalDays = it },
                    label = { Text(stringResource(Res.string.plant_interval_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, species.takeIf { it.isNotBlank() }, intervalDays.toIntOrNull() ?: 7)
                },
                enabled = name.isNotBlank() && intervalDays.isNotBlank()
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}
