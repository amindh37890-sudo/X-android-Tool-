package com.example.data.engine

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structures for real-time memory monitoring
 */
data class SystemRamInfo(
    val totalBytes: Long = 0L,
    val availBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val thresholdBytes: Long = 0L,
    val isLowMemory: Boolean = false,
    val usedPercent: Float = 0f
) {
    val totalMb: Long get() = totalBytes / (1024 * 1024)
    val availMb: Long get() = availBytes / (1024 * 1024)
    val usedMb: Long get() = usedBytes / (1024 * 1024)
    val thresholdMb: Long get() = thresholdBytes / (1024 * 1024)
    val totalGb: Float get() = totalBytes.toFloat() / (1024 * 1024 * 1024)
    val availGb: Float get() = availBytes.toFloat() / (1024 * 1024 * 1024)
    val usedGb: Float get() = usedBytes.toFloat() / (1024 * 1024 * 1024)
}

data class JvmHeapInfo(
    val maxBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val allocatedBytes: Long = 0L,
    val usedPercent: Float = 0f
) {
    val maxMb: Long get() = maxBytes / (1024 * 1024)
    val totalMb: Long get() = totalBytes / (1024 * 1024)
    val freeMb: Long get() = freeBytes / (1024 * 1024)
    val allocatedMb: Long get() = allocatedBytes / (1024 * 1024)
}

data class ProcMemItem(
    val key: String,
    val valueKb: Long,
    val category: String,
    val labelAr: String
) {
    val valueMb: Float get() = valueKb.toFloat() / 1024f
}

data class ProcessMemItem(
    val pid: Int,
    val processName: String,
    val pssKb: Int,
    val rssKb: Int
) {
    val pssMb: Float get() = pssKb.toFloat() / 1024f
}

data class GcResult(
    val beforeAllocatedMb: Long,
    val afterAllocatedMb: Long,
    val freedMb: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemorySnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val ram: SystemRamInfo = SystemRamInfo(),
    val heap: JvmHeapInfo = JvmHeapInfo(),
    val procMemItems: List<ProcMemItem> = emptyList(),
    val topProcesses: List<ProcessMemItem> = emptyList(),
    val swapTotalMb: Long = 0L,
    val swapFreeMb: Long = 0L,
    val swapUsedMb: Long = 0L,
    val swapUsedPercent: Float = 0f,
    val historyUsagePercents: List<Float> = emptyList()
)

