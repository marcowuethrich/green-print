package ch.wuethrich.greenprint.di

import ch.wuethrich.greenprint.db.GreenPrintDb
import ch.wuethrich.greenprint.db.createDatabase
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val databaseModule = module {
    single { createDatabase(get()) }
    single { get<GreenPrintDb>().greenPrintDbQueries }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, databaseModule, networkModule, viewModelModule)
    }
}
