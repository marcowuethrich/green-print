package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.app.AppViewModel
import ch.wuethrich.greenprint.space.SpaceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AppViewModel(get()) }
    viewModel { SpaceViewModel(get(), get()) }
}
