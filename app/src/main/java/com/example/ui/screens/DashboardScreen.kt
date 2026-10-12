package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PresetRegistry
import com.example.data.model.PrivilegeLevel
import com.example.ui.components.CyberCard
import com.example.ui.components.PrivilegeBadge
import com.example.ui.components.StatTile
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusCyanGlow
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusTab
import com.example.ui.viewmodel.NexusViewModel

@Composable
fun DashboardScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.privilegeLevel.collectAsStateWithLifecycle()
    val isRoot by viewModel.isRootAvailable.collectAsStateWithLifecycle()
    val isAdb by viewModel.isAdbAvailable.collectAsStateWithLifecycle()
    val activePort by viewModel.activeAdbPort.collectAsStateWithLifecycle()
    val seLinux by viewModel.seLinuxStatus.collectAsStateWithLifecycle()
    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeAuthorizedCount.collectAsStateWithLifecycle()
    val selfPrivileges by viewModel.selfPrivilegeStatus.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Status Banner
            CyberCard(
                borderColor = Color(currentLevel.badgeColorHex),
                backgroundColor = Color(0xFF0D121B)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(currentLevel.badgeColorHex), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NEXUS PRIVILEGE ENGINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentLevel.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(currentLevel.badgeColorHex),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentLevel.description,
                            fontSize = 12.sp,
                            color = NexusTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.runDiagnostics() },
                        enabled = !isDiagnosing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_privileges_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusCyan,
                            contentColor = Color(0xFF001F28)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isDiagnosing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF001F28),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROBING...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SCAN ENGINE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.selectTab(NexusTab.WIRELESS_ADB) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("connect_adb_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NexusViolet.copy(alpha = 0.2f),
                            contentColor = NexusViolet
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADB CONNECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // NEXUS X Self-Elevation Card
        item {
            CyberCard(
                borderColor = if (selfPrivileges.isFullyElevated) NexusGreen else NexusCyan,
                backgroundColor = Color(0xFF0F1824)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NexusCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXUS X SELF-ELEVATION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (selfPrivileges.isFullyElevated)
                                "NEXUS X operates at MAXIMUM SYSTEM PRIVILEGES"
                            else
                                "Grant NEXUS X write secure settings, dump & unthrottled execution",
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SelfPermChip(
                        name = "SECURE_SET",
                        granted = selfPrivileges.hasWriteSecureSettings,
                        modifier = Modifier.weight(1f)
                    )
                    SelfPermChip(
                        name = "DUMP_SYS",
                        granted = selfPrivileges.hasDump,
                        modifier = Modifier.weight(1f)
                    )
                    SelfPermChip(
                        name = "USAGE_STAT",
                        granted = selfPrivileges.hasPackageUsageStats,
                        modifier = Modifier.weight(1f)
                    )
                    SelfPermChip(
                        name = "DOZE_OFF",
                        granted = selfPrivileges.isBatteryExempt,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.selfElevateMaxPrivileges() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("self_elevate_nexus_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selfPrivileges.isFullyElevated) NexusGreen else NexusCyan,
                        contentColor = Color(0xFF001F28)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selfPrivileges.isFullyElevated)
                            "RE-APPLY MAXIMUM PRIVILEGES"
                        else
                            "⚡ SUPERCHARGE NEXUS X PRIVILEGES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 100 Deep Dangerous Features Matrix Card
        item {
            CyberCard(
                borderColor = NexusRed,
                backgroundColor = Color(0xFF140D17)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(NexusRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, NexusRed, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dangerous,
                            contentDescription = null,
                            tint = NexusRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "100 DEEP FEATURES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = NexusRed,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = NexusRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "100 ميزة خطيرة",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مصفوفة الـ 100 ميزة الفائقة والخطيرة مع تحذير إلزامي قبل كل استخدام",
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.selectTab(NexusTab.DEEP_FEATURES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_100_deep_features_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فتح مصفوفة الـ 100 ميزة العميقة ⚠️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // SELinux Switcher
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SELINUX ENFORCEMENT SWITCH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Mode: ${seLinux.uppercase()} (Switch for unrestricted kernel hooks)",
                            fontSize = 10.sp,
                            color = NexusTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.setSeLinuxMode(permissive = true) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (seLinux.equals("Permissive", ignoreCase = true)) NexusGreen.copy(alpha = 0.2f) else NexusSurfaceVariant,
                                contentColor = if (seLinux.equals("Permissive", ignoreCase = true)) NexusGreen else NexusTextSecondary
                            )
                        ) {
                            Text("PERMISSIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setSeLinuxMode(permissive = false) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (seLinux.equals("Enforcing", ignoreCase = true)) NexusCyan.copy(alpha = 0.2f) else NexusSurfaceVariant,
                                contentColor = if (seLinux.equals("Enforcing", ignoreCase = true)) NexusCyan else NexusTextSecondary
                            )
                        ) {
                            Text("ENFORCING", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Telemetry Grid
        item {
            Text(
                text = "SYSTEM TELEMETRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        icon = Icons.Default.Bolt,
                        label = "ROOT ACCESS",
                        value = if (isRoot) "AVAILABLE" else "ABSENT",
                        accentColor = if (isRoot) NexusGreen else NexusRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Default.Wifi,
                        label = "ADB DAEMON",
                        value = if (isAdb) "PORT ${activePort ?: 5555}" else "OFFLINE",
                        accentColor = if (isAdb) NexusCyan else NexusAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        icon = Icons.Default.Security,
                        label = "SELINUX MODE",
                        value = seLinux.uppercase(),
                        accentColor = if (seLinux.equals("Enforcing", ignoreCase = true)) NexusCyan else NexusAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Default.VpnKey,
                        label = "AUTHORIZED APPS",
                        value = "$activeCount APPS",
                        accentColor = NexusViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        icon = Icons.Default.Computer,
                        label = "DEVICE MODEL",
                        value = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                        accentColor = NexusCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Default.Memory,
                        label = "KERNEL ABI",
                        value = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                        accentColor = NexusViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Elevated Operations Presets
        item {
            Text(
                text = "FAST ELEVATED PRESETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Special Termux Bridge Preset
            CyberCard(
                borderColor = NexusCyan,
                backgroundColor = Color(0xFF0F1722)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NexusCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = NexusCyan)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TERMUX COMPLETE ELEVATION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Grant WRITE_SECURE_SETTINGS, DUMP, SYSTEM_ALERT_WINDOW, and battery exemption in 1 tap.",
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.elevateTermuxBridge() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("elevate_termux_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusCyan,
                        contentColor = Color(0xFF001F28)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEPLOY TERMUX ELEVATED BRIDGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Other Presets
        items(PresetRegistry.presets.filter { !it.requiresTargetPackage && it.id != "termux_elevate" }.size) { index ->
            val preset = PresetRegistry.presets.filter { !it.requiresTargetPackage && it.id != "termux_elevate" }[index]
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.category.uppercase(),
                                fontSize = 10.sp,
                                color = NexusCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.description,
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.runPreset(preset) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NexusViolet.copy(alpha = 0.2f),
                            contentColor = NexusViolet
                        ),
                        modifier = Modifier.testTag("run_preset_${preset.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RUN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelfPermChip(
    name: String,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (granted) NexusGreen.copy(alpha = 0.15f) else NexusRed.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (granted) NexusGreen.copy(alpha = 0.4f) else NexusRed.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (granted) NexusGreen else NexusRed,
                maxLines = 1
            )
            Text(
                text = if (granted) "OK" else "OFF",
                fontSize = 8.sp,
                color = if (granted) NexusGreen else NexusRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
