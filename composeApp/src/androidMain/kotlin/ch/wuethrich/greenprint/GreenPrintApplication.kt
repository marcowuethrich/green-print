package ch.wuethrich.greenprint

import android.app.Application
import ch.wuethrich.greenprint.di.initKoin

class GreenPrintApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
