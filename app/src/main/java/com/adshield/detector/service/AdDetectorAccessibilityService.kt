package com.adshield.detector.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.adshield.detector.data.AdStatsRepository
import com.adshield.detector.util.OverlayPermissionScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The detection + block engine.
 *
 * How it works:
 *  1. Android notifies this service on window changes system-wide (this is the
 *     only supported way for a non-root app to observe windows it doesn't own).
 *  2. For each active window we score it with heuristics (below) to guess whether
 *     it's an unwanted ad/overlay vs. a normal app window.
 *  3. High-confidence hits get an automatic dismiss action (BACK, falling back to
 *     HOME if the window survives), and every hit is logged locally for the
 *     Statistics screen — never transmitted anywhere.
 *
 * Known, honest limitations (see chat explanation): this cannot see inside a
 * window's normally-rendered in-app ad content, and some ad SDKs intentionally
 * swallow the back button, in which case only HOME can escape it.
 */
class AdDetectorAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: AdStatsRepository

    // Track recent (packageName -> list of timestamps) to score "burstiness".
    private val recentWindowTimestamps = HashMap<String, MutableList<Long>>()
    // Debounce: avoid re-acting on the exact same window within a short interval.
    private var lastActedWindowSignature: String? = null
    private var lastActedAtMillis: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = AdStatsRepository.getInstance(applicationContext)

        // Belt-and-suspenders: ensure runtime config matches the XML (some OEMs
        // strip flags from the XML at parse time).
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val currentWindows = windows ?: return

        for (window in currentWindows) {
            evaluateWindow(window)
        }
    }

    private fun evaluateWindow(window: AccessibilityWindowInfo) {
        val root: AccessibilityNodeInfo = window.root ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg == packageName) return // never act on ourselves

        val isOverlayType = window.type == AccessibilityWindowInfo.TYPE_APPLICATION_OVERLAY ||
            window.type == AccessibilityWindowInfo.TYPE_SYSTEM

        val now = System.currentTimeMillis()
        val timestamps = recentWindowTimestamps.getOrPut(pkg) { mutableListOf() }
        timestamps.add(now)
        timestamps.removeAll { now - it > BURST_WINDOW_MS }
        val burstCount = timestamps.size

        val score = heuristicScore(window, root, isOverlayType, burstCount)
        val windowSignature = "$pkg:${window.id}:${window.type}"

        if (score >= LOG_THRESHOLD) {
            val action = decideAndPerformAction(score, windowSignature, now)
            scope.launch {
                repository.recordEvent(
                    packageName = pkg,
                    heuristicScore = score,
                    windowType = windowTypeName(window.type),
                    wasWhileAppInBackground = isOverlayType,
                    actionTaken = action
                )
            }
        }
    }

    /**
     * Heuristic scoring — no single signal is proof of an ad, so we combine several
     * weighted, explainable signals rather than trying to "know" what an ad looks like.
     */
    private fun heuristicScore(
        window: AccessibilityWindowInfo,
        root: AccessibilityNodeInfo,
        isOverlayType: Boolean,
        burstCount: Int
    ): Int {
        var score = 0

        if (isOverlayType) score += 3
        if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION_OVERLAY && !window.isFocused) score += 1
        if (!hasVisibleCloseAffordance(root)) score += 2
        if (containsKnownAdSignature(root)) score += 4
        if (isFullScreenNoNav(root)) score += 1
        if (burstCount >= BURST_COUNT_THRESHOLD) score += 3

        val pkg = root.packageName?.toString().orEmpty()
        if (OverlayPermissionScanner.isLikelyKnownSafe(pkg)) score -= 3

        return score.coerceAtLeast(0)
    }

    private fun hasVisibleCloseAffordance(root: AccessibilityNodeInfo): Boolean {
        return try {
            val candidates = listOf("close", "skip", "dismiss", "x", "no thanks", "cancel")
            candidates.any { keyword ->
                root.findAccessibilityNodeInfosByText(keyword)?.isNotEmpty() == true
            }
        } catch (_: Exception) {
            true // fail open: don't penalize on inspection failure
        }
    }

    private fun containsKnownAdSignature(root: AccessibilityNodeInfo): Boolean {
        val knownIds = listOf(
            "com.google.android.gms.ads",
            "adcolony", "mopub", "vungle", "unityads", "applovin",
            "ironsource", "chartboost", "adcolony", "tapjoy", "startapp"
        )
        return try {
            val idName = root.viewIdResourceName ?: return false
            knownIds.any { idName.contains(it, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isFullScreenNoNav(root: AccessibilityNodeInfo): Boolean {
        val rect = android.graphics.Rect()
        root.getBoundsInScreen(rect)
        val metrics = resources.displayMetrics
        val coversScreen = rect.width() >= metrics.widthPixels * 0.9 &&
            rect.height() >= metrics.heightPixels * 0.75
        return coversScreen
    }

    /**
     * High-score windows get an automatic dismiss attempt. We try BACK first
     * (least disruptive), and only fall back to HOME if the same window keeps
     * reappearing — some ad overlays intentionally swallow BACK.
     */
    private fun decideAndPerformAction(score: Int, signature: String, now: Long): String {
        if (score < ACTION_THRESHOLD) return "logged_only"

        val recentlyActedSameWindow = signature == lastActedWindowSignature &&
            (now - lastActedAtMillis) < REACT_DEBOUNCE_MS
        if (recentlyActedSameWindow) return "logged_only"

        lastActedWindowSignature = signature
        lastActedAtMillis = now

        val persistent = (recentWindowTimestamps.values.flatten().count { now - it < BURST_WINDOW_MS }) > BURST_COUNT_THRESHOLD

        return if (persistent) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            "dismissed_home"
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
            "dismissed_back"
        }
    }

    private fun windowTypeName(type: Int): String = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "APPLICATION"
        AccessibilityWindowInfo.TYPE_APPLICATION_OVERLAY -> "APPLICATION_OVERLAY"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "SYSTEM"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "INPUT_METHOD"
        else -> "OTHER"
    }

    override fun onInterrupt() {}

    companion object {
        private const val LOG_THRESHOLD = 4
        private const val ACTION_THRESHOLD = 6
        private const val BURST_WINDOW_MS = 60_000L
        private const val BURST_COUNT_THRESHOLD = 3
        private const val REACT_DEBOUNCE_MS = 4_000L
    }
}
