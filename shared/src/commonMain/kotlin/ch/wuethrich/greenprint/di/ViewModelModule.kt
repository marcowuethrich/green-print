package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.app.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AppViewModel(get()) }
}
