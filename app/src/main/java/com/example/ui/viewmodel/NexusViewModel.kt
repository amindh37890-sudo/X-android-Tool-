package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AuditLogEntity
import com.example.data.db.AuthorizedAppEntity
import com.example.data.engine.AdbWirelessManager
import com.example.data.model.AppInfo
import com.example.data.model.ElevatedPreset
import com.example.data.model.ExecutionResult
import com.example.data.model.PrivilegeLevel
import com.example.data.repository.NexusRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL,
    TERMUX_TOOLS,
    AUTHORIZED,
    USER,
    SYSTEM
}

enum class NexusTab {
    DASHBOARD,
    APPS,
    DEEP_FEATURES,
    WIRELESS_ADB,
    TERMINAL,
    SECURITY
}

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NexusRepository(application)
    val privilegeEngine = repository.privilegeEngine
    val ultraPrivilegeEngine = com.example.data.engine.UltraPrivilegeEngine(privilegeEngine)
    private val adbManager = AdbWirelessManager()

    private val _pendingRiskAssessment = MutableStateFlow<com.example.data.model.RiskAssessment?>(null)
    val pendingRiskAssessment: StateFlow<com.example.data.model.RiskAssessment?> = _pendingRiskAssessment.asStateFlow()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(NexusTab.DASHBOARD)
    val currentTab: StateFlow<NexusTab> = _currentTab.asStateFlow()

    fun selectTab(tab: NexusTab) {
        _currentTab.value = tab
    }

    // Privilege State
    val privilegeLevel: StateFlow<PrivilegeLevel> = privilegeEngine.currentLevel
    val isRootAvailable: StateFlow<Boolean> = privilegeEngine.isRootAvailable
    val isAdbAvailable: StateFlow<Boolean> = privilegeEngine.isAdbAvailable
    val activeAdbPort: StateFlow<Int?> = privilegeEngine.activeAdbPort
    val seLinuxStatus: StateFlow<String> = privilegeEngine.seLinuxStatus
    val lastDiagnosticTime: StateFlow<Long> = privilegeEngine.lastDiagnosticTime

    private val _selfPrivilegeStatus = MutableStateFlow(privilegeEngine.checkSelfPrivileges())
    val selfPrivilegeStatus: StateFlow<com.example.data.engine.SelfPrivilegeStatus> = _selfPrivilegeStatus.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    // Installed Apps
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        _allApps,
        _searchQuery,
        _selectedFilter
    ) { apps, query, filter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.TERMUX_TOOLS -> app.isTermux
                AppFilter.AUTHORIZED -> app.isElevatedAuthorized
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected App for details
    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp.asStateFlow()

    // Database Flows
    val authorizedApps: StateFlow<List<AuthorizedAppEntity>> = repository.authorizedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAuthorizedCount: StateFlow<Int> = repository.activeAuthorizedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Terminal State
    private val _terminalLogs = MutableStateFlow<List<ExecutionResult>>(emptyList())
    val terminalLogs: StateFlow<List<ExecutionResult>> = _terminalLogs.asStateFlow()

    private val _isExecutingCommand = MutableStateFlow(false)
    val isExecutingCommand: StateFlow<Boolean> = _isExecutingCommand.asStateFlow()

    private val _isDirectAdbMode = MutableStateFlow(true)
    val isDirectAdbMode: StateFlow<Boolean> = _isDirectAdbMode.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(
        listOf("pm list packages -3", "dumpsys battery", "getprop ro.build.version.release", "id", "whoami")
    )
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    val persistentCommandHistory: StateFlow<List<com.example.data.db.CommandHistoryEntity>> = repository.commandHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wireless ADB Connection State
    private val _adbHost = MutableStateFlow("127.0.0.1")
    val adbHost: StateFlow<String> = _adbHost.asStateFlow()

    private val _adbPort = MutableStateFlow("5555")
    val adbPort: StateFlow<String> = _adbPort.asStateFlow()

    private val _adbState = MutableStateFlow(AdbWirelessManager.AdbConnectionState())
    val adbState: StateFlow<AdbWirelessManager.AdbConnectionState> = _adbState.asStateFlow()

    private val _isTestingAdb = MutableStateFlow(false)
    val isTestingAdb: StateFlow<Boolean> = _isTestingAdb.asStateFlow()

    // Transient UI message / toast
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        runDiagnostics()
        loadInstalledApps()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _isDiagnosing.value = true
            val level = privilegeEngine.runPrivilegeDiagnostics()
            _isDiagnosing.value = false
            _uiEvents.emit("Privilege Scan Complete: Detected ${level.title}")
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _allApps.value = repository.getInstalledApps()
            _isLoadingApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun selectApp(app: AppInfo?) {
        _selectedApp.value = app
    }

    fun toggleAppAuthorization(app: AppInfo, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAppAuthorization(app, enabled)
            loadInstalledApps()
            val msg = if (enabled) "Authorized elevated privileges for ${app.appName}" else "Revoked authorization for ${app.appName}"
            _uiEvents.emit(msg)
        }
    }

    fun grantPermission(app: AppInfo, permission: String) {
        viewModelScope.launch {
            val result = privilegeEngine.grantPermission(app.packageName, permission)
            val permName = permission.substringAfterLast('.')
            if (result.isSuccess) {
                _uiEvents.emit("Successfully granted $permName to ${app.appName}")
            } else {
                _uiEvents.emit("Failed to grant $permName: ${result.stderr.ifEmpty { result.stdout }}")
            }
            loadInstalledApps()
            _selectedApp.value = _allApps.value.find { it.packageName == app.packageName }
        }
    }

    fun revokePermission(app: AppInfo, permission: String) {
        viewModelScope.launch {
            val result = privilegeEngine.revokePermission(app.packageName, permission)
            val permName = permission.substringAfterLast('.')
            if (result.isSuccess) {
                _uiEvents.emit("Revoked $permName from ${app.appName}")
            } else {
                _uiEvents.emit("Failed to revoke $permName: ${result.stderr.ifEmpty { result.stdout }}")
            }
            loadInstalledApps()
            _selectedApp.value = _allApps.value.find { it.packageName == app.packageName }
        }
    }

    fun toggleAppFreeze(app: AppInfo) {
        viewModelScope.launch {
            val willEnable = !app.isEnabled
            val result = privilegeEngine.setAppEnabled(app.packageName, willEnable)
            if (result.isSuccess) {
                val action = if (willEnable) "Defrosted" else "Frozen"
                _uiEvents.emit("$action ${app.appName}")
            } else {
                _uiEvents.emit("Freeze/Defrost failed: ${result.stderr}")
            }
            loadInstalledApps()
            _selectedApp.value = _allApps.value.find { it.packageName == app.packageName }
        }
    }

    fun forceStopApp(app: AppInfo) {
        viewModelScope.launch {
            val result = privilegeEngine.forceStopApp(app.packageName)
            if (result.isSuccess) {
                _uiEvents.emit("Force-stopped ${app.appName}")
            } else {
                _uiEvents.emit("Force-stop failed: ${result.stderr}")
            }
        }
    }

    fun clearAppData(app: AppInfo) {
        viewModelScope.launch {
            val result = privilegeEngine.clearAppData(app.packageName)
            if (result.isSuccess) {
                _uiEvents.emit("Cleared all data for ${app.appName}")
            } else {
                _uiEvents.emit("Clear data failed: ${result.stderr}")
            }
        }
    }

    fun toggleBatteryOptimization(app: AppInfo, exempt: Boolean) {
        viewModelScope.launch {
            val result = privilegeEngine.setBatteryOptimization(app.packageName, exempt)
            if (result.isSuccess) {
                _uiEvents.emit(if (exempt) "Exempted from battery saver" else "Restored battery optimization")
            } else {
                _uiEvents.emit("Failed: ${result.stderr}")
            }
        }
    }

    fun elevateTermuxBridge() {
        viewModelScope.launch {
            val results = privilegeEngine.elevateTermuxBridge()
            val successful = results.count { it.isSuccess }
            _uiEvents.emit("Termux Bridge configured: $successful/${results.size} operations succeeded.")
            loadInstalledApps()
        }
    }

    fun setDirectAdbMode(enabled: Boolean) {
        _isDirectAdbMode.value = enabled
    }

    fun executeCustomCommand(rawCommand: String) {
        val trimmed = rawCommand.trim()
        if (trimmed.isBlank()) return

        if (trimmed.equals("clear", ignoreCase = true)) {
            clearTerminalLogs()
            return
        }

        viewModelScope.launch {
            val updatedHistory = listOf(trimmed) + _commandHistory.value.filter { it != trimmed }
            _commandHistory.value = updatedHistory.take(30)
            repository.recordCommandInHistory(trimmed)

            _isExecutingCommand.value = true

            val result = if (_isDirectAdbMode.value) {
                val port = _adbPort.value.toIntOrNull() ?: 5555
                val host = _adbHost.value.ifBlank { "127.0.0.1" }
                privilegeEngine.executeAdbDirect(
                    command = trimmed,
                    host = host,
                    port = port,
                    operationName = "ADB Shell: $trimmed"
                )
            } else {
                privilegeEngine.execute(
                    command = trimmed,
                    operationName = "Elevated Shell: $trimmed"
                )
            }

            _terminalLogs.value = listOf(result) + _terminalLogs.value
            _isExecutingCommand.value = false
        }
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    fun setAdbHost(host: String) {
        _adbHost.value = host
    }

    fun setAdbPort(port: String) {
        _adbPort.value = port
    }

    fun testAdbConnection() {
        val port = _adbPort.value.toIntOrNull() ?: 5555
        val host = _adbHost.value.ifBlank { "127.0.0.1" }
        viewModelScope.launch {
            _isTestingAdb.value = true
            val state = adbManager.testAdbPort(host = host, port = port)
            _adbState.value = state
            _isTestingAdb.value = false
            if (state.isConnected) {
                _uiEvents.emit("Connected to ADB on $host:$port")
                runDiagnostics()
            } else {
                _uiEvents.emit("Failed to connect: ${state.message}")
            }
        }
    }

    fun autoDetectAdbPort() {
        viewModelScope.launch {
            _isTestingAdb.value = true
            val detected = adbManager.autoDetectAdbPort()
            _isTestingAdb.value = false
            if (detected != null) {
                _adbPort.value = detected.toString()
                _uiEvents.emit("Found ADB daemon on port $detected!")
                testAdbConnection()
            } else {
                _uiEvents.emit("No active ADB daemon found on common ports.")
            }
        }
    }

    fun runPreset(preset: ElevatedPreset, targetPackage: String? = null) {
        viewModelScope.launch {
            val result = repository.runPreset(preset, targetPackage)
            if (result.isSuccess) {
                _uiEvents.emit("Executed preset: ${preset.title}")
            } else {
                _uiEvents.emit("Preset failed: ${result.stderr.ifEmpty { result.stdout }}")
            }
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
            _uiEvents.emit("Security audit logs cleared.")
        }
    }

    fun refreshSelfPrivileges() {
        _selfPrivilegeStatus.value = privilegeEngine.checkSelfPrivileges()
    }

    fun selfElevateMaxPrivileges() {
        viewModelScope.launch {
            _uiEvents.emit("Elevating NEXUS X to maximum system privileges...")
            val results = privilegeEngine.selfElevateMaxPrivileges()
            val successful = results.count { it.isSuccess }
            refreshSelfPrivileges()
            runDiagnostics()
            _uiEvents.emit("NEXUS X Self-Elevation complete: $successful/${results.size} operations granted!")
        }
    }

    fun superElevateApp(app: AppInfo) {
        viewModelScope.launch {
            _uiEvents.emit("Granting maximum privileges to ${app.appName}...")
            val results = privilegeEngine.superElevateApp(app.packageName)
            val successful = results.count { it.isSuccess }
            loadInstalledApps()
            _selectedApp.value = _allApps.value.find { it.packageName == app.packageName }
            _uiEvents.emit("Maximum privileges applied to ${app.appName}: $successful/${results.size} operations succeeded!")
        }
    }

    fun setSeLinuxMode(permissive: Boolean) {
        viewModelScope.launch {
            val result = privilegeEngine.setSeLinuxMode(permissive)
            val mode = if (permissive) "Permissive (0)" else "Enforcing (1)"
            if (result.isSuccess) {
                _uiEvents.emit("SELinux switched to $mode")
            } else {
                _uiEvents.emit("Failed to switch SELinux: ${result.stderr}")
            }
        }
    }

    fun setAppStandbyBucket(app: AppInfo, bucket: String) {
        viewModelScope.launch {
            val result = privilegeEngine.setAppStandbyBucket(app.packageName, bucket)
            if (result.isSuccess) {
                _uiEvents.emit("${app.appName} standby bucket set to ${bucket.uppercase()}")
            } else {
                _uiEvents.emit("Failed: ${result.stderr}")
            }
        }
    }

    fun toggleHistoryFavorite(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleHistoryFavorite(id, isFav)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearCommandHistory() {
        viewModelScope.launch {
            repository.clearCommandHistory()
            _uiEvents.emit("Command history cleared.")
        }
    }

    fun requestUltraPrivilege(app: AppInfo, privilege: com.example.data.model.UltraPrivilege) {
        val assessment = ultraPrivilegeEngine.analyzeRisk(app, privilege)
        _pendingRiskAssessment.value = assessment
    }

    fun dismissRiskAssessment() {
        _pendingRiskAssessment.value = null
    }

    fun confirmAndExecuteUltraPrivilege() {
        val assessment = _pendingRiskAssessment.value ?: return
        viewModelScope.launch {
            val app = _allApps.value.find { it.packageName == assessment.targetPackage }
            if (app != null) {
                _uiEvents.emit("Deploying '${assessment.privilege.title}' via elevated engine...")
                val result = ultraPrivilegeEngine.executeUltraPrivilege(app, assessment.privilege)
                if (result.isSuccess) {
                    _uiEvents.emit("Ultra Privilege '${assessment.privilege.title}' authorized and deployed!")
                } else {
                    _uiEvents.emit("Deployment error: ${result.stderr.ifEmpty { result.stdout }}")
                }
                loadInstalledApps()
                _selectedApp.value = _allApps.value.find { it.packageName == app.packageName }
            }
            _pendingRiskAssessment.value = null
        }
    }

    // ==========================================
    // Batch Operation Manager
    // ==========================================
    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _selectedBatchPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedBatchPackages: StateFlow<Set<String>> = _selectedBatchPackages.asStateFlow()

    private val _showBatchSheet = MutableStateFlow(false)
    val showBatchSheet: StateFlow<Boolean> = _showBatchSheet.asStateFlow()

    private val _batchProgressState = MutableStateFlow(com.example.data.model.BatchProgressState())
    val batchProgressState: StateFlow<com.example.data.model.BatchProgressState> = _batchProgressState.asStateFlow()

    fun toggleBatchMode() {
        val willBeActive = !_isBatchMode.value
        _isBatchMode.value = willBeActive
        if (!willBeActive) {
            _selectedBatchPackages.value = emptySet()
            _showBatchSheet.value = false
        }
    }

    fun setBatchMode(active: Boolean) {
        _isBatchMode.value = active
        if (!active) {
            _selectedBatchPackages.value = emptySet()
            _showBatchSheet.value = false
        }
    }

    fun toggleAppBatchSelection(packageName: String) {
        val current = _selectedBatchPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedBatchPackages.value = current
        if (!_isBatchMode.value && current.isNotEmpty()) {
            _isBatchMode.value = true
        }
    }

    fun selectAllBatchApps() {
        val allFiltered = filteredApps.value.map { it.packageName }.toSet()
        _selectedBatchPackages.value = allFiltered
        if (allFiltered.isNotEmpty()) {
            _isBatchMode.value = true
        }
    }

    fun clearBatchSelection() {
        _selectedBatchPackages.value = emptySet()
    }

    fun openBatchOperations() {
        if (_selectedBatchPackages.value.isNotEmpty()) {
            _showBatchSheet.value = true
        } else {
            viewModelScope.launch {
                _uiEvents.emit("Please select at least one application for batch operations")
            }
        }
    }

    fun dismissBatchSheet() {
        _showBatchSheet.value = false
    }

    fun dismissBatchProgress() {
        _batchProgressState.value = com.example.data.model.BatchProgressState()
    }

    fun executeBatchAction(action: com.example.data.model.BatchActionType) {
        val targetPackages = _selectedBatchPackages.value.toList()
        if (targetPackages.isEmpty()) return

        _showBatchSheet.value = false
        viewModelScope.launch {
            val total = targetPackages.size
            var success = 0
            var failed = 0

            _batchProgressState.value = com.example.data.model.BatchProgressState(
                isExecuting = true,
                totalCount = total,
                actionType = action
            )

            for ((index, pkg) in targetPackages.withIndex()) {
                val app = _allApps.value.find { it.packageName == pkg }
                val appName = app?.appName ?: pkg

                _batchProgressState.value = _batchProgressState.value.copy(
                    currentPackageName = pkg,
                    currentAppName = appName,
                    completedCount = index
                )

                val opSuccess = when (action) {
                    com.example.data.model.BatchActionType.SUPERCHARGE_MAX_PRIVILEGES -> {
                        val results = privilegeEngine.superElevateApp(pkg)
                        if (app != null) repository.toggleAppAuthorization(app, true)
                        results.any { it.isSuccess }
                    }
                    com.example.data.model.BatchActionType.AUTHORIZE_NEXUS -> {
                        if (app != null) repository.toggleAppAuthorization(app, true)
                        true
                    }
                    com.example.data.model.BatchActionType.REVOKE_NEXUS -> {
                        if (app != null) repository.toggleAppAuthorization(app, false)
                        true
                    }
                    com.example.data.model.BatchActionType.EXEMPT_BATTERY_SAVER -> {
                        privilegeEngine.setBatteryOptimization(pkg, true).isSuccess
                    }
                    com.example.data.model.BatchActionType.SET_STANDBY_ACTIVE -> {
                        privilegeEngine.setAppStandbyBucket(pkg, "active").isSuccess
                    }
                    com.example.data.model.BatchActionType.FORCE_STOP -> {
                        privilegeEngine.forceStopApp(pkg).isSuccess
                    }
                    com.example.data.model.BatchActionType.FREEZE_APPS -> {
                        privilegeEngine.setAppEnabled(pkg, false).isSuccess
                    }
                    com.example.data.model.BatchActionType.DEFROST_APPS -> {
                        privilegeEngine.setAppEnabled(pkg, true).isSuccess
                    }
                    com.example.data.model.BatchActionType.CLEAR_DATA -> {
                        privilegeEngine.clearAppData(pkg).isSuccess
                    }
                }

                if (opSuccess) success++ else failed++

                _batchProgressState.value = _batchProgressState.value.copy(
                    completedCount = index + 1,
                    successfulCount = success,
                    failedCount = failed
                )
            }

            _batchProgressState.value = _batchProgressState.value.copy(
                isExecuting = false,
                isCompleted = true,
                completedCount = total,
                successfulCount = success,
                failedCount = failed
            )

            loadInstalledApps()
            _uiEvents.emit("Batch ${action.title} finished: $success succeeded, $failed failed.")
        }
    }

    // ==========================================
    // 100 DEEP DANGEROUS FEATURES LOGIC
    // ==========================================
    private val _activeDeepFeatureWarning = MutableStateFlow<com.example.data.model.DeepDangerousFeature?>(null)
    val activeDeepFeatureWarning: StateFlow<com.example.data.model.DeepDangerousFeature?> = _activeDeepFeatureWarning.asStateFlow()

    private val _deepFeatureFilter = MutableStateFlow<com.example.data.model.DeepCategory?>(null)
    val deepFeatureFilter: StateFlow<com.example.data.model.DeepCategory?> = _deepFeatureFilter.asStateFlow()

    private val _deepFeatureSearch = MutableStateFlow("")
    val deepFeatureSearch: StateFlow<String> = _deepFeatureSearch.asStateFlow()

    private val _lastDeepExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastDeepExecutionResult: StateFlow<ExecutionResult?> = _lastDeepExecutionResult.asStateFlow()

    private val _isExecutingDeepFeature = MutableStateFlow(false)
    val isExecutingDeepFeature: StateFlow<Boolean> = _isExecutingDeepFeature.asStateFlow()

    fun openDeepFeatureWarning(feature: com.example.data.model.DeepDangerousFeature) {
        _activeDeepFeatureWarning.value = feature
    }

    fun closeDeepFeatureWarning() {
        _activeDeepFeatureWarning.value = null
    }

    fun dismissDeepExecutionResult() {
        _lastDeepExecutionResult.value = null
    }

    fun setDeepFeatureCategory(category: com.example.data.model.DeepCategory?) {
        _deepFeatureFilter.value = category
    }

    fun setDeepFeatureSearch(query: String) {
        _deepFeatureSearch.value = query
    }

    fun executeDeepFeature(
        feature: com.example.data.model.DeepDangerousFeature,
        commandToRun: String,
        targetPackage: String? = null
    ) {
        viewModelScope.launch {
            _activeDeepFeatureWarning.value = null
            _isExecutingDeepFeature.value = true
            _uiEvents.emit("⚡ جاري تنفيذ الميزة #${feature.number}: ${feature.titleAr}")

            val result = privilegeEngine.execute(
                command = commandToRun,
                targetPackage = targetPackage ?: "com.android.system",
                operationName = "Deep Feature #${feature.number}: ${feature.titleEn}"
            )

            _lastDeepExecutionResult.value = result
            _isExecutingDeepFeature.value = false

            if (result.isSuccess) {
                _uiEvents.emit("✓ تم تنفيذ الميزة #${feature.number} بنجاح: ${feature.titleAr}")
            } else {
                _uiEvents.emit("⚠️ خطأ في تنفيذ الميزة #${feature.number}: ${result.stderr.ifBlank { "رمز الخطأ ${result.exitCode}" }}")
            }
        }
    }
}
