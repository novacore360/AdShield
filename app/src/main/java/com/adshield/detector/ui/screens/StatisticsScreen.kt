package com.adshield.detector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adshield.detector.data.AdEvent
import com.adshield.detector.ui.AdShieldViewModel
import com.adshield.detector.ui.theme.BrandDanger
import com.adshield.detector.ui.theme.BrandSafe
import com.adshield.detector.ui.theme.BrandWarning
import com.adshield.detector.ui.theme.SurfaceDarkAlt
import com.adshield.detector.util.AppInfoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: AdShieldViewModel,
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val events by viewModel.recentEvents.collectAsState()
    val eventsToday by viewModel.eventsToday.collectAsState()
    val events7Days by viewModel.events7Days.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(Modifier.weight(1f), "Today", eventsToday.toString())
                    SummaryTile(Modifier.weight(1f), "7 days", events7Days.toString())
                    SummaryTile(Modifier.weight(1f), "Stored", events.size.toString())
                }
                Spacer(Modifier.height(12.dp))
                Text("Recent activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            if (events.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandSafe, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No pop-ups detected yet", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(events) { event ->
                EventRow(event, onClick = { onOpenApp(event.packageName) })
            }
        }
    }
}

@Composable
private fun SummaryTile(modifier: Modifier, label: String, value: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventRow(event: AdEvent, onClick: () -> Unit) {
    val context = LocalContext.current
    val label = remember(event.packageName) { AppInfoHelper.labelFor(context, event.packageName) }
    val time = remember(event.timestampMillis) {
        SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(Date(event.timestampMillis))
    }
    val actionColor = when (event.actionTaken) {
        "dismissed_back", "dismissed_home" -> BrandSafe
        "logged_only" -> BrandWarning
        else -> BrandDanger
    }
    val actionLabel = when (event.actionTaken) {
        "dismissed_back" -> "Blocked"
        "dismissed_home" -> "Blocked (forced)"
        "logged_only" -> "Logged"
        else -> event.actionTaken
    }

    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(actionColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, color = actionColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
