package com.adshield.detector

import android.app.Application
import com.adshield.detector.data.AppDatabase

class AdShieldApplication : Application() {

    // Single Room instance for the whole app lifecycle. Local-only, on-device.
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
