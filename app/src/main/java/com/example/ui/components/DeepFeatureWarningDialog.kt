package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DangerLevel
import com.example.data.model.DeepDangerousFeature
import com.example.data.model.TargetType
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun DeepFeatureWarningDialog(
    feature: DeepDangerousFeature,
    targetPackage: String? = null,
    onConfirm: (commandToRun: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isAcknowledged by remember { mutableStateOf(false) }
    val dangerColor = Color(feature.dangerLevel.colorHex)

    val finalCommand = remember(feature, targetPackage) {
        val pkg = targetPackage ?: "com.android.systemui"
        feature.command.replace("{package}", pkg)
            .replace("{package_path_placeholder}", "/data/local/tmp/app.apk")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, dangerColor, RoundedCornerShape(16.dp))
                .testTag("deep_feature_warning_dialog"),
            color = Color(0xFF0A0F18)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(20.dp)
            ) {
                // Header with Warning Pulse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(dangerColor.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, dangerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Hazard Warning",
                                tint = dangerColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FEATURE #${String.format("%03d", feature.number)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = dangerColor,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = dangerColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, dangerColor)
                                ) {
                                    Text(
                                        text = feature.dangerLevel.badgeLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dangerColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "تحذير أمني قبل التشغيل",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                        }
                    }

                    // Risk Score Badge
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${feature.riskScore}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = dangerColor
                        )
                        Text(
                            text = "RISK INDEX",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Risk progress bar
                LinearProgressIndicator(
                    progress = { feature.riskScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = dangerColor,
                    trackColor = NexusSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title in Arabic & English
                    Text(
                        text = feature.titleAr,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = NexusTextPrimary,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = feature.titleEn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // PRIMARY SAFETY WARNING BOX (التحذير الصريح والواضح)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = dangerColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, dangerColor.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = dangerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "التحذير الأمني الصريح:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = dangerColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = feature.warningAr,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feature.warningEn,
                                fontSize = 11.sp,
                                color = NexusTextSecondary,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Deep Description
                    Text(
                        text = "شرح عمل الميزة وآلية النظام:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.descriptionAr,
                        fontSize = 12.sp,
                        color = NexusTextPrimary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Potential Hazards
                    Text(
                        text = "المخاطر والعواقب المحتملة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    feature.potentialHazards.forEach { hazard ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = dangerColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = hazard,
                                color = NexusTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shell Command Preview
                    Text(
                        text = "الأمر البرمجي الذي سيتم تنفيذه:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF03070D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, NexusCardBorder)
                    ) {
                        Text(
                            text = "$ $finalCommand",
                            modifier = Modifier.padding(10.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NexusCyan,
                            lineHeight = 15.sp
                        )
                    }

                    if (feature.targetType != TargetType.GLOBAL_SYSTEM && targetPackage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "الحزمة المستهدفة: $targetPackage",
                            fontSize = 11.sp,
                            color = NexusTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mandatory Checkbox
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NexusSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isAcknowledged) NexusCyan else NexusCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAcknowledged,
                                onCheckedChange = { isAcknowledged = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = dangerColor,
                                    uncheckedColor = NexusTextSecondary
                                ),
                                modifier = Modifier.testTag("deep_feature_acknowledge_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أقر بأنني قرأت وفهمت كافة التحذيرات والمخاطر المذكورة أعلاه، وأتحمل المسؤولية الكاملة عن تنفيذ هذا التعديل العميق.",
                                fontSize = 11.sp,
                                color = NexusTextPrimary,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_warning_dialog_button"),
                        border = BorderStroke(1.dp, NexusCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = NexusTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إلغاء وتراجع",
                            color = NexusTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (isAcknowledged) {
                                onConfirm(finalCommand)
                            }
                        },
                        enabled = isAcknowledged,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_dangerous_execution_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dangerColor,
                            disabledContainerColor = dangerColor.copy(alpha = 0.3f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأكيد التنفيذ الخطر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
