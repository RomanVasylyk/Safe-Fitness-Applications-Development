package com.example.safefitness.helpers

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionManager(private val activity: Activity) {
    fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.BODY_SENSORS)
        }
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), 0)
        }
    }
}
