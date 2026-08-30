package com.novatube.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Placeholder for any boot-time work. WorkManager persists state; we just wake the
        // Application class to ensure DB/prefs are ready.
        val pkg = context.packageName
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        // No-op for now; reserved for future scheduled work.
        launch?.let { /* keep reference to silence linter */ }
    }
}
