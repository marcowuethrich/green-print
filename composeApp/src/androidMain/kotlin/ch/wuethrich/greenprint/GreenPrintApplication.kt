package ch.wuethrich.greenprint

import android.app.Application
import ch.wuethrich.greenprint.di.initKoin
import org.koin.android.ext.koin.androidContext

class GreenPrintApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GreenPrintApplication)
        }
    }
}
