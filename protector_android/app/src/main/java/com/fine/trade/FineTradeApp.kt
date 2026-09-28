package com.fine.trade

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Ensures consistent UI behaviour across API 26–36.
 */
class FineTradeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Never follow forced dark mode that breaks our light Material theme on OEM skins.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
