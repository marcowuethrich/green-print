package ch.wuethrich.greenprint

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import ch.wuethrich.greenprint.db.DriverFactory
import ch.wuethrich.greenprint.db.openWebCache
import ch.wuethrich.greenprint.di.initKoin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.w3c.dom.Worker

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // The browser cache's schema is created asynchronously (ADR-017), so the app starts once it is open.
    MainScope().launch {
        val driverFactory = openCache()
        initKoin {
            modules(module { single { driverFactory } })
        }
        ComposeViewport {
            App()
        }
    }
}

// A saved cache the app cannot start on (one it cannot read or migrate) would leave the page blank on
// every visit, so it is discarded once and the app starts on an empty cache.
private suspend fun openCache(): DriverFactory {
    val worker = cacheWorker(discardSaved = false)
    return try {
        openWebCache(worker)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // The driver reports any failure of the worker as an Exception; a cancelled start stays cancelled.
        currentCoroutineContext().ensureActive()
        worker.terminate()
        println("GreenPrint: the app could not open the saved local cache: ${e.message}")
        openWebCache(cacheWorker(discardSaved = true))
    }
}

// The worker's first message says whether it opens the saved copy or starts empty (greenprint-db.worker.js).
@OptIn(ExperimentalWasmJsInterop::class)
private fun cacheWorker(discardSaved: Boolean): Worker =
    newCacheWorker().also { it.postMessage(openMessage(discardSaved)) }

// webpack bundles the worker (src/webMain/resources) with sql.js, found through this URL.
@OptIn(ExperimentalWasmJsInterop::class)
private fun newCacheWorker(): Worker = js("""new Worker(new URL("./greenprint-db.worker.js", import.meta.url))""")

// detekt cannot see a parameter used inside js().
@Suppress("UnusedParameter")
@OptIn(ExperimentalWasmJsInterop::class)
private fun openMessage(discardSaved: Boolean): JsAny = js("""({ action: "open", discardSaved: discardSaved })""")
