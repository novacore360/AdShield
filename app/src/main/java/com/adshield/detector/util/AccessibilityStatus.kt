package com.adshield.detector.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.adshield.detector.service.AdDetectorAccessibilityService

object AccessibilityStatus {

    fun isServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${AdDetectorAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        if (TextUtils.isEmpty(enabledServices)) return false

        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
