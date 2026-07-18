package com.adshield.detector.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android restarts enabled AccessibilityServices automatically after boot;
 * this receiver exists only as a hook for future maintenance tasks
 * (e.g. pruning old local records) and performs no network activity.
 */
class BootRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // No-op today. Reserved for local maintenance (e.g. DB pruning) on boot.
        }
    }
}
