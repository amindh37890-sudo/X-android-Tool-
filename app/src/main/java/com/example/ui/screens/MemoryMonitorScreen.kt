package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.MemorySnapshot
import com.example.data.engine.ProcMemItem
import com.example.ui.components.CyberCard
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MemoryMonitorScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTerminalWithCommand: ((String) -> Unit)? = null
) {
    val snapshot by viewModel.memorySnapshot.collectAsStateWithLifecycle()
    val isAutoRefresh by viewModel.isAutoRefreshMemory.collectAsStateWithLifecycle()
    val isOptimizing by viewModel.isOptimizingMemory.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var procFilterCategory by remember { mutableStateOf("All") }
    var procSearchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Core", "Cache", "Pages", "Kernel", "Swap")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header card
            MemoryHeaderCard(
                snapshot = snapshot,
                isAutoRefresh = isAutoRefresh,
                onToggleAutoRefresh = { viewModel.toggleAutoRefreshMemory() },
                onRefreshNow = { viewModel.refreshMemorySnapshot() }
            )
        }

        item {
            // Neon Radial Gauges: RAM and JVM Heap
            MemoryGaugesCard(snapshot = snapshot)
        }

        item {
            // Real-Time Waveform Graph
            MemoryWaveformCard(historyPercents = snapshot.historyUsagePercents)
        }

        item {
            // Quick Memory Action Controls
            MemoryQuickActionsCard(
                isOptimizing = isOptimizing,
                onForceGc = { viewModel.forceGarbageCollection() },
                onTrimMemory = { viewModel.trimApplicationMemory() },
                onExportReport = {
                    scope.launch {
                        val report = viewModel.getFullMemoryReport()
                        clipboardManager.setText(AnnotatedString(report))
                    }
                },
                onInspectInTerminal = {
                    onNavigateToTerminalWithCommand?.invoke("cat /proc/meminfo")
                }
            )
        }

        item {
            // ZRAM / Swap and Low Memory Diagnostics
            SwapAndDiagnosticsCard(snapshot = snapshot)
        }

        item {
            // /proc/meminfo Kernel Telemetry Header & Filters
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = NexusCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بيانات نواة لينكس (/proc/meminfo)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                    }

                    Text(
                        text = "${snapshot.procMemItems.size} مؤشرات",
                        fontSize = 11.sp,
                        color = NexusCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = procSearchQuery,
                    onValueChange = { procSearchQuery = it },
                    placeholder = {
                        Text("بحث في سجلات الذاكرة (Mem, Swap, Cache...)", fontSize = 12.sp, color = NexusTextSecondary)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary,
                        cursorColor = NexusCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = procFilterCategory == category,
                            onClick = { procFilterCategory = category },
                            label = { Text(category, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NexusCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NexusCyan,
                                containerColor = Color.Transparent,
                                labelColor = NexusTextSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (procFilterCategory == category) NexusCyan else NexusCardBorder
                            )
                        )
                    }
                }
            }
        }

        // Proc Mem Items
        val filteredProcItems = snapshot.procMemItems.filter { item ->
            val matchesCategory = procFilterCategory == "All" || item.category.equals(procFilterCategory, ignoreCase = true)
            val matchesSearch = procSearchQuery.isBlank() ||
                    item.key.contains(procSearchQuery, ignoreCase = true) ||
                    item.labelAr.contains(procSearchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        if (filteredProcItems.isEmpty()) {
            item {
                CyberCard {
                    Text(
                        text = "لا توجد مؤشرات مطابقة للبحث",
                        color = NexusTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredProcItems) { item ->
                ProcMemItemCard(item = item)
            }
        }

        // Top Processes Memory
        if (snapshot.topProcesses.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = NexusViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أعلى العمليات استهلاكاً للذاكرة (Monitored Processes)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                }
            }

            items(snapshot.topProcesses) { proc ->
                ProcessMemoryCard(process = proc, totalRamMb = snapshot.ram.totalMb)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MemoryHeaderCard(
    snapshot: MemorySnapshot,
    isAutoRefresh: Boolean,
    onToggleAutoRefresh: () -> Unit,
    onRefreshNow: () -> Unit
) {
    CyberCard(
        borderColor = NexusCyan.copy(alpha = 0.4f),
        backgroundColor = Color(0xFF080D14)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isAutoRefresh) NexusGreen else NexusAmber, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CYBER MEMORY MONITOR // مراقب الذاكرة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = NexusCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isAutoRefresh) "تحديث فوري نشط (1.5s Live Telemetry)" else "التحديث التلقائي متوقف",
                        fontSize = 10.sp,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleAutoRefresh,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isAutoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle auto refresh",
                        tint = if (isAutoRefresh) NexusGreen else NexusAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRefreshNow,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh memory snapshot",
                        tint = NexusCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryGaugesCard(snapshot: MemorySnapshot) {
    CyberCard(
        borderColor = NexusCardBorder,
        backgroundColor = NexusSurface
    ) {
        Text(
            text = "المؤشرات الأساسية للذاكرة (Primary Memory Cores)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NexusTextSecondary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // System RAM Gauge
            RadialMemoryGauge(
                percent = snapshot.ram.usedPercent,
                title = "SYSTEM RAM",
                subtitle = "${snapshot.ram.usedMb} / ${snapshot.ram.totalMb} MB",
                color = when {
                    snapshot.ram.usedPercent > 85f -> NexusRed
                    snapshot.ram.usedPercent > 65f -> NexusAmber
                    else -> NexusCyan
                }
            )

            // JVM Heap Gauge
            RadialMemoryGauge(
                percent = snapshot.heap.usedPercent,
                title = "APP JVM HEAP",
                subtitle = "${snapshot.heap.allocatedMb} / ${snapshot.heap.maxMb} MB",
                color = NexusViolet
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress Bar for System RAM
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المساحة المتاحة: ${snapshot.ram.availMb} MB (~${String.format(Locale.US, "%.1f", snapshot.ram.availGb)} GB)",
                    fontSize = 11.sp,
                    color = NexusGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", snapshot.ram.usedPercent)}% مستهلك",
                    fontSize = 11.sp,
                    color = NexusTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (snapshot.ram.usedPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    snapshot.ram.usedPercent > 85f -> NexusRed
                    snapshot.ram.usedPercent > 65f -> NexusAmber
                    else -> NexusCyan
                },
                trackColor = Color(0xFF141F2D)
            )
        }
    }
}

@Composable
private fun RadialMemoryGauge(
    percent: Float,
    title: String,
    subtitle: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = color.copy(alpha = 0.15f),
                strokeWidth = 9.dp
            )
            CircularProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 9.dp,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", percent)}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = NexusTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = title,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = NexusTextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MemoryWaveformCard(historyPercents: List<Float>) {
    CyberCard(
        borderColor = NexusCyan.copy(alpha = 0.3f),
        backgroundColor = Color(0xFF070B10)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مخطط استهلاك الذاكرة المباشر (RAM Utilization Waveform)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "LIVE GRAPH",
                fontSize = 9.sp,
                color = NexusGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF03060A), RoundedCornerShape(6.dp))
                .border(0.5.dp, NexusCardBorder, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw background grid lines
                val gridLines = 4
                for (i in 1..gridLines) {
                    val y = height * (i.toFloat() / (gridLines + 1))
                    drawLine(
                        color = Color(0xFF131D28),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                if (historyPercents.size >= 2) {
                    val points = historyPercents.mapIndexed { index, pct ->
                        val x = (index.toFloat() / (historyPercents.size - 1)) * width
                        val y = height - ((pct / 100f).coerceIn(0f, 1f) * height)
                        Offset(x, y)
                    }

                    // Stroke line
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = NexusCyan,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Fill under curve
                    val fillPath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NexusCyan.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryQuickActionsCard(
    isOptimizing: Boolean,
    onForceGc: () -> Unit,
    onTrimMemory: () -> Unit,
    onExportReport: () -> Unit,
    onInspectInTerminal: () -> Unit
) {
    CyberCard(
        borderColor = NexusCardBorder,
        backgroundColor = NexusSurface
    ) {
        Text(
            text = "إجراءات وتحسينات الذاكرة السريعة (Optimization Actions)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NexusTextSecondary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Force GC
            Button(
                onClick = onForceGc,
                enabled = !isOptimizing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("action_force_gc")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOptimizing) "جاري التنظيف..." else "تنظيف GC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Trim Memory
            Button(
                onClick = onTrimMemory,
                enabled = !isOptimizing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusViolet,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("action_trim_memory")
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تقليص Trim",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy Full Report
            OutlinedButton(
                onClick = onExportReport,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NexusCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("action_copy_report")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "نسخ التقرير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Inspect in Terminal
            OutlinedButton(
                onClick = onInspectInTerminal,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NexusGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusGreen),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("action_inspect_terminal")
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "فحص بالطرفية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SwapAndDiagnosticsCard(snapshot: MemorySnapshot) {
    CyberCard(
        borderColor = NexusCardBorder,
        backgroundColor = NexusSurface
    ) {
        Text(
            text = "تشخيص ملف التبديل والذاكرة المنخفضة (Swap / ZRAM & Low-Mem)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NexusTextSecondary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "إجمالي التبديل (ZRAM):", fontSize = 10.sp, color = NexusTextSecondary)
                Text(
                    text = "${snapshot.swapTotalMb} MB",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(text = "المستخدم من التبديل:", fontSize = 10.sp, color = NexusTextSecondary)
                Text(
                    text = "${snapshot.swapUsedMb} MB (${String.format(Locale.US, "%.1f", snapshot.swapUsedPercent)}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (snapshot.swapUsedPercent > 70f) NexusAmber else NexusCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(text = "حالة الذاكرة المنخفضة:", fontSize = 10.sp, color = NexusTextSecondary)
                Text(
                    text = if (snapshot.ram.isLowMemory) "LOW MEMORY" else "STABLE / OK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (snapshot.ram.isLowMemory) NexusRed else NexusGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ProcMemItemCard(item: ProcMemItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = Color(0xFF0A0F16),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, NexusCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.key,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${item.category}]",
                        fontSize = 9.sp,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = item.labelAr,
                    fontSize = 10.sp,
                    color = NexusTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.valueKb} KB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "~${String.format(Locale.US, "%.1f", item.valueMb)} MB",
                    fontSize = 10.sp,
                    color = NexusGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ProcessMemoryCard(
    process: com.example.data.engine.ProcessMemItem,
    totalRamMb: Long
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = Color(0xFF090E15),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, NexusCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(NexusViolet.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PID ${process.pid}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusViolet,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = process.processName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", process.pssMb)} MB PSS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val ratio = if (totalRamMb > 0) (process.pssMb / totalRamMb.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { ratio * 10f }, // Scaled progress for visualization
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NexusViolet,
                trackColor = Color(0xFF141F2D)
            )
        }
    }
}
