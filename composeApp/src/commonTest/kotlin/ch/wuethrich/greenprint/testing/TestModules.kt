package ch.wuethrich.greenprint.testing

import ch.wuethrich.greenprint.db.GreenPrintDbQueries
import ch.wuethrich.greenprint.plant.PlantViewModel
import ch.wuethrich.greenprint.repository.PlantRepository
import ch.wuethrich.greenprint.repository.RoomRepository
import ch.wuethrich.greenprint.repository.SpaceRepository
import ch.wuethrich.greenprint.space.SpaceViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Default Koin module for screen tests: wires the four faked dependencies and the ViewModels
 * that screens may resolve. Tests that need different behavior should pass already-configured
 * fake instances; tests that need additional bindings can pass `extraModules` to `ScreenTest`.
 */
fun testAppModule(
    spaceRepository: SpaceRepository,
    roomRepository: RoomRepository,
    plantRepository: PlantRepository,
    queries: GreenPrintDbQueries,
): Module = module {
    single<GreenPrintDbQueries> { queries }
    single<SpaceRepository> { spaceRepository }
    single<RoomRepository> { roomRepository }
    single<PlantRepository> { plantRepository }
    viewModel { SpaceViewModel(get(), get(), get(), get()) }
    viewModel { PlantViewModel(get()) }
}
