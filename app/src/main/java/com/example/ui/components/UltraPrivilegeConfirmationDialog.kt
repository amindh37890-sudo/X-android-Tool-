package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RiskAssessment
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun UltraPrivilegeConfirmationDialog(
    assessment: RiskAssessment,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isConsentAcknowledged by remember { mutableStateOf(false) }
    val riskColor = Color(assessment.privilege.riskLevel.colorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(BorderStroke(1.5.dp, riskColor), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NexusSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Warning Shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(riskColor.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, riskColor.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = riskColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = riskColor.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, riskColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${assessment.privilege.riskLevel.label} (${assessment.overallRiskScore}/100)",
                                    color = riskColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تحليل الأمان وتأكيد المستخدم",
                                fontSize = 11.sp,
                                color = NexusTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextSecondary)
                    }
                }

                // Privilege Title
                Column {
                    Text(
                        text = assessment.privilege.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                    Text(
                        text = assessment.privilege.titleArabic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexusCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target: ${assessment.targetAppName} (${assessment.targetPackage})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (assessment.isTargetTrusted) NexusGreen else NexusRed
                    )
                }

                // Security Impact Analysis Card
                CyberCard(
                    borderColor = riskColor.copy(alpha = 0.4f),
                    backgroundColor = NexusSurfaceVariant
                ) {
                    Text(
                        text = "IMPACT & RISK ANALYSIS / تحليل الأثر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = assessment.impactAnalysis,
                        fontSize = 12.sp,
                        color = NexusTextPrimary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = assessment.impactAnalysisArabic,
                        fontSize = 11.sp,
                        color = NexusTextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Subsystems Affected Chips
                Column {
                    Text(
                        text = "AFFECTED SUBSYSTEMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        assessment.subsystemsAffected.forEach { sub ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = riskColor.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, riskColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = sub,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = riskColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Potential Hazards Bullet List
                Column {
                    Text(
                        text = "SECURITY AUDIT CHECKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    assessment.potentialHazards.forEach { hazard ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("• ", color = riskColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(hazard, fontSize = 11.sp, color = NexusTextSecondary, lineHeight = 15.sp)
                        }
                    }
                }

                // Explicit User Consent Gate
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF141923),
                    border = BorderStroke(1.dp, if (isConsentAcknowledged) NexusCyan else NexusCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isConsentAcknowledged,
                            onCheckedChange = { isConsentAcknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NexusCyan,
                                uncheckedColor = NexusTextSecondary
                            ),
                            modifier = Modifier.testTag("consent_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "I understand the security implications and authorize this ultra-high privilege.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NexusTextPrimary,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "أقر بفهم المخاطر الأمنية وأوافق على منح هذه الصلاحية الفائقة.",
                                fontSize = 10.sp,
                                color = NexusTextSecondary
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NexusCardBorder)
                    ) {
                        Text("CANCEL", color = NexusTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = isConsentAcknowledged,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_ultra_privilege_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = riskColor,
                            contentColor = Color.White,
                            disabledContainerColor = riskColor.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTHORIZE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
