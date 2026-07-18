package com.adshield.detector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adshield.detector.ui.AdShieldViewModel
import com.adshield.detector.ui.theme.SurfaceDarkAlt
import com.adshield.detector.util.AppInfoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: AdShieldViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val label = remember(packageName) { AppInfoHelper.labelFor(context, packageName) }
    val events by viewModel.eventsFor(packageName).collectAsState()
    val allowlist by viewModel.allowlist.collectAsState()
    val isAllowlisted = allowlist.any { it.packageName == packageName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { AppInfoHelper.openAppSettings(context, packageName) }) {
                        Text("App info")
                    }
                    OutlinedButton(onClick = { AppInfoHelper.openOverlaySettings(context, packageName) }) {
                        Text("Overlay permission")
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (isAllowlisted) {
                    val entry = allowlist.first { it.packageName == packageName }
                    OutlinedButton(onClick = { viewModel.removeFromAllowlist(entry) }) {
                        Text("Remove from trusted list")
                    }
                } else {
                    OutlinedButton(onClick = { viewModel.addToAllowlist(packageName) }) {
                        Text("Mark as trusted (stop flagging this app)")
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Detection history (${events.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
            }

            items(events) { event ->
                val time = remember(event.timestampMillis) {
                    SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(Date(event.timestampMillis))
                }
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(time, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Window: ${event.windowType} · Score: ${event.heuristicScore} · Action: ${event.actionTaken}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        "No detection history for this app yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
