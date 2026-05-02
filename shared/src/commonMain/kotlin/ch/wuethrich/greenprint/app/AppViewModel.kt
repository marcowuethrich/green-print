package ch.wuethrich.greenprint.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wuethrich.greenprint.repository.AuthRepository
import kotlinx.coroutines.launch

class AppViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            authRepository.login()
        }
    }
}
