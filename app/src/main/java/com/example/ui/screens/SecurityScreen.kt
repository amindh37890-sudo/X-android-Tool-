package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AuditLogEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Architecture Banner
            CyberCard(
                borderColor = NexusViolet,
                backgroundColor = Color(0xFF110E1D)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NexusViolet.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = NexusViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CLOSED-SOURCE ENGINE ARCHITECTURE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusViolet,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zero public AIDL or external RPC endpoints exposed. All elevated executions are contained strictly within the NEXUS X sandbox and cryptographically isolated.",
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Security Features Grid
        item {
            Text(
                text = "ENGINE SAFEGUARDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityFeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Strict Process Isolation",
                    description = "Prevents malicious 3rd party applications from hijacking elevated root or ADB sockets."
                )
                SecurityFeatureRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Cryptographic Execution Verification",
                    description = "Commands must be explicitly authorized by user intent with logged return code telemetry."
                )
            }
        }

        // Audit Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUDIT EXECUTION LOGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${logs.size} operations recorded",
                        fontSize = 10.sp,
                        color = NexusCyan
                    )
                }

                if (logs.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { viewModel.clearAuditLogs() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NexusRed.copy(alpha = 0.15f),
                            contentColor = NexusRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("clear_audit_logs_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                CyberCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No audit records yet", color = NexusTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Executed elevated operations will automatically appear here.", color = NexusTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                AuditLogItemCard(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    CyberCard(backgroundColor = NexusSurfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(NexusCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(18.dp))
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
}

@Composable
private fun AuditLogItemCard(log: AuditLogEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(log.timestamp))

    CyberCard(
        borderColor = if (log.success) NexusCardBorder else NexusRed.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.operation,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextPrimary
                )
                if (log.packageName != null) {
                    Text(
                        text = "Target: ${log.packageName}",
                        fontSize = 10.sp,
                        color = NexusCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = if (log.success) NexusGreen.copy(alpha = 0.15f) else NexusRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = log.privilegeLevel,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (log.success) NexusGreen else NexusRed,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$ ${log.command}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = NexusTextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = NexusTextSecondary
            )
            Text(
                text = if (log.success) "EXIT 0" else "EXIT ${log.exitCode}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (log.success) NexusGreen else NexusRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
