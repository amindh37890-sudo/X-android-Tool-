package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.manager.AudioRecordTestResult
import com.example.data.manager.MediaItemInfo
import com.example.data.manager.StorageSpaceInfo
import com.example.data.manager.SystemAccessManager
import com.example.service.AdbNotificationBridge
import com.example.ui.components.CyberCard
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full System Access & Media Forensics Screen:
 * - Live Storage Space & Filesystem Inspector
 * - System Photos & Gallery Media Telemetry
 * - Audio Telemetry & Real-time Microphone Sample Probe
 * - Notification Center & Wireless Debugging Direct Notification Trigger
 */
@Composable
fun SystemAccessScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accessManager = remember { SystemAccessManager(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // State holders
    var storageInfo by remember { mutableStateOf<StorageSpaceInfo?>(null) }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentDirPath by remember { mutableStateOf("/system") }
    var imagesList by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var audioList by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var audioTestResult by remember { mutableStateOf<AudioRecordTestResult?>(null) }
    var isTestingAudio by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Permissions Request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch {
            imagesList = accessManager.queryImages()
            audioList = accessManager.queryAudioFiles()
        }
    }

    fun checkAndRequestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.add(Manifest.permission.RECORD_AUDIO)
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(Unit) {
        isLoading = true
        storageInfo = accessManager.getStorageOverview()
        fileList = accessManager.listSystemFiles(currentDirPath)
        imagesList = accessManager.queryImages()
        audioList = accessManager.queryAudioFiles()
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Tabs Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = NexusSurface,
            contentColor = NexusCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = NexusCyan
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("الملفات والنظام", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = {
                    selectedTabIndex = 1
                    checkAndRequestPermissions()
                },
                text = { Text("الصور والوسائط", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("عتاد الصوت", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("الإشعارات والرمز", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> {
                // Filesystem & Storage Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        storageInfo?.let { s ->
                            CyberCard(borderColor = NexusCyan) {
                                Text(
                                    text = "سعة التخزين والنظام الفعلي",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "المسار الداخلي: ${s.internalPath}",
                                    fontSize = 11.sp,
                                    color = NexusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المساحة الحرة: ${s.freeFormatted}", fontSize = 12.sp, color = NexusGreen, fontWeight = FontWeight.Bold)
                                    Text("الإجمالي: ${s.totalFormatted}", fontSize = 12.sp, color = NexusTextPrimary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val usedRatio = if (s.totalBytes > 0) (s.totalBytes - s.freeBytes).toFloat() / s.totalBytes else 0f
                                LinearProgressIndicator(
                                    progress = { usedRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NexusCyan,
                                    trackColor = NexusSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مستعرض مجلدات النظام: $currentDirPath",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    fileList = accessManager.listSystemFiles(currentDirPath)
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan)
                            }
                        }
                    }

                    // Directory buttons
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("/system", "/data", "/proc", "/sdcard").forEach { path ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentDirPath == path) NexusCyan.copy(alpha = 0.2f) else NexusSurfaceVariant,
                                    border = BorderStroke(0.5.dp, if (currentDirPath == path) NexusCyan else NexusCardBorder),
                                    modifier = Modifier.clickable {
                                        currentDirPath = path
                                        scope.launch {
                                            fileList = accessManager.listSystemFiles(path)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = path,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (currentDirPath == path) NexusCyan else NexusTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    items(fileList) { file ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexusSurfaceVariant,
                            border = BorderStroke(0.5.dp, NexusCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) NexusViolet else NexusTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NexusTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "الحجم: ${file.length()} B | تصريح: ${if (file.canRead()) "قراءة" else ""}${if (file.canWrite()) "،كتابة" else ""}",
                                        fontSize = 10.sp,
                                        color = NexusTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Images & Media Forensics Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CyberCard(borderColor = NexusViolet) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الوصول إلى الصور والوسائط",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexusViolet
                                    )
                                    Text(
                                        text = "عرض وتشخيص ${imagesList.size} صورة من وسائط الجهاز الحقيقية",
                                        fontSize = 11.sp,
                                        color = NexusTextSecondary
                                    )
                                }
                                Button(
                                    onClick = { checkAndRequestPermissions() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusViolet,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تحديث الصلاحيات", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (imagesList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد صور أو صلاحية الوسائط بانتظار الموافقة",
                                        color = NexusTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(imagesList) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusSurfaceVariant,
                                border = BorderStroke(0.5.dp, NexusCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NexusTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "النوع: ${item.mimeType ?: "صورة"} | الحجم: ${item.sizeBytes / 1024} KB",
                                            fontSize = 10.sp,
                                            color = NexusTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Audio Hardware & Telemetry Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CyberCard(borderColor = NexusGreen) {
                            Text(
                                text = "فحص عتاد ومستشعرات الصوت الحية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اختبار عتاد الميكروفون المباشر ومعايرة قنوات PCM ومستوى التقاط الإشارة الحقيقية.",
                                fontSize = 11.sp,
                                color = NexusTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isTestingAudio = true
                                        audioTestResult = accessManager.sampleAudioHardware()
                                        isTestingAudio = false
                                    }
                                },
                                enabled = !isTestingAudio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_audio_hardware_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexusGreen,
                                    contentColor = Color(0xFF003314)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingAudio) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFF003314),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري الفحص المباشر...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بدء اختبار عتاد الصوت المباشر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    audioTestResult?.let { res ->
                        item {
                            CyberCard(
                                borderColor = if (res.isHardwareAvailable) NexusGreen else NexusRed
                            ) {
                                Text(
                                    text = "نتائج فحص عتاد الصوت",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.isHardwareAvailable) NexusGreen else NexusRed
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = res.message, fontSize = 11.sp, color = NexusTextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("معدل التردد: ${res.sampleRate} Hz", fontSize = 10.sp, color = NexusTextSecondary)
                                Text("القنوات: ${res.channelConfig}", fontSize = 10.sp, color = NexusTextSecondary)
                                Text("الذروة الحية: ${res.peakAmplitude}", fontSize = 10.sp, color = NexusCyan)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ملفات الصوت المسجلة بالنظام (${audioList.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextSecondary
                        )
                    }

                    items(audioList) { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexusSurfaceVariant,
                            border = BorderStroke(0.5.dp, NexusCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NexusTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "النوع: ${item.mimeType ?: "صوت"} | الحجم: ${item.sizeBytes / 1024} KB",
                                        fontSize = 10.sp,
                                        color = NexusTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Interactive Notification & Pairing Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        CyberCard(borderColor = NexusCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "إشعار الاقتران وتصحيح الأخطاء التفاعلي",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexusCyan
                                    )
                                    Text(
                                        text = "يسجل الرمز مباشرة داخل الإشعار وينفذ الأوامر",
                                        fontSize = 11.sp,
                                        color = NexusTextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "يقوم النظام بإظهار إشعار دائم يحتوي على حقل إدخال فوري للرمز السري (PIN)، بحيث يمكنك نسخه وإدخاله داخل الإشعار دون الحاجة لفتح التطبيق، مع إمكانية إرسال أوامر شل فورية.",
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    AdbNotificationBridge.showPairingNotification(
                                        context = context,
                                        statusText = "جاهز لاستقبال رمز الاقتران (PIN)",
                                        currentPort = 5555,
                                        isConnected = false
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("launch_interactive_notification_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexusCyan,
                                    contentColor = Color(0xFF001F28)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إظهار إشعار الإقران التفاعلي الآن ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        CyberCard {
                            Text(
                                text = "مزايا التحكم على مستوى فوق النظام كله",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• الاقتران اللاسلكي يعمل على منفذ الشل المحلي بدون كمبيوتر.\n• أي تطبيق تختاره يتم منحه صلاحيات عليا حقيقية فوق قيود أندرويد.\n• الميزات الـ 100 العميقة ترتبط مباشرة بالنظام وتعمل بشكل حي ومؤكد.\n• حفظ سجل الأمان ومراجعة الأوامر المنفذة لحماية الجهاز من التلف.",
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
