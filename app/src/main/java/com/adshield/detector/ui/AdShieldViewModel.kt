package com.adshield.detector.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adshield.detector.data.AdEvent
import com.adshield.detector.data.AdStatsRepository
import com.adshield.detector.data.AllowlistEntry
import com.adshield.detector.data.PackageCount
import com.adshield.detector.util.AccessibilityStatus
import com.adshield.detector.util.OverlayPermissionScanner
import com.adshield.detector.util.SuspectAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdStatsRepository.getInstance(application)

    val recentEvents: StateFlow<List<AdEvent>> = repository.recentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rankedSuspects: StateFlow<List<PackageCount>> = repository.rankedSuspects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventsToday: StateFlow<Int> = repository.eventCountToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val events7Days: StateFlow<Int> = repository.eventCountLast7Days()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allowlist: StateFlow<List<AllowlistEntry>> = repository.allowlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _overlayApps = MutableStateFlow<List<SuspectAppInfo>>(emptyList())
    val overlayApps: StateFlow<List<SuspectAppInfo>> = _overlayApps.asStateFlow()

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    fun refreshPermissionScan() {
        _overlayApps.value = OverlayPermissionScanner.getAppsWithOverlayPermission(getApplication())
    }

    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = AccessibilityStatus.isServiceEnabled(getApplication())
    }

    fun eventsFor(pkg: String) = repository.eventsForPackage(pkg)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToAllowlist(pkg: String) = viewModelScope.launch { repository.addToAllowlist(pkg) }

    fun removeFromAllowlist(entry: AllowlistEntry) = viewModelScope.launch {
        repository.removeFromAllowlist(entry.packageName, entry.addedAtMillis)
    }

    fun clearAllData() = viewModelScope.launch { repository.clearAllData() }
}
