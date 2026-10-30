package ch.wuethrich.greenprint

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import ch.wuethrich.greenprint.db.openWebCache
import ch.wuethrich.greenprint.di.initKoin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.w3c.dom.Worker

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // The browser cache's schema is created asynchronously (ADR-017), so the app starts once it is open.
    MainScope().launch {
        val driverFactory = openWebCache(cacheWorker())
        initKoin {
            modules(module { single { driverFactory } })
        }
        ComposeViewport {
            App()
        }
    }
}

// webpack bundles the worker (src/webMain/resources) with sql.js, found through this URL.
private fun cacheWorker(): Worker = js("""new Worker(new URL("./greenprint-db.worker.js", import.meta.url))""")
