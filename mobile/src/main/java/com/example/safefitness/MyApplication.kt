package com.example.safefitness

import android.app.Application
import android.content.res.Configuration
import java.util.Locale

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setLocale(Locale.getDefault())
    }

    fun setLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
