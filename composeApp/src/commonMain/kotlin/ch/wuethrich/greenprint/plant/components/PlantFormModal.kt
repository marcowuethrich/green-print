package ch.wuethrich.greenprint.plant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import ch.wuethrich.greenprint.model.Plant
import ch.wuethrich.greenprint.ui.components.CancelSaveButtons
import ch.wuethrich.greenprint.ui.components.DialogTitle
import ch.wuethrich.greenprint.ui.components.RequestInitialFocus
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.plant_add_title
import greenprint.composeapp.generated.resources.plant_edit_title
import greenprint.composeapp.generated.resources.plant_interval_label
import greenprint.composeapp.generated.resources.plant_name_label
import greenprint.composeapp.generated.resources.plant_species_label
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_INTERVAL_DAYS = 7

/** The form's fields, seeded from the plant being edited or the defaults for a new one. */
@Stable
private class PlantFormState(private val plant: Plant?) {
    private val initialName = plant?.name.orEmpty()
    private val initialSpecies = plant?.species.orEmpty()
    private val initialInterval = (plant?.wateringIntervalDays ?: DEFAULT_INTERVAL_DAYS).toString()

    var name by mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length)))
    var species by mutableStateOf(TextFieldValue(initialSpecies, TextRange(initialSpecies.length)))
    var intervalDays by mutableStateOf(initialInterval)

    val canSave: Boolean
        get() {
            val isModified =
                name.text != initialName || species.text != initialSpecies || intervalDays != initialInterval
            return name.text.isNotBlank() && intervalDays.isNotBlank() && (plant == null || isModified)
        }

    val speciesOrNull: String? get() = species.text.takeIf { it.isNotBlank() }

    val intervalOrDefault: Int get() = intervalDays.toIntOrNull() ?: DEFAULT_INTERVAL_DAYS
}

/** Adds a plant, or edits [plant] when one is given. */
@Composable
fun PlantFormModal(
    onConfirm: (name: String, species: String?, intervalDays: Int) -> Unit,
    onDismiss: () -> Unit,
    plant: Plant? = null,
) {
    val form = remember(plant) { PlantFormState(plant) }
    val focusRequester = remember { FocusRequester() }
    RequestInitialFocus(focusRequester)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            DialogTitle(stringResource(if (plant == null) Res.string.plant_add_title else Res.string.plant_edit_title))
        },
        text = { PlantFormFields(form = form, focusRequester = focusRequester) },
        confirmButton = {
            CancelSaveButtons(
                onCancel = onDismiss,
                onSave = { onConfirm(form.name.text, form.speciesOrNull, form.intervalOrDefault) },
                saveEnabled = form.canSave,
            )
        },
        dismissButton = null,
    )
}

@Composable
private fun PlantFormFields(form: PlantFormState, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4), modifier = modifier) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { form.name = it },
            label = { Text(stringResource(Res.string.plant_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignSystem.Space2)
                .focusRequester(focusRequester),
        )
        OutlinedTextField(
            value = form.species,
            onValueChange = { form.species = it },
            label = { Text(stringResource(Res.string.plant_species_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.intervalDays,
            onValueChange = { if (it.all(Char::isDigit)) form.intervalDays = it },
            label = { Text(stringResource(Res.string.plant_interval_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .stepDaysWithArrowKeys(form.intervalDays) { form.intervalDays = it },
        )
    }
}

/** Up and Down step the interval by a day (ADR-013); Down stops at one. */
private fun Modifier.stepDaysWithArrowKeys(days: String, onDaysChange: (String) -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        val current = days.toIntOrNull() ?: 0
        val handled = event.type == KeyEventType.KeyDown &&
            (event.key == Key.DirectionUp || event.key == Key.DirectionDown)
        if (handled) {
            when {
                event.key == Key.DirectionUp -> onDaysChange((current + 1).toString())
                current > 1 -> onDaysChange((current - 1).toString())
            }
        }
        handled
    }
