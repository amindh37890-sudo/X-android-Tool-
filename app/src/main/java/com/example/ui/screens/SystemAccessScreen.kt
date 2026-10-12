package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.PrivilegeEngine
import com.example.data.manager.*
import com.example.service.AdbNotificationBridge
import com.example.ui.components.CyberCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full System Access, Data/Obb Explorer, File Editor & Media Forensics Screen:
 * - Live Storage Space & Filesystem Inspector
 * - Deep browsing of /system, /data, /storage/emulated/0/Android/data, /storage/emulated/0/Android/obb
 * - Read, Edit, Write, Create Files & Folders
 * - System Photos & Gallery Media Telemetry
 * - Audio Telemetry & Real-time Microphone Sample Probe
 * - Notification Center & Wireless Debugging Direct Notification Trigger
 */
@Composable
fun SystemAccessScreen(
    modifier: Modifier = Modifier,
    privilegeEngine: PrivilegeEngine? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accessManager = remember { SystemAccessManager(context) }
    val fileManager = remember { SystemFileManager(privilegeEngine) }
    val notificationFeedback by AdbNotificationBridge.lastNotificationFeedback.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // State holders
    var storageInfo by remember { mutableStateOf<StorageSpaceInfo?>(null) }
    var currentDirPath by remember { mutableStateOf("/system") }
    var fileNodes by remember { mutableStateOf<List<SystemNodeInfo>>(emptyList()) }
    var isBrowsingFiles by remember { mutableStateOf(false) }

    // Dialog & Editor states
    var viewingNode by remember { mutableStateOf<SystemNodeInfo?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var isReadingFile by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createModeFolder by remember { mutableStateOf(false) } // true for folder, false for file
    var newTargetName by remember { mutableStateOf("") }
    var initialFileBody by remember { mutableStateOf("") }
    var nodeToDelete by remember { mutableStateOf<SystemNodeInfo?>(null) }

    // Media & Telemetry states
    var imagesList by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var audioList by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var audioTestResult by remember { mutableStateOf<AudioRecordTestResult?>(null) }
    var isTestingAudio by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshCurrentDirectory() {
        scope.launch {
            isBrowsingFiles = true
            fileNodes = fileManager.listNodes(currentDirPath)
            isBrowsingFiles = false
        }
    }

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
        refreshCurrentDirectory()
        imagesList = accessManager.queryImages()
        audioList = accessManager.queryAudioFiles()
        isLoading = false
    }

    // Quick shortcuts including /system, /data, Android/data, Android/obb
    val quickLocations = remember {
        listOf(
            "نظام /system" to "/system",
            "بيانات /data" to "/data",
            "بيانات التطبيقات Android/data" to "/storage/emulated/0/Android/data",
            "ملفات الألعاب Android/obb" to "/storage/emulated/0/Android/obb",
            "الذاكرة /sdcard" to "/sdcard",
            "العمليات /proc" to "/proc"
        )
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
                text = { Text("النظام والملفات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                // Filesystem, data & obb Browser & Editor Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        storageInfo?.let { s ->
                            CyberCard(borderColor = NexusCyan) {
                                Text(
                                    text = "سعة التخزين والنظام الفعلي",
                                    fontSize = 13.sp,
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

                    // Directory Location Bar + Navigation Controls
                    item {
                        CyberCard(borderColor = NexusViolet) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "المسار الحالي:",
                                        fontSize = 10.sp,
                                        color = NexusTextSecondary
                                    )
                                    Text(
                                        text = currentDirPath,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexusCyan,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Parent directory button
                                    IconButton(
                                        onClick = {
                                            val parent = File(currentDirPath).parent ?: "/"
                                            currentDirPath = parent
                                            refreshCurrentDirectory()
                                        },
                                        enabled = currentDirPath != "/"
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = if (currentDirPath != "/") NexusCyan else NexusTextSecondary)
                                    }

                                    // Refresh button
                                    IconButton(onClick = { refreshCurrentDirectory() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusCyan)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons: Create Folder / Create File
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        createModeFolder = true
                                        newTargetName = ""
                                        showCreateDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusViolet.copy(alpha = 0.25f),
                                        contentColor = NexusViolet
                                    ),
                                    border = BorderStroke(0.5.dp, NexusViolet),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ مجلد جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        createModeFolder = false
                                        newTargetName = ""
                                        initialFileBody = ""
                                        showCreateDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusGreen.copy(alpha = 0.25f),
                                        contentColor = NexusGreen
                                    ),
                                    border = BorderStroke(0.5.dp, NexusGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ ملف جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Directory shortcuts row
                    item {
                        Column {
                            Text(
                                text = "اختصارات الوصول السريع للمسارات الحساسة (data / obb / system):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NexusTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickLocations.forEach { (label, path) ->
                                    val isSelected = currentDirPath == path
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) NexusCyan.copy(alpha = 0.2f) else NexusSurfaceVariant,
                                        border = BorderStroke(0.5.dp, if (isSelected) NexusCyan else NexusCardBorder),
                                        modifier = Modifier.clickable {
                                            currentDirPath = path
                                            refreshCurrentDirectory()
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSelected) NexusCyan else NexusTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isBrowsingFiles) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NexusCyan, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else if (fileNodes.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusSurfaceVariant,
                                border = BorderStroke(0.5.dp, NexusCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "المجلد فارغ أو يتطلب امتيازات ADB / Root للوصول الكامل",
                                        fontSize = 12.sp,
                                        color = NexusTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "يمكنك إنشاء مجلد أو ملف جديد داخل هذا المسار باستخدام الأزرار أعلاه.",
                                        fontSize = 11.sp,
                                        color = NexusCyan
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "العناصر الموجودة (${fileNodes.size}):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusTextSecondary
                                )
                                Text(
                                    text = "اضغط للمعاينة والتعديل",
                                    fontSize = 10.sp,
                                    color = NexusCyan
                                )
                            }
                        }

                        items(fileNodes) { node ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusSurfaceVariant,
                                border = BorderStroke(0.5.dp, if (node.isDirectory) NexusViolet.copy(alpha = 0.5f) else NexusCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (node.isDirectory) {
                                            currentDirPath = node.path
                                            refreshCurrentDirectory()
                                        } else {
                                            // View / Edit file
                                            viewingNode = node
                                            isReadingFile = true
                                            scope.launch {
                                                val res = fileManager.readFileContent(node.path)
                                                editorContent = res.second
                                                isReadingFile = false
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (node.isDirectory) NexusViolet else NexusCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = node.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NexusTextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (node.isDirectory) "مجلد | صلاحيات: ${node.permissionsStr}"
                                            else "حجم: ${node.size} B | ${if (node.canWrite) "قابل للكتابة" else "للقراءة"} | ${node.permissionsStr}",
                                            fontSize = 10.sp,
                                            color = NexusTextSecondary
                                        )
                                    }

                                    // Action buttons for node
                                    IconButton(
                                        onClick = {
                                            if (!node.isDirectory) {
                                                viewingNode = node
                                                isReadingFile = true
                                                scope.launch {
                                                    val res = fileManager.readFileContent(node.path)
                                                    editorContent = res.second
                                                    isReadingFile = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (node.isDirectory) Icons.Default.ChevronRight else Icons.Default.Edit,
                                            contentDescription = "Edit or open",
                                            tint = NexusCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { nodeToDelete = node },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = NexusRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
                                    colors = ButtonDefaults.buttonColors(containerColor = NexusViolet),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحديث", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (imagesList.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لم يتم العثور على صور أو يلزم منح تصريح قراءة الوسائط من الزر أعلاه.",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 12.sp,
                                    color = NexusTextSecondary
                                )
                            }
                        }
                    } else {
                        items(imagesList) { img ->
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
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NexusViolet, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = img.name, fontSize = 12.sp, color = NexusTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = "الحجم: ${img.sizeBytes / 1024} KB | النوع: ${img.mimeType}", fontSize = 10.sp, color = NexusTextSecondary)
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
                                text = "عتاد وتسجيل الصوت الحي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اختبار ميكروفون النظام وقياس سعة الصوت الحقيقية (RMS Amplitude)",
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
                                    .testTag("test_audio_record_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexusGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingAudio) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري التقاط عينة صوتية...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("بدء فحص الميكروفون المباشر (1 ثانية)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            audioTestResult?.let { res ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (res.isHardwareAvailable) Color(0xFF00221A) else Color(0xFF2E0A10),
                                    border = BorderStroke(0.5.dp, if (res.isHardwareAvailable) NexusGreen else NexusRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = res.message, fontSize = 12.sp, color = if (res.isHardwareAvailable) NexusGreen else NexusRed, fontWeight = FontWeight.Bold)
                                        if (res.isHardwareAvailable) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "أقصى سعة ملتقطة (Peak): ${res.peakAmplitude} | التنسيق: ${res.channelConfig}", fontSize = 11.sp, color = NexusTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ملفات الصوت المسجلة بالنظام (${audioList.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextSecondary
                        )
                    }

                    if (audioList.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NexusSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد ملفات صوتية مسجلة أو لم يتم إعطاء التصريح بعد.",
                                    modifier = Modifier.padding(14.dp),
                                    fontSize = 11.sp,
                                    color = NexusTextSecondary
                                )
                            }
                        }
                    } else {
                        items(audioList) { aud ->
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
                                        Text(text = aud.name, fontSize = 12.sp, color = NexusTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = "الحجم: ${aud.sizeBytes / 1024} KB | النوع: ${aud.mimeType}", fontSize = 10.sp, color = NexusTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Notifications & Quick PIN Input Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CyberCard(borderColor = NexusCyan) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "إشعار الاقتران التفاعلي التلقائي",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "يقوم النظام بإظهار إشعار دائم يحتوي على حقل إدخال فوري للرمز السري (PIN)، بحيث يمكنك نسخه وإدخاله داخل الإشعار دون الحاجة لفتح التطبيق، مع إمكانية إرسال أوامر شل فورية.",
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                        .weight(1f)
                                        .testTag("launch_interactive_notification_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusCyan,
                                        contentColor = Color(0xFF001F28)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إظهار الإشعار التفاعلي ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            })
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                })
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusViolet,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("خيارات المطور ⚙️", fontSize = 11.sp)
                                }
                            }

                            notificationFeedback?.let { fb ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF001E18),
                                    border = BorderStroke(0.5.dp, NexusGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = fb, fontSize = 11.sp, color = NexusGreen)
                                    }
                                }
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
                                text = "• الاقتران اللاسلكي يعمل على منفذ الشل المحلي بدون كمبيوتر.\n• أي تطبيق تختاره يتم منحه صلاحيات عليا حقيقية فوق قيود أندرويد.\n• إمكانية الوصول والتعديل على مجلدات /data و /obb و /system وإنشاء الملفات وحفظها.\n• حفظ سجل الأمان ومراجعة الأوامر المنفذة لحماية الجهاز من التلف.",
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

    // Modal Sheet / Dialog: View & Edit File Content
    viewingNode?.let { node ->
        AlertDialog(
            onDismissRequest = { viewingNode = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = NexusCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "محرر وعارض الملف: ${node.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "المسار: ${node.path}",
                        fontSize = 10.sp,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isReadingFile) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NexusCyan)
                        }
                    } else {
                        OutlinedTextField(
                            value = editorContent,
                            onValueChange = { editorContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexusCyan,
                                unfocusedBorderColor = NexusCardBorder,
                                focusedTextColor = NexusTextPrimary,
                                unfocusedTextColor = NexusTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val opRes = fileManager.writeFileContent(node.path, editorContent)
                            when (opRes) {
                                is FileOpResult.Success -> {
                                    Toast.makeText(context, opRes.message, Toast.LENGTH_SHORT).show()
                                    viewingNode = null
                                    refreshCurrentDirectory()
                                }
                                is FileOpResult.Error -> {
                                    Toast.makeText(context, opRes.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color(0xFF001F28)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ وتعديل الملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingNode = null }) {
                    Text("إغلاق", color = NexusTextSecondary, fontSize = 11.sp)
                }
            },
            containerColor = NexusSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Modal Dialog: Create new Folder or File
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (createModeFolder) Icons.Default.CreateNewFolder else Icons.Default.NoteAdd,
                        contentDescription = null,
                        tint = if (createModeFolder) NexusViolet else NexusGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (createModeFolder) "إنشاء مجلد جديد" else "إنشاء ملف جديد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (createModeFolder) NexusViolet else NexusGreen
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "الموقع الحالي: $currentDirPath",
                        fontSize = 10.sp,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newTargetName,
                        onValueChange = { newTargetName = it },
                        label = { Text(if (createModeFolder) "اسم المجلد (مثال: new_folder)" else "اسم الملف (مثال: config.txt)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (createModeFolder) NexusViolet else NexusGreen,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (!createModeFolder) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = initialFileBody,
                            onValueChange = { initialFileBody = it },
                            label = { Text("المحتوى المبدئي (اختياري)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexusGreen,
                                unfocusedBorderColor = NexusCardBorder,
                                focusedTextColor = NexusTextPrimary,
                                unfocusedTextColor = NexusTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newTargetName.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "يرجى كتابة الاسم", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val res = if (createModeFolder) {
                                fileManager.createDirectory(currentDirPath, name)
                            } else {
                                fileManager.createNewFile(currentDirPath, name, initialFileBody)
                            }

                            when (res) {
                                is FileOpResult.Success -> {
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    showCreateDialog = false
                                    refreshCurrentDirectory()
                                }
                                is FileOpResult.Error -> {
                                    Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (createModeFolder) NexusViolet else NexusGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("إنشاء الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء", color = NexusTextSecondary, fontSize = 11.sp)
                }
            },
            containerColor = NexusSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Delete Confirmation Dialog
    nodeToDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { nodeToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NexusRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد الحذف", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NexusRed)
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف ${if (node.isDirectory) "المجلد ومحتوياته" else "الملف"}:\n${node.path}؟",
                    fontSize = 11.sp,
                    color = NexusTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = fileManager.deleteNode(node.path, node.isDirectory)
                            when (res) {
                                is FileOpResult.Success -> {
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    nodeToDelete = null
                                    refreshCurrentDirectory()
                                }
                                is FileOpResult.Error -> {
                                    Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusRed, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("حذف نهائي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { nodeToDelete = null }) {
                    Text("إلغاء", color = NexusTextSecondary, fontSize = 11.sp)
                }
            },
            containerColor = NexusSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
