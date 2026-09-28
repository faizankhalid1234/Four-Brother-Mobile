package com.fine.trade

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Safe window / UI helpers for API 26 through 36. */
object UiCompat {
    fun setupActivityWindow(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = true
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.teal_dark)
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = ContextCompat.getColor(activity, android.R.color.white)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            activity.window.isNavigationBarContrastEnforced = false
        }
    }

    fun setVisible(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** e.g. "Version 1.5.1 (9)" */
    fun appVersionText(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val name = info.versionName ?: "?"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return "Version $name ($code)"
    }
}
