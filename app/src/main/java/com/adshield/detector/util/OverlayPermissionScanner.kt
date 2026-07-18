package com.adshield.detector.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process

data class SuspectAppInfo(
    val packageName: String,
    val label: String,
    val hasOverlayPermission: Boolean,
    val isSystemApp: Boolean,
    val installTimeMillis: Long
)

/**
 * A small, ship-with-the-app allowlist of well-known package prefixes that
 * legitimately use overlay permission (launchers, accessibility tools, etc.),
 * used only to reduce false positives in the "who might be causing this"
 * ranking — not to hide anything from the user, who can always see the full list.
 */
private val KNOWN_SAFE_PREFIXES = listOf(
    "com.google.android.",
    "com.android.",
    "com.samsung.android.",
    "com.miui.",
    "com.oneplus.",
    "com.sonyericsson.",
    "com.htc."
)

object OverlayPermissionScanner {

    fun getAppsWithOverlayPermission(context: Context): List<SuspectAppInfo> {
        val pm = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages.mapNotNull { appInfo ->
            if (appInfo.packageName == context.packageName) return@mapNotNull null

            val mode = try {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    appInfo.uid,
                    appInfo.packageName
                )
            } catch (_: Exception) {
                AppOpsManager.MODE_DEFAULT
            }

            val hasOverlay = mode == AppOpsManager.MODE_ALLOWED
            if (!hasOverlay) return@mapNotNull null

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                appInfo.packageName
            }
            val installTime = try {
                pm.getPackageInfo(appInfo.packageName, 0).firstInstallTime
            } catch (_: Exception) {
                0L
            }

            SuspectAppInfo(
                packageName = appInfo.packageName,
                label = label,
                hasOverlayPermission = true,
                isSystemApp = isSystem,
                installTimeMillis = installTime
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun isLikelyKnownSafe(packageName: String): Boolean =
        KNOWN_SAFE_PREFIXES.any { packageName.startsWith(it) }
}
