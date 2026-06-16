package com.siaka

import android.app.Application
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SiakaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
