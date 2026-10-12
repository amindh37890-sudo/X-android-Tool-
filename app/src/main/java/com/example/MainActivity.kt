package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PrivilegeBadge
import com.example.ui.screens.AdbScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeepFeaturesScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusTheme
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusTab
import com.example.ui.viewmodel.NexusViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                NexusMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NexusMainScreen(viewModel: NexusViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentLevel by viewModel.privilegeLevel.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = selectedApp != null || currentTab != NexusTab.DASHBOARD) {
        if (selectedApp != null) {
            viewModel.selectApp(null)
        } else {
            viewModel.selectTab(NexusTab.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NexusBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexusTopAppBar(currentLevel = currentLevel)
        },
        bottomBar = {
            NexusBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                NexusTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                NexusTab.APPS -> AppsScreen(viewModel = viewModel)
                NexusTab.DEEP_FEATURES -> DeepFeaturesScreen(viewModel = viewModel)
                NexusTab.WIRELESS_ADB -> AdbScreen(viewModel = viewModel)
                NexusTab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                NexusTab.SECURITY -> SecurityScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun NexusTopAppBar(currentLevel: com.example.data.model.PrivilegeLevel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = NexusSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-tech logo badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(NexusCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, NexusCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = NexusCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEXUS X",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = NexusTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "ELEVATED OPS ENGINE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary,
                    letterSpacing = 0.8.sp
                )
            }

            PrivilegeBadge(level = currentLevel, showPulse = true)
        }
    }
}

@Composable
private fun NexusBottomNavBar(
    currentTab: NexusTab,
    onTabSelected: (NexusTab) -> Unit
) {
    NavigationBar(
        containerColor = NexusSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                androidx.compose.foundation.BorderStroke(0.5.dp, NexusCardBorder)
            )
            .testTag("nexus_bottom_navigation")
    ) {
        NavigationBarItem(
            selected = currentTab == NexusTab.DASHBOARD,
            onClick = { onTabSelected(NexusTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Bolt, contentDescription = "Engine") },
            label = { Text("Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("nav_tab_engine")
        )

        NavigationBarItem(
            selected = currentTab == NexusTab.APPS,
            onClick = { onTabSelected(NexusTab.APPS) },
            icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
            label = { Text("Apps", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("nav_tab_apps")
        )

        NavigationBarItem(
            selected = currentTab == NexusTab.DEEP_FEATURES,
            onClick = { onTabSelected(NexusTab.DEEP_FEATURES) },
            icon = { Icon(Icons.Default.Dangerous, contentDescription = "100 Deep") },
            label = { Text("100 Deep", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("nav_tab_deep_features")
        )

        NavigationBarItem(
            selected = currentTab == NexusTab.TERMINAL,
            onClick = { onTabSelected(NexusTab.TERMINAL) },
            icon = { Icon(Icons.Default.Terminal, contentDescription = "Shell") },
            label = { Text("Shell", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("nav_tab_shell")
        )

        NavigationBarItem(
            selected = currentTab == NexusTab.SECURITY,
            onClick = { onTabSelected(NexusTab.SECURITY) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
            label = { Text("Audit", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("nav_tab_audit")
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = NexusCyan,
    selectedTextColor = NexusCyan,
    unselectedIconColor = NexusTextSecondary,
    unselectedTextColor = NexusTextSecondary,
    indicatorColor = NexusCyan.copy(alpha = 0.15f)
)
