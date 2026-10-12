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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
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
    var isPairing by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = adbHost,
                        onValueChange = { viewModel.setAdbHost(it) },
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
                        onValueChange = { viewModel.setAdbPort(it) },
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
                text = "WIRELESS DEBUGGING PAIRING (ANDROID 11+)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pairPort,
                        onValueChange = { pairPort = it },
                        label = { Text("Pairing Port (e.g. 38475)", fontSize = 11.sp) },
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
                        onValueChange = { pairCode = it },
                        label = { Text("6-Digit PIN", fontSize = 11.sp) },
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
                        if (pairPort.isNotBlank() && pairCode.isNotBlank()) {
                            isPairing = true
                            viewModel.setAdbPort(pairPort)
                            viewModel.testAdbConnection()
                            isPairing = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pair_wireless_adb_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PAIR & ACTIVATE BRIDGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
