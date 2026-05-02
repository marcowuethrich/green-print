package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.db.DriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory(get()) }
}