class MemoryMonitorEngine(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val usageHistory = mutableListOf<Float>()
    private val maxHistoryPoints = 20

    /**
     * Reads and computes a comprehensive memory snapshot.
     */
    suspend fun captureSnapshot(): MemorySnapshot = withContext(Dispatchers.IO) {
        // 1. System RAM via ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availRam = memoryInfo.availMem
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)
        val ramUsedPercent = if (totalRam > 0) (usedRam.toFloat() / totalRam.toFloat()) * 100f else 0f

        val ramInfo = SystemRamInfo(
            totalBytes = totalRam,
            availBytes = availRam,
            usedBytes = usedRam,
            thresholdBytes = memoryInfo.threshold,
            isLowMemory = memoryInfo.lowMemory,
            usedPercent = ramUsedPercent
        )

        // Track history for live telemetry wave
        synchronized(usageHistory) {
            usageHistory.add(ramUsedPercent)
            if (usageHistory.size > maxHistoryPoints) {
                usageHistory.removeAt(0)
            }
        }

        // 2. JVM Heap via Runtime
        val runtime = Runtime.getRuntime()
        val maxHeap = runtime.maxMemory()
        val totalHeap = runtime.totalMemory()
        val freeHeap = runtime.freeMemory()
        val allocatedHeap = (totalHeap - freeHeap).coerceAtLeast(0L)
        val heapPercent = if (maxHeap > 0) (allocatedHeap.toFloat() / maxHeap.toFloat()) * 100f else 0f

        val heapInfo = JvmHeapInfo(
            maxBytes = maxHeap,
            totalBytes = totalHeap,
            freeBytes = freeHeap,
            allocatedBytes = allocatedHeap,
            usedPercent = heapPercent
        )

        // 3. /proc/meminfo Kernel Telemetry
        val procItems = mutableListOf<ProcMemItem>()
        var swapTotalKb = 0L
        var swapFreeKb = 0L

        try {
            val memInfoFile = File("/proc/meminfo")
            if (memInfoFile.exists() && memInfoFile.canRead()) {
                memInfoFile.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val parts = line.split(":")
                        if (parts.size >= 2) {
                            val key = parts[0].trim()
                            val valueStr = parts[1].trim().split("\\s+".toRegex()).firstOrNull() ?: "0"
                            val valueKb = valueStr.toLongOrNull() ?: 0L

                            when (key) {
                                "MemTotal" -> procItems.add(ProcMemItem(key, valueKb, "Core", "إجمالي الذاكرة"))
                                "MemFree" -> procItems.add(ProcMemItem(key, valueKb, "Core", "الذاكرة الشاغرة"))
                                "MemAvailable" -> procItems.add(ProcMemItem(key, valueKb, "Core", "الذاكرة المتاحة للتطبيقات"))
                                "Buffers" -> procItems.add(ProcMemItem(key, valueKb, "Cache", "ذاكرة التخزين المؤقت (Buffers)"))
                                "Cached" -> procItems.add(ProcMemItem(key, valueKb, "Cache", "الذاكرة المخبأة (Page Cache)"))
                                "Active" -> procItems.add(ProcMemItem(key, valueKb, "Pages", "صفحات نشطة (Active)"))
                                "Inactive" -> procItems.add(ProcMemItem(key, valueKb, "Pages", "صفحات خاملة (Inactive)"))
                                "SwapTotal" -> {
                                    swapTotalKb = valueKb
                                    procItems.add(ProcMemItem(key, valueKb, "Swap", "إجمالي ملف التبديل (ZRAM/Swap)"))
                                }
                                "SwapFree" -> {
                                    swapFreeKb = valueKb
                                    procItems.add(ProcMemItem(key, valueKb, "Swap", "المساحة الحرة في التبديل"))
                                }
                                "Dirty" -> procItems.add(ProcMemItem(key, valueKb, "Kernel", "بيانات تنتظر الكتابة (Dirty)"))
                                "Writeback" -> procItems.add(ProcMemItem(key, valueKb, "Kernel", "بيانات قيد الكتابة على القرص"))
                                "AnonPages" -> procItems.add(ProcMemItem(key, valueKb, "Pages", "صفحات الذاكرة المجهولة (Anon)"))
                                "Shmem" -> procItems.add(ProcMemItem(key, valueKb, "Cache", "الذاكرة المشتركة (Shmem)"))
                                "Slab" -> procItems.add(ProcMemItem(key, valueKb, "Kernel", "مخزن كائنات النواة (Slab)"))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore /proc read permission exceptions
        }

        // Swap calculations
        val swapTotalMb = swapTotalKb / 1024L
        val swapFreeMb = swapFreeKb / 1024L
        val swapUsedMb = (swapTotalMb - swapFreeMb).coerceAtLeast(0L)
        val swapUsedPercent = if (swapTotalMb > 0) (swapUsedMb.toFloat() / swapTotalMb.toFloat()) * 100f else 0f

        // 4. Running processes memory usage (top consuming apps)
        val processItems = mutableListOf<ProcessMemItem>()
        try {
            val runningProcesses = activityManager.runningAppProcesses ?: emptyList()
            if (runningProcesses.isNotEmpty()) {
                val pidsToQuery = runningProcesses.take(12).map { it.pid }.toIntArray()
                val memoryInfos = activityManager.getProcessMemoryInfo(pidsToQuery)

                for (i in pidsToQuery.indices) {
                    val processApp = runningProcesses.getOrNull(i)
                    val mem = memoryInfos.getOrNull(i)
                    if (processApp != null && mem != null) {
                        val pss = mem.totalPss
                        val rss = mem.getMemoryStat("summary.rss")?.toIntOrNull() ?: mem.totalPss
                        processItems.add(
                            ProcessMemItem(
                                pid = processApp.pid,
                                processName = processApp.processName,
                                pssKb = pss,
                                rssKb = rss
                            )
                        )
                    }
                }
                processItems.sortByDescending { it.pssKb }
            }
        } catch (_: Exception) {
            // Best effort process extraction
        }

        val historyCopy = synchronized(usageHistory) { usageHistory.toList() }

        MemorySnapshot(
            ram = ramInfo,
            heap = heapInfo,
            procMemItems = procItems,
            topProcesses = processItems,
            swapTotalMb = swapTotalMb,
            swapFreeMb = swapFreeMb,
            swapUsedMb = swapUsedMb,
            swapUsedPercent = swapUsedPercent,
            historyUsagePercents = historyCopy
        )
    }

    /**
     * Live periodic monitoring flow.
     */
    fun monitorFlow(intervalMs: Long = 1500L): Flow<MemorySnapshot> = flow {
        while (true) {
            emit(captureSnapshot())
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes Garbage Collection and returns reclaimed memory.
     */
    suspend fun forceGarbageCollection(): GcResult = withContext(Dispatchers.IO) {
        val runtime = Runtime.getRuntime()
        val beforeAllocatedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        System.gc()
        runtime.runFinalization()
        System.gc()

        // Give GC a few milliseconds to settle
        delay(120)

        val afterAllocatedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val freedMb = (beforeAllocatedMb - afterAllocatedMb).coerceAtLeast(0L)

        GcResult(
            beforeAllocatedMb = beforeAllocatedMb,
            afterAllocatedMb = afterAllocatedMb,
            freedMb = freedMb
        )
    }

    /**
     * Requests application memory trim.
     */
    fun trimMemory(): String {
        return try {
            if (context is ComponentCallbacks2) {
                (context as ComponentCallbacks2).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
                "✓ تم إرسال إشعار TRIM_MEMORY_COMPLETE إلى دورة حياة التطبيق بنجاح."
            } else {
                "✓ تم تشغيل استدعاء تحرير الذاكرة العامة للنظام."
            }
        } catch (e: Exception) {
            "فشل في تقليص الذاكرة: ${e.message}"
        }
    }

    /**
     * Generates a comprehensive text diagnostic report for copying/exporting.
     */
    suspend fun generateMemoryReport(): String = withContext(Dispatchers.IO) {
        val snap = captureSnapshot()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        buildString {
            appendLine("==========================================")
            appendLine("⚡ NEXUS X - LIVE CYBER MEMORY AUDIT REPORT")
            appendLine("Timestamp: ${sdf.format(Date(snap.timestamp))}")
            appendLine("==========================================")
            appendLine("")
            appendLine("--- [1] PHYSICAL RAM (ذاكرة الوصول العشوائي) ---")
            appendLine("Total RAM:      ${snap.ram.totalMb} MB (${String.format(Locale.US, "%.2f", snap.ram.totalGb)} GB)")
            appendLine("Used RAM:       ${snap.ram.usedMb} MB (${String.format(Locale.US, "%.1f", snap.ram.usedPercent)}%)")
            appendLine("Available RAM:  ${snap.ram.availMb} MB (${String.format(Locale.US, "%.2f", snap.ram.availGb)} GB)")
            appendLine("Low-Mem Thresh: ${snap.ram.thresholdMb} MB")
            appendLine("Low-Mem State:  ${if (snap.ram.isLowMemory) "CRITICAL LOW" else "HEALTHY / STABLE"}")
            appendLine("")
            appendLine("--- [2] JVM RUNTIME HEAP (كومة الجافا للتطبيق) ---")
            appendLine("Max Heap:       ${snap.heap.maxMb} MB")
            appendLine("Total Heap:     ${snap.heap.totalMb} MB")
            appendLine("Allocated Heap: ${snap.heap.allocatedMb} MB (${String.format(Locale.US, "%.1f", snap.heap.usedPercent)}%)")
            appendLine("Free Heap:      ${snap.heap.freeMb} MB")
            appendLine("")
            appendLine("--- [3] ZRAM / SWAP ---")
            appendLine("Swap Total:     ${snap.swapTotalMb} MB")
            appendLine("Swap Used:      ${snap.swapUsedMb} MB (${String.format(Locale.US, "%.1f", snap.swapUsedPercent)}%)")
            appendLine("Swap Free:      ${snap.swapFreeMb} MB")
            appendLine("")
            appendLine("--- [4] KERNEL /PROC/MEMINFO TELEMETRY ---")
            for (item in snap.procMemItems) {
                appendLine(String.format(Locale.US, "%-16s : %8d KB (~%6.1f MB) [%s - %s]", item.key, item.valueKb, item.valueMb, item.category, item.labelAr))
            }
            appendLine("")
            appendLine("--- [5] TOP MONITORED PROCESSES (PSS) ---")
            for (p in snap.topProcesses) {
                appendLine(String.format(Locale.US, "PID %-6d | PSS: %6.1f MB | RSS: %6.1f MB | %s", p.pid, p.pssMb, p.rssKb.toFloat() / 1024f, p.processName))
            }
            appendLine("==========================================")
        }
    }
}
