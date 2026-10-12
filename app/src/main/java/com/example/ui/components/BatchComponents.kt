package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BatchActionType
import com.example.data.model.BatchProgressState
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchOperationsSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onSelectAction: (BatchActionType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexusSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NexusCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BATCH OVERRIDE MANAGER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "مدير العمليات المتعددة • $selectedCount تطبيقات محددة",
                            fontSize = 11.sp,
                            color = NexusCyan
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                items(BatchActionType.values()) { action ->
                    val actionColor = Color(action.badgeColorHex)
                    CyberCard(
                        borderColor = if (action == BatchActionType.SUPERCHARGE_MAX_PRIVILEGES) NexusGreen.copy(alpha = 0.6f) else NexusCardBorder,
                        backgroundColor = if (action == BatchActionType.SUPERCHARGE_MAX_PRIVILEGES) Color(0xFF0F1A1B) else NexusSurfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectAction(action) }
                            .testTag("batch_action_${action.name}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(actionColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getBatchIcon(action.iconName),
                                    contentDescription = null,
                                    tint = actionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusTextPrimary
                                )
                                Text(
                                    text = action.titleArabic,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = actionColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = action.description,
                                    fontSize = 10.sp,
                                    color = NexusTextSecondary,
                                    lineHeight = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = actionColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, actionColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "1-CLICK",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = actionColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BatchProgressDialog(
    progressState: BatchProgressState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (progressState.isCompleted) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = progressState.isCompleted, dismissOnClickOutside = progressState.isCompleted)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, NexusCyan), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NexusSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (progressState.isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NexusCyan,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EXECUTING BATCH OVERRIDES...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NexusGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "BATCH COMPLETED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${progressState.completedCount} / ${progressState.totalCount} packages",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusTextSecondary
                        )
                        Text(
                            text = "${(progressState.progressPercent * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressState.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = NexusCyan,
                        trackColor = NexusSurfaceVariant
                    )
                }

                // Current Package
                if (progressState.isExecuting && progressState.currentAppName.isNotBlank()) {
                    CyberCard(backgroundColor = NexusSurfaceVariant) {
                        Text(
                            text = "Current: ${progressState.currentAppName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                        Text(
                            text = progressState.currentPackageName,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusCyan
                        )
                    }
                }

                // Summary Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = NexusGreen.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, NexusGreen.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SUCCESS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NexusGreen)
                            Text("${progressState.successfulCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NexusGreen, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = NexusRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, NexusRed.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("FAILED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NexusRed)
                            Text("${progressState.failedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NexusRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (progressState.isCompleted) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_batch_dialog_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusCyan,
                            contentColor = Color(0xFF001F28)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DONE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BatchSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenBatchActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("batch_selection_bar"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1420),
        border = BorderStroke(1.dp, NexusCyan.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = NexusCyan.copy(alpha = 0.2f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$selectedCount",
                            color = NexusCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$selectedCount SELECTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (selectedCount == totalCount) "Tap Clear to reset" else "Tap Select All for all apps",
                        fontSize = 9.sp,
                        color = NexusTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (selectedCount == totalCount) onClearSelection() else onSelectAll() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (selectedCount == totalCount) Icons.Default.Clear else Icons.Default.Check,
                        contentDescription = "Toggle all",
                        tint = NexusTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = onOpenBatchActions,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusCyan,
                        contentColor = Color(0xFF001F28)
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("open_batch_sheet_btn")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("APPLY OVERRIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getBatchIcon(name: String): ImageVector {
    return when (name) {
        "Bolt" -> Icons.Default.Bolt
        "VpnKey" -> Icons.Default.VpnKey
        "BatteryAlert" -> Icons.Default.BatteryAlert
        "Speed" -> Icons.Default.Speed
        "Block" -> Icons.Default.Block
        "AcUnit" -> Icons.Default.AcUnit
        "Whatshot" -> Icons.Default.Whatshot
        "Delete" -> Icons.Default.Delete
        else -> Icons.Default.Close
    }
}
