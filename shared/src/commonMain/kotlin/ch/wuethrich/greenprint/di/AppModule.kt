package ch.wuethrich.greenprint.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(additionalModules: List<Module> = emptyList()) {
    startKoin {
        modules(listOf(networkModule, viewModelModule) + additionalModules)
    }
}
