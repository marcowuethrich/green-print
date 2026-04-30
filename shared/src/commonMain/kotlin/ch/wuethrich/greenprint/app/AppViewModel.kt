package ch.wuethrich.greenprint.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.repository.GreetingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val greetingRepository: GreetingRepository
) : ViewModel() {
    private val _greeting = MutableStateFlow("Loading...")
    val greeting: StateFlow<String> = _greeting.asStateFlow()

    init {
        updateGreeting()
    }

    fun updateGreeting() {
        viewModelScope.launch {
            try {
                val response = greetingRepository.getGreeting()
                _greeting.value = response.message
            } catch (e: Exception) {
                _greeting.value = "Error: ${e.message}"
            }
        }
    }
}
