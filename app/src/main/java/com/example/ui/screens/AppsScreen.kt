package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.ui.components.BatchOperationsSheet
import com.example.ui.components.BatchProgressDialog
import com.example.ui.components.BatchSelectionBar
import com.example.ui.components.CyberCard
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.AppFilter
import com.example.ui.viewmodel.NexusViewModel

@Composable
fun AppsScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()

    val isBatchMode by viewModel.isBatchMode.collectAsStateWithLifecycle()
    val selectedBatchPackages by viewModel.selectedBatchPackages.collectAsStateWithLifecycle()
    val showBatchSheet by viewModel.showBatchSheet.collectAsStateWithLifecycle()
    val batchProgressState by viewModel.batchProgressState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search installed packages or Termux...", color = NexusTextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NexusCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NexusTextSecondary)
                    }
                } else {
                    IconButton(onClick = { viewModel.loadInstalledApps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NexusTextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apps_search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NexusCyan,
                unfocusedBorderColor = NexusCardBorder,
                focusedContainerColor = NexusSurfaceVariant,
                unfocusedContainerColor = NexusSurfaceVariant,
                focusedTextColor = NexusTextPrimary,
                unfocusedTextColor = NexusTextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                Pair(AppFilter.ALL, "All Apps"),
                Pair(AppFilter.TERMUX_TOOLS, "Termux & Tools"),
                Pair(AppFilter.AUTHORIZED, "Authorized"),
                Pair(AppFilter.USER, "User Apps"),
                Pair(AppFilter.SYSTEM, "System Apps")
            )

            items(filters) { (filter, label) ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexusCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NexusCyan,
                        containerColor = NexusSurfaceVariant,
                        labelColor = NexusTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = NexusCardBorder,
                        selectedBorderColor = NexusCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Header with Batch Mode Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${apps.size} PACKAGES DETECTED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary,
                    letterSpacing = 0.8.sp
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = NexusCyan,
                        strokeWidth = 2.dp
                    )
                }
            }

            // Batch Mode Toggle Button
            Surface(
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isBatchMode) NexusCyan else NexusCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .testTag("toggle_batch_mode_btn"),
                shape = RoundedCornerShape(8.dp),
                color = if (isBatchMode) NexusCyan.copy(alpha = 0.2f) else NexusSurfaceVariant,
                onClick = { viewModel.toggleBatchMode() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (isBatchMode) NexusCyan else NexusTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBatchMode) "EXIT BATCH" else "BATCH OPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBatchMode) NexusCyan else NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Batch Selection Bar if active
        if (isBatchMode || selectedBatchPackages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BatchSelectionBar(
                selectedCount = selectedBatchPackages.size,
                totalCount = apps.size,
                onSelectAll = { viewModel.selectAllBatchApps() },
                onClearSelection = { viewModel.clearBatchSelection() },
                onOpenBatchActions = { viewModel.openBatchOperations() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NexusCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Scanning device packages & permissions...", color = NexusTextSecondary, fontSize = 13.sp)
                }
            }
        } else if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CyberCard(modifier = Modifier.padding(24.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching packages found", color = NexusTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Try refining your search term or filter category.", color = NexusTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val isSelected = selectedBatchPackages.contains(app.packageName)
                    AppItemRow(
                        app = app,
                        isBatchMode = isBatchMode,
                        isSelected = isSelected,
                        onAppClick = {
                            if (isBatchMode) {
                                viewModel.toggleAppBatchSelection(app.packageName)
                            } else {
                                viewModel.selectApp(app)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleAppBatchSelection(app.packageName)
                        },
                        onAuthToggle = { isChecked ->
                            viewModel.toggleAppAuthorization(app, isChecked)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedApp?.let { app ->
        AppDetailSheet(
            app = app,
            viewModel = viewModel,
            onDismiss = { viewModel.selectApp(null) }
        )
    }

    // Batch Operations Bottom Sheet
    if (showBatchSheet) {
        BatchOperationsSheet(
            selectedCount = selectedBatchPackages.size,
            onDismiss = { viewModel.dismissBatchSheet() },
            onSelectAction = { action -> viewModel.executeBatchAction(action) }
        )
    }

    // Batch Execution Progress Dialog
    if (batchProgressState.isExecuting || batchProgressState.isCompleted) {
        BatchProgressDialog(
            progressState = batchProgressState,
            onDismiss = { viewModel.dismissBatchProgress() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItemRow(
    app: AppInfo,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onAppClick: () -> Unit,
    onLongClick: () -> Unit,
    onAuthToggle: (Boolean) -> Unit
) {
    val borderColor = when {
        isSelected -> NexusCyan
        app.isElevatedAuthorized -> NexusCyan.copy(alpha = 0.5f)
        else -> NexusCardBorder
    }

    val backgroundColor = when {
        isSelected -> Color(0xFF0D1B28)
        else -> NexusSurfaceVariant
    }

    CyberCard(
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        modifier = Modifier
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onLongClick
            )
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for Batch Mode
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onAppClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NexusCyan,
                        checkmarkColor = Color(0xFF001F28),
                        uncheckedColor = NexusTextSecondary
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("batch_checkbox_${app.packageName}")
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            val appBitmap: Bitmap? = app.icon?.toBitmap(72, 72)
            if (appBitmap != null) {
                Image(
                    bitmap = appBitmap.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, if (app.isTermux) NexusCyan else Color.Transparent, RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(NexusSurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (app.isTermux) Icons.Default.Terminal else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (app.isTermux) NexusCyan else NexusTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary,
                        maxLines = 1
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
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (!app.isEnabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = NexusViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FROZEN",
                                color = NexusViolet,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = NexusTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${app.versionName} • UID ${app.uid}",
                        fontSize = 10.sp,
                        color = NexusTextSecondary
                    )
                    if (app.permissions.isNotEmpty()) {
                        val grantedCount = app.permissions.count { it.isGranted }
                        Text(
                            text = " • $grantedCount/${app.permissions.size} Privileged",
                            fontSize = 10.sp,
                            color = if (grantedCount > 0) NexusCyan else NexusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isBatchMode) {
                Switch(
                    checked = app.isElevatedAuthorized,
                    onCheckedChange = onAuthToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NexusCyan,
                        checkedTrackColor = NexusCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = NexusTextSecondary,
                        uncheckedTrackColor = NexusSurfaceVariant
                    ),
                    modifier = Modifier.testTag("app_switch_${app.packageName}")
                )
            }
        }
    }
}
