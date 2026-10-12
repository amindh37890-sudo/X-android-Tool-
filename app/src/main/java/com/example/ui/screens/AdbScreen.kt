package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CyberCard
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusViewModel

@Composable
fun AdbScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val adbHost by viewModel.adbHost.collectAsStateWithLifecycle()
    val adbPort by viewModel.adbPort.collectAsStateWithLifecycle()
    val adbState by viewModel.adbState.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingAdb.collectAsStateWithLifecycle()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsStateWithLifecycle()
    val activePort by viewModel.activeAdbPort.collectAsStateWithLifecycle()

    var pairCode by remember { mutableStateOf("") }
    var pairPort by remember { mutableStateOf("") }
    val context = LocalContext.current
    val deviceIps = remember { com.example.data.engine.AdbWirelessManager.default.getDeviceIpAddresses() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Connection Status Banner
            CyberCard(
                borderColor = if (isAdbAvailable) NexusCyan else NexusCardBorder,
                backgroundColor = if (isAdbAvailable) Color(0xFF0C1924) else NexusSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (isAdbAvailable) NexusCyan.copy(alpha = 0.2f) else NexusViolet.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isAdbAvailable) NexusCyan else NexusViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAdbAvailable) "ADB DAEMON ONLINE" else "WIRELESS ADB DISCONNECTED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdbAvailable) NexusCyan else NexusTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isAdbAvailable) {
                                "Daemon listening on 127.0.0.1:${activePort ?: adbPort}. Elevated commands active."
                            } else {
                                adbState.message
                            },
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )
                    }
                }
            }
        }

        // Connection Controller
        item {
            Text(
                text = "ADB DAEMON CONNECTION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CyberCard {
                if (deviceIps.size > 1) {
                    Text(
                        text = "عناوين IP المتاحة على الجهاز (انقر للاختيار):",
                        fontSize = 11.sp,
                        color = NexusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deviceIps.forEach { ip ->
                            val isSelected = adbHost == ip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) NexusCyan.copy(alpha = 0.25f) else Color(0xFF142435),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NexusCyan else NexusCardBorder
                                ),
                                modifier = Modifier
                                    .testTag("ip_chip_$ip")
                                    .weight(1f, fill = false),
                                onClick = { viewModel.setAdbHost(ip) }
                            ) {
                                Text(
                                    text = if (ip == "127.0.0.1") "Loopback ($ip)" else "Wi-Fi ($ip)",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NexusCyan else NexusTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = adbHost,
                        onValueChange = { input ->
                            if (input.contains(":")) {
                                val parts = input.split(":")
                                viewModel.setAdbHost(parts[0].trim())
                                if (parts.size > 1 && parts[1].isNotBlank()) {
                                    viewModel.setAdbPort(parts[1].trim())
                                }
                            } else {
                                viewModel.setAdbHost(input)
                            }
                        },
                        label = { Text("Host", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = adbPort,
                        onValueChange = { input ->
                            if (input.contains(":")) {
                                val parts = input.split(":")
                                viewModel.setAdbHost(parts[0].trim())
                                if (parts.size > 1 && parts[1].isNotBlank()) {
                                    viewModel.setAdbPort(parts[1].trim())
                                }
                            } else {
                                viewModel.setAdbPort(input)
                            }
                        },
                        label = { Text("Port", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testAdbConnection() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_adb_connect_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusCyan,
                            contentColor = Color(0xFF001F28)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF001F28),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TESTING...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.autoDetectAdbPort() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_detect_port_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NexusViolet.copy(alpha = 0.2f),
                            contentColor = NexusViolet
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTO-SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Pairing Assistant
        item {
            Text(
                text = "اقتران تصحيح الأخطاء اللاسلكي (WIRELESS ADB PAIRING)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CyberCard(borderColor = NexusCyan) {
                // Automatic Notification Action Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF002230),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = NexusCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الإشعار التفاعلي التلقائي (تسجيل فوري)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط الزر بالأسفل ليظهر إشعار دائم أعلى الشاشة، يمكنك إدخال رمز الـ PIN المكون من 6 أرقام مباشرة داخل الإشعار دون الحاجة للتبديل بين التطبيقات، ويتصل فوراً فوق النظام كاملاً!",
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerPairingNotification()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("launch_pairing_notification_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexusCyan,
                                    contentColor = Color(0xFF001F28)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إظهار الإشعار التلقائي 🔔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
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
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خيارات المطور ⚙️", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "أو أدخل بيانات الإقران يدوياً هنا:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexusTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pairPort,
                        onValueChange = { input ->
                            val trimmed = input.trim()
                            if (trimmed.contains(":")) {
                                val parts = trimmed.split(":")
                                pairPort = parts[0].trim()
                                if (parts.size > 1 && parts[1].length == 6) {
                                    pairCode = parts[1].trim()
                                }
                            } else {
                                pairPort = input
                            }
                        },
                        label = { Text("منفذ الإقران (اختياري/تلقائي)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = pairCode,
                        onValueChange = { input ->
                            val trimmed = input.trim()
                            if (trimmed.contains(" ") || trimmed.contains(":")) {
                                val parts = trimmed.split("[:\\s]+".toRegex())
                                for (p in parts) {
                                    if (p.length == 6 && p.all { it.isDigit() }) {
                                        pairCode = p
                                    } else if (p.toIntOrNull() != null) {
                                        pairPort = p
                                    }
                                }
                            } else {
                                pairCode = input
                            }
                        },
                        label = { Text("رمز PIN (6 أرقام)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (pairCode.isNotBlank()) {
                            viewModel.pairDeviceWithCode(pairPort.ifBlank { null }, pairCode)
                        }
                    },
                    enabled = pairCode.isNotBlank() && !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pair_wireless_adb_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري الاقتران وتأكيد الاتصال...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اقتران وتفعيل الجسر اللاسلكي ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Step-by-Step Instructions
        item {
            Text(
                text = "HOW TO ENABLE WIRELESS DEBUGGING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CyberCard {
                StepItem(
                    number = "1",
                    title = "Enable Developer Options",
                    description = "Go to Settings -> About Phone -> Tap 'Build Number' 7 times."
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(
                    number = "2",
                    title = "Turn on Wireless Debugging",
                    description = "In Settings -> System -> Developer Options -> Turn on 'Wireless Debugging' (ensure connected to Wi-Fi)."
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(
                    number = "3",
                    title = "Pair with Code",
                    description = "Tap 'Pair device with pairing code'. Note the 6-digit code and port shown on screen."
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(
                    number = "4",
                    title = "Connect in NEXUS X",
                    description = "Enter the port and code above. Once paired, NEXUS X executes elevated commands without a PC."
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = NexusCyan.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = NexusCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NexusTextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = NexusTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
