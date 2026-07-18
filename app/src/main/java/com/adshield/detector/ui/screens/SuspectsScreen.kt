package com.adshield.detector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adshield.detector.data.PackageCount
import com.adshield.detector.ui.AdShieldViewModel
import com.adshield.detector.ui.theme.BrandDanger
import com.adshield.detector.ui.theme.BrandSafe
import com.adshield.detector.ui.theme.BrandWarning
import com.adshield.detector.ui.theme.SurfaceDarkAlt
import com.adshield.detector.util.AppInfoHelper
import com.adshield.detector.util.SuspectAppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspectsScreen(
    viewModel: AdShieldViewModel,
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val ranked by viewModel.rankedSuspects.collectAsState()
    val overlayApps by viewModel.overlayApps.collectAsState()

    val rankedPackages = ranked.map { it.packageName }.toSet()
    val overlayOnly = overlayApps.filter { it.packageName !in rankedPackages }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suspect apps") },
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
            if (ranked.isNotEmpty()) {
                item {
                    Text(
                        "Detected showing pop-ups (last 7 days)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(ranked) { suspect ->
                    RankedSuspectRow(context, suspect, onClick = { onOpenApp(suspect.packageName) })
                }
            }

            if (overlayOnly.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Other apps that can draw over your screen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "No pop-ups detected from these yet, but they hold the permission needed to show one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(overlayOnly) { app ->
                    OverlayAppRow(app, onClick = { onOpenApp(app.packageName) })
                }
            }

            if (ranked.isEmpty() && overlayOnly.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = BrandSafe, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing suspicious found", fontWeight = FontWeight.SemiBold)
                        Text(
                            "No apps currently hold overlay permission, and no pop-ups have been detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankedSuspectRow(context: android.content.Context, suspect: PackageCount, onClick: () -> Unit) {
    val label = remember(suspect.packageName) { AppInfoHelper.labelFor(context, suspect.packageName) }
    val severityColor = when {
        suspect.count >= 10 -> BrandDanger
        suspect.count >= 3 -> BrandWarning
        else -> BrandSafe
    }
    val lastSeen = remember(suspect.lastSeen) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(suspect.lastSeen))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(context, suspect.packageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(
                    "Last seen $lastSeen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(severityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("${suspect.count}", color = severityColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OverlayAppRow(app: SuspectAppInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(context, app.packageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppIcon(context: android.content.Context, packageName: String) {
    val icon = remember(packageName) { AppInfoHelper.iconFor(context, packageName) }
    val bitmap = remember(icon) { icon?.let { drawableToBitmap(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) return drawable.bitmap
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
