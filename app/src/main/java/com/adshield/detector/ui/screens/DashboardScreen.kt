package com.adshield.detector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adshield.detector.ui.AdShieldViewModel
import com.adshield.detector.ui.theme.BrandDanger
import com.adshield.detector.ui.theme.BrandSafe
import com.adshield.detector.ui.theme.SurfaceDarkAlt
import com.adshield.detector.util.AccessibilityStatus
import com.adshield.detector.util.AppInfoHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AdShieldViewModel,
    onOpenStatistics: () -> Unit,
    onOpenSuspects: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val enabled by viewModel.accessibilityEnabled.collectAsState()
    val eventsToday by viewModel.eventsToday.collectAsState()
    val events7Days by viewModel.events7Days.collectAsState()
    val ranked by viewModel.rankedSuspects.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AdShield", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProtectionStatusCard(
                enabled = enabled,
                onEnableClick = { AccessibilityStatus.openAccessibilitySettings(context) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Blocked today",
                    value = eventsToday.toString()
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Last 7 days",
                    value = events7Days.toString()
                )
            }

            if (ranked.isNotEmpty()) {
                Text("Top suspect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val top = ranked.first()
                TopSuspectCard(
                    packageName = top.packageName,
                    label = AppInfoHelper.labelFor(context, top.packageName),
                    count = top.count,
                    onClick = onOpenSuspects
                )
            }

            Card(
                onClick = onOpenSuspects,
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Suspect apps", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Apps that can draw over your screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            }

            Card(
                onClick = onOpenStatistics,
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Statistics", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Full local history of detected pop-ups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.BarChart, contentDescription = null)
                }
            }

            Text(
                "All detection happens on this device. AdShield has no internet permission — nothing is ever uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ProtectionStatusCard(enabled: Boolean, onEnableClick: () -> Unit) {
    val color = if (enabled) BrandSafe else BrandDanger
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (enabled) "Protection active" else "Protection off",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (enabled) "Monitoring your screen for unwanted pop-ups"
                    else "Turn on the Accessibility service to start blocking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!enabled) {
                Button(onClick = onEnableClick) { Text("Enable") }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopSuspectCard(packageName: String, label: String, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF5A623))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$count events", fontWeight = FontWeight.Bold)
        }
    }
}
