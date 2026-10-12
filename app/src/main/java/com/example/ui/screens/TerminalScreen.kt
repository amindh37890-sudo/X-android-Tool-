package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.CommandHistoryEntity
import com.example.data.model.ExecutionResult
import com.example.ui.components.CyberCard
import com.example.ui.components.PrivilegeBadge
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTerminalBg
import com.example.ui.theme.NexusTerminalText
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CommandCategory(val label: String) {
    ALL("All"),
    PACKAGES("Packages (pm)"),
    ACTIVITIES("Activities (am)"),
    SYSTEM("System Info"),
    SETTINGS("Settings"),
    APPOPS("AppOps")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.privilegeLevel.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecutingCommand.collectAsStateWithLifecycle()
    val isDirectAdb by viewModel.isDirectAdbMode.collectAsStateWithLifecycle()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsStateWithLifecycle()
    val adbHost by viewModel.adbHost.collectAsStateWithLifecycle()
    val adbPort by viewModel.adbPort.collectAsStateWithLifecycle()
    val activePort by viewModel.activeAdbPort.collectAsStateWithLifecycle()
    val persistentHistory by viewModel.persistentCommandHistory.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var commandInput by remember { mutableStateOf("") }
    var scratchInput by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var selectedCategory by remember { mutableStateOf(CommandCategory.ALL) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val categoryPresets = remember {
        mapOf(
            CommandCategory.ALL to listOf(
                "pm list packages -3",
                "dumpsys battery",
                "getprop ro.build.version.release",
                "id",
                "whoami",
                "cat /proc/version"
            ),
            CommandCategory.PACKAGES to listOf(
                "pm list packages -3",
                "pm path com.termux",
                "pm dump com.termux",
                "pm list permissions -d -g"
            ),
            CommandCategory.ACTIVITIES to listOf(
                "am get-config",
                "am stack list",
                "am start -n com.termux/.app.TermuxActivity",
                "am force-stop com.termux"
            ),
            CommandCategory.SYSTEM to listOf(
                "dumpsys battery",
                "dumpsys meminfo",
                "dumpsys deviceidle",
                "cat /proc/cpuinfo",
                "df -h",
                "uptime"
            ),
            CommandCategory.SETTINGS to listOf(
                "settings get system screen_off_timeout",
                "settings get global airplane_mode_on",
                "settings list global"
            ),
            CommandCategory.APPOPS to listOf(
                "cmd appops get com.termux",
                "cmd appops set com.termux RUN_IN_BACKGROUND allow",
                "cmd appops set com.termux SYSTEM_ALERT_WINDOW allow"
            )
        )
    }

    fun submitCommand(cmd: String = commandInput) {
        if (cmd.isNotBlank()) {
            viewModel.executeCustomCommand(cmd)
            commandInput = ""
            scratchInput = ""
            historyIndex = -1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Status & Channel Header
        CyberCard(
            backgroundColor = Color(0xFF0A0F17),
            borderColor = if (isAdbAvailable) NexusCyan.copy(alpha = 0.5f) else NexusCardBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isAdbAvailable) NexusGreen else NexusRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isDirectAdb) "DIRECT ADB SHELL CHANNEL" else "ELEVATED SYSTEM RUNNER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isAdbAvailable) "Connected: $adbHost:${activePort ?: adbPort}" else "ADB Daemon: Offline (Tap to Scan)",
                            fontSize = 10.sp,
                            color = NexusTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Open Command History Button
                    FilledTonalButton(
                        onClick = { showHistorySheet = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NexusViolet.copy(alpha = 0.2f),
                            contentColor = NexusViolet
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("open_history_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "HIST (${persistentHistory.size})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (!isAdbAvailable) {
                        FilledTonalButton(
                            onClick = { viewModel.autoDetectAdbPort() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexusCyan.copy(alpha = 0.2f),
                                contentColor = NexusCyan
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        PrivilegeBadge(level = currentLevel, showPulse = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Execution Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isDirectAdb) "Mode: Direct ADB Socket Protocol" else "Mode: Highest System Privilege (Root/ADB)",
                    fontSize = 10.sp,
                    color = NexusTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Direct ADB",
                        fontSize = 10.sp,
                        color = if (isDirectAdb) NexusCyan else NexusTextSecondary,
                        fontWeight = if (isDirectAdb) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isDirectAdb,
                        onCheckedChange = { viewModel.setDirectAdbMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexusCyan,
                            checkedTrackColor = NexusCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = NexusTextSecondary,
                            uncheckedTrackColor = NexusSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("direct_adb_mode_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CommandCategory.values()) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat.label,
                            fontSize = 10.sp,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Presets Chips
        val presets = categoryPresets[selectedCategory] ?: emptyList()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { cmd ->
                SuggestionChip(
                    onClick = {
                        commandInput = cmd
                        submitCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusCyan
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = NexusSurfaceVariant
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = NexusCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Terminal Console Screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NexusTerminalBg, RoundedCornerShape(12.dp))
                .border(1.dp, NexusCardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (terminalLogs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(NexusCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ADB SHELL SESSION READY",
                        color = NexusCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAdbAvailable)
                            "Direct socket channel active on $adbHost:${activePort ?: adbPort}. Send commands directly."
                        else
                            "Connect to Wireless ADB or Local ADB port to establish session.",
                        color = NexusTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(terminalLogs) { log ->
                        AdbTerminalLogCard(
                            log = log,
                            onCopy = { clipboardManager.setText(AnnotatedString(log.combinedOutput)) }
                        )
                    }
                }
            }

            // Top action buttons: Copy all & Clear
            if (terminalLogs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0xCC05070A), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val allOutput = terminalLogs.joinToString("\n\n") { "$ ${it.command}\n${it.combinedOutput}" }
                            clipboardManager.setText(AnnotatedString(allOutput))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy all",
                            tint = NexusCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearTerminalLogs() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = NexusRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Quick History Bar (Scrollable recent commands)
        if (persistentHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showHistorySheet = true }
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "History",
                        tint = NexusViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECENT:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusViolet,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(persistentHistory.take(8), key = { it.id }) { item ->
                        Surface(
                            onClick = {
                                commandInput = item.command
                                historyIndex = -1
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (item.isFavorite) NexusAmber.copy(alpha = 0.15f) else NexusSurfaceVariant,
                            border = BorderStroke(
                                0.5.dp,
                                if (item.isFavorite) NexusAmber.copy(alpha = 0.5f) else NexusCardBorder
                            ),
                            modifier = Modifier.testTag("recent_history_chip_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isFavorite) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = NexusAmber,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = item.command,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (item.isFavorite) NexusAmber else NexusTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // History Navigation & Command Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Up Arrow for previous command in history
            Surface(
                onClick = {
                    if (persistentHistory.isNotEmpty()) {
                        if (historyIndex == -1) {
                            scratchInput = commandInput
                        }
                        val nextIndex = (historyIndex + 1).coerceAtMost(persistentHistory.size - 1)
                        historyIndex = nextIndex
                        commandInput = persistentHistory[nextIndex].command
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = if (historyIndex >= 0) NexusCyan.copy(alpha = 0.2f) else NexusSurfaceVariant,
                border = BorderStroke(1.dp, if (historyIndex >= 0) NexusCyan else NexusCardBorder),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("history_up_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "History Up",
                        tint = if (historyIndex >= 0) NexusCyan else NexusTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Down Arrow for next command in history
            Surface(
                onClick = {
                    if (persistentHistory.isNotEmpty()) {
                        if (historyIndex > 0) {
                            val prevIndex = historyIndex - 1
                            historyIndex = prevIndex
                            commandInput = persistentHistory[prevIndex].command
                        } else if (historyIndex == 0) {
                            historyIndex = -1
                            commandInput = scratchInput
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = NexusSurfaceVariant,
                border = BorderStroke(1.dp, NexusCardBorder),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("history_down_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "History Down",
                        tint = NexusTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Command Text Input
            OutlinedTextField(
                value = commandInput,
                onValueChange = {
                    commandInput = it
                    historyIndex = -1
                },
                placeholder = {
                    Text(
                        if (historyIndex >= 0) "History [${historyIndex + 1}/${persistentHistory.size}]" else "adb shell pm / am / dumpsys...",
                        color = if (historyIndex >= 0) NexusCyan.copy(alpha = 0.7f) else NexusTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Text(
                        "$",
                        color = NexusCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                trailingIcon = {
                    if (commandInput.isNotEmpty()) {
                        IconButton(onClick = { commandInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NexusTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCommand() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = if (historyIndex >= 0) NexusCyan.copy(alpha = 0.6f) else NexusCardBorder,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary,
                    focusedContainerColor = NexusSurfaceVariant,
                    unfocusedContainerColor = NexusSurfaceVariant
                )
            )

            // Submit Button
            Button(
                onClick = { submitCommand() },
                enabled = !isExecuting && commandInput.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusCyan,
                    contentColor = Color(0xFF001F28)
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("run_command_btn")
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF001F28),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Command History Modal Sheet
    if (showHistorySheet) {
        CommandHistorySheet(
            history = persistentHistory,
            onDismiss = { showHistorySheet = false },
            onSelect = { cmd ->
                commandInput = cmd
                showHistorySheet = false
            },
            onExecute = { cmd ->
                commandInput = cmd
                submitCommand(cmd)
                showHistorySheet = false
            },
            onToggleFavorite = { id, isFav -> viewModel.toggleHistoryFavorite(id, isFav) },
            onDeleteItem = { id -> viewModel.deleteHistoryItem(id) },
            onClearAll = { viewModel.clearCommandHistory() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandHistorySheet(
    history: List<CommandHistoryEntity>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onExecute: (String) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history else {
            history.filter { it.command.contains(searchQuery, ignoreCase = true) }
        }
    }

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
                            .size(34.dp)
                            .background(NexusViolet.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NexusViolet, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "COMMAND HISTORY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${history.size} saved shell commands",
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (history.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = onClearAll,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexusRed.copy(alpha = 0.15f),
                                contentColor = NexusRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("CLEAR ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search History Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter past commands...", fontSize = 12.sp, color = NexusTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NexusTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusCardBorder,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary,
                    focusedContainerColor = NexusSurfaceVariant,
                    unfocusedContainerColor = NexusSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NexusTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No commands matching '$searchQuery'" else "Command history is empty",
                            color = NexusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        HistoryRowItem(
                            item = item,
                            onSelect = { onSelect(item.command) },
                            onExecute = { onExecute(item.command) },
                            onToggleFavorite = { onToggleFavorite(item.id, !item.isFavorite) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HistoryRowItem(
    item: CommandHistoryEntity,
    onSelect: () -> Unit,
    onExecute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(item.executedAt))

    CyberCard(
        borderColor = if (item.isFavorite) NexusAmber.copy(alpha = 0.5f) else NexusCardBorder,
        backgroundColor = if (item.isFavorite) Color(0xFF14120B) else NexusSurfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Pin Favorite",
                    tint = if (item.isFavorite) NexusAmber else NexusTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect() }
            ) {
                Text(
                    text = item.command,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (item.isFavorite) NexusAmber else NexusCyan,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = NexusTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Insert to input field
            FilledTonalButton(
                onClick = onSelect,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NexusCyan.copy(alpha = 0.15f),
                    contentColor = NexusCyan
                ),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Insert", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("INSERT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Action: Execute now
            Button(
                onClick = onExecute,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusGreen,
                    contentColor = Color(0xFF002912)
                ),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("RUN", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusTextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AdbTerminalLogCard(
    log: ExecutionResult,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090D14), RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (log.isSuccess) NexusCyan.copy(alpha = 0.25f) else NexusRed.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "shell@android:~$ ",
                    fontSize = 11.sp,
                    color = NexusGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = log.command,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NexusCyan
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                color = if (log.isSuccess) NexusGreen.copy(alpha = 0.15f) else NexusRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (log.isSuccess) "EXIT 0 (${log.durationMs}ms)" else "EXIT ${log.exitCode} (${log.durationMs}ms)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (log.isSuccess) NexusGreen else NexusRed,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = log.combinedOutput,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (log.isSuccess) NexusTerminalText else NexusRed,
                lineHeight = 15.sp
            )
        }
    }
}
