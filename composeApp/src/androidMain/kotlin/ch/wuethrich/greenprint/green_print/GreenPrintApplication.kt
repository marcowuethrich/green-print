package ch.wuethrich.greenprint.green_print

import android.app.Application
import ch.wuethrich.greenprint.green_print.di.initKoin

class GreenPrintApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
