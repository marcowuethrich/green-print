package ch.wuethrich.greenprint.green_print.di

import ch.wuethrich.greenprint.green_print.app.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AppViewModel() }
}
