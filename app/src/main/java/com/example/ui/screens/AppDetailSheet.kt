package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppInfo
import com.example.data.model.UltraPrivilegeRegistry
import com.example.ui.components.CyberCard
import com.example.ui.components.UltraPrivilegeConfirmationDialog
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusTab
import com.example.ui.viewmodel.NexusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
    app: AppInfo,
    viewModel: NexusViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pendingAssessment by viewModel.pendingRiskAssessment.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexusSurface,
        dragHandle = null
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val appBitmap: Bitmap? = app.icon?.toBitmap(96, 96)
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(54.dp)
                                .border(1.dp, NexusCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(NexusSurfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = NexusCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.appName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                            if (app.isTermux) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = NexusCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TERMUX",
                                        color = NexusCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = app.packageName,
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UID: ${app.uid} • v${app.versionName} • Target SDK: ${app.targetSdkVersion}",
                            fontSize = 10.sp,
                            color = NexusCyan
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextSecondary)
                    }
                }
            }

            // Authorization Toggle
            item {
                CyberCard(
                    borderColor = if (app.isElevatedAuthorized) NexusCyan else NexusCardBorder,
                    backgroundColor = NexusSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (app.isElevatedAuthorized) NexusCyan.copy(alpha = 0.2f) else NexusTextSecondary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (app.isElevatedAuthorized) NexusCyan else NexusTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEXUS ELEVATION ENGINE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                            Text(
                                text = if (app.isElevatedAuthorized) "App is authorized for elevated bridge actions" else "Authorize this app to receive elevated operations",
                                fontSize = 11.sp,
                                color = NexusTextSecondary
                            )
                        }
                        Switch(
                            checked = app.isElevatedAuthorized,
                            onCheckedChange = { viewModel.toggleAppAuthorization(app, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexusCyan,
                                checkedTrackColor = NexusCyan.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("auth_switch_${app.packageName}")
                        )
                    }
                }
            }

            // 1-Tap Supercharge to Maximum Privileges
            item {
                CyberCard(
                    borderColor = NexusCyan,
                    backgroundColor = Color(0xFF0F1A26)
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
                                text = "MAXIMUM PRIVILEGES ELEVATION",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Grants all secure permissions, enables unrestricted background AppOps, forces ACTIVE standby & Doze exemption.",
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.superElevateApp(app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("super_elevate_app_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusCyan,
                            contentColor = Color(0xFF001F28)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ GRANT ALL MAXIMUM PRIVILEGES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Standby Bucket Manager
            item {
                Text(
                    text = "STANDBY BUCKET (BACKGROUND THROTTLE)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                CyberCard {
                    Text(
                        text = "Force application into Android power bucket to prevent background suspension:",
                        fontSize = 11.sp,
                        color = NexusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.setAppStandbyBucket(app, "active") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexusGreen.copy(alpha = 0.2f),
                                contentColor = NexusGreen
                            )
                        ) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setAppStandbyBucket(app, "working_set") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexusCyan.copy(alpha = 0.2f),
                                contentColor = NexusCyan
                            )
                        ) {
                            Text("WORKING", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setAppStandbyBucket(app, "rare") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexusSurfaceVariant,
                                contentColor = NexusTextSecondary
                            )
                        ) {
                            Text("RARE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Elevated Commands
            item {
                Text(
                    text = "ELEVATED SYSTEM ACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleAppFreeze(app) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isEnabled) NexusViolet.copy(alpha = 0.2f) else NexusGreen.copy(alpha = 0.2f),
                            contentColor = if (app.isEnabled) NexusViolet else NexusGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (app.isEnabled) Icons.Default.AcUnit else Icons.Default.Whatshot,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (app.isEnabled) "FREEZE" else "DEFROST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.forceStopApp(app) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusRed.copy(alpha = 0.2f),
                            contentColor = NexusRed
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FORCE STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearAppData(app) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NexusRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = NexusRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLEAR DATA", color = NexusRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleBatteryOptimization(app, true) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NexusCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DOZE EXEMPT", color = NexusCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sensitive Permissions Manager
            item {
                Text(
                    text = "SENSITIVE PERMISSIONS (${app.permissions.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NEXUS X executes elevated commands (pm grant/revoke) using the highest active engine privilege.",
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }

            if (app.permissions.isEmpty()) {
                item {
                    CyberCard {
                        Text(
                            text = "No privileged sensitive permissions requested in manifest.",
                            fontSize = 12.sp,
                            color = NexusTextSecondary
                        )
                    }
                }
            } else {
                items(app.permissions.size) { index ->
                    val perm = app.permissions[index]
                    CyberCard(
                        borderColor = if (perm.isGranted) NexusGreen.copy(alpha = 0.4f) else NexusCardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = perm.simpleName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NexusTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (perm.isGranted) NexusGreen.copy(alpha = 0.15f) else NexusRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (perm.isGranted) "GRANTED" else "DENIED",
                                            color = if (perm.isGranted) NexusGreen else NexusRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = perm.permission,
                                    fontSize = 10.sp,
                                    color = NexusTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (perm.isGranted) {
                                FilledTonalButton(
                                    onClick = { viewModel.revokePermission(app, perm.permission) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = NexusRed.copy(alpha = 0.15f),
                                        contentColor = NexusRed
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("revoke_${perm.simpleName}")
                                ) {
                                    Text("REVOKE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.grantPermission(app, perm.permission) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NexusCyan,
                                        contentColor = Color(0xFF001F28)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("grant_${perm.simpleName}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Ultra-High Privileges Section with User Risk Analysis
            item {
                Text(
                    text = "ULTRA-HIGH PRIVILEGES (الصلاحيات الفائقة)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "High-impact privileges featuring real-time user risk analysis & explicit security confirmation before deployment.",
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }

            items(UltraPrivilegeRegistry.privileges.size) { index ->
                val ultra = UltraPrivilegeRegistry.privileges[index]
                val riskColor = Color(ultra.riskLevel.colorHex)
                CyberCard(
                    borderColor = riskColor.copy(alpha = 0.5f),
                    backgroundColor = Color(0xFF0F1522)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = riskColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, riskColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = ultra.riskLevel.badgeText,
                                        color = riskColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ultra.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ultra.titleArabic,
                                fontSize = 11.sp,
                                color = NexusCyan
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = ultra.description,
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Subsystems: ${ultra.affectedSubsystems.joinToString(" • ")}",
                                fontSize = 9.sp,
                                color = riskColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.requestUltraPrivilege(app, ultra) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = riskColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("analyze_ultra_${ultra.id}")
                        ) {
                            Text("ANALYZE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Interactive User Risk Analysis & Confirmation Dialog
    pendingAssessment?.let { assessment ->
        UltraPrivilegeConfirmationDialog(
            assessment = assessment,
            onConfirm = { viewModel.confirmAndExecuteUltraPrivilege() },
            onDismiss = { viewModel.dismissRiskAssessment() }
        )
    }
}
