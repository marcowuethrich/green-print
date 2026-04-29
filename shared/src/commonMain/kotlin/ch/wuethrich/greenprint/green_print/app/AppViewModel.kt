package ch.wuethrich.greenprint.green_print.app

import androidx.lifecycle.ViewModel
import ch.wuethrich.greenprint.green_print.Greeting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel : ViewModel() {
    private val _greeting = MutableStateFlow(Greeting().greet())
    val greeting: StateFlow<String> = _greeting.asStateFlow()
}
