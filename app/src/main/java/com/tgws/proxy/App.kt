package com.tgws.proxy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule the periodic GitHub release check.
        UpdateWorker.schedule(this)
    }
}
