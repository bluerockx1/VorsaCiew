package com.vorsaciew.app

import android.app.Application
import android.preference.PreferenceManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class VorsaCiewApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // OSMDroid configuration — must run before any MapView is created
        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, PreferenceManager.getDefaultSharedPreferences(this))
        osmConfig.userAgentValue = packageName
    }
}
