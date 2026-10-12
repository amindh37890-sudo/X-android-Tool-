package com.example.data.engine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import com.example.data.db.AuditLogEntity
import com.example.data.model.ExecutionResult
import com.example.data.model.PrivilegeLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class SelfPrivilegeStatus(
    val hasWriteSecureSettings: Boolean,
    val hasDump: Boolean,
    val hasPackageUsageStats: Boolean,
    val hasSystemAlertWindow: Boolean,
    val hasReadLogs: Boolean,
    val isBatteryExempt: Boolean
) {
    val isFullyElevated: Boolean
        get() = hasWriteSecureSettings && hasDump && isBatteryExempt
}

class PrivilegeEngine(
    private val context: Context,
    private val adbWirelessManager: AdbWirelessManager = AdbWirelessManager()
) {
    private val db = AppDatabase.getInstance(context)

    private val _currentLevel = MutableStateFlow(PrivilegeLevel.RESTRICTED)
    val currentLevel: StateFlow<PrivilegeLevel> = _currentLevel.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _isAdbAvailable = MutableStateFlow(false)
    val isAdbAvailable: StateFlow<Boolean> = _isAdbAvailable.asStateFlow()

    private val _activeAdbPort = MutableStateFlow<Int?>(null)
    val activeAdbPort: StateFlow<Int?> = _activeAdbPort.asStateFlow()

    private val _seLinuxStatus = MutableStateFlow("Enforcing")
    val seLinuxStatus: StateFlow<String> = _seLinuxStatus.asStateFlow()

    private val _lastDiagnosticTime = MutableStateFlow(0L)
    val lastDiagnosticTime: StateFlow<Long> = _lastDiagnosticTime.asStateFlow()

    suspend fun runPrivilegeDiagnostics(): PrivilegeLevel = withContext(Dispatchers.IO) {
        val rootFound = testRootPrivilege()
        _isRootAvailable.value = rootFound

        // Check ADB over Wi-Fi / Localhost
        val detectedPort = detectAdbPort()
        _activeAdbPort.value = detectedPort
        val adbFound = detectedPort != null
        _isAdbAvailable.value = adbFound

        // Check SELinux
        _seLinuxStatus.value = querySeLinux()

        val highestLevel = when {
            rootFound -> PrivilegeLevel.ROOT
            adbFound && detectedPort != 5555 -> PrivilegeLevel.WIRELESS_ADB
            adbFound -> PrivilegeLevel.LOCAL_ADB
            testShellPrivilege() -> PrivilegeLevel.APP_SHELL
            else -> PrivilegeLevel.RESTRICTED
        }

        _currentLevel.value = highestLevel
        _lastDiagnosticTime.value = System.currentTimeMillis()
        highestLevel
    }

    private fun testRootPrivilege(): Boolean {
        // Step 1: Check known binary locations
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/magisk/.core/bin/su"
        )
        val binaryExists = paths.any { File(it).exists() }

        // Step 2: Try running `su -c id`
        try {
            val process = ProcessBuilder("su", "-c", "id").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exited = process.waitFor(1200, TimeUnit.MILLISECONDS)
            if (exited && process.exitValue() == 0 && output.contains("uid=0")) {
                return true
            }
        } catch (_: Exception) {
            // Su not found or denied
        }

        return binaryExists
    }

    private suspend fun detectAdbPort(): Int? {
        // Check system property first
        try {
            val propVal = getSystemProperty("service.adb.tcp.port")
            val parsed = propVal.trim().toIntOrNull()
            if (parsed != null && parsed > 0) {
                val test = adbWirelessManager.testAdbPort(port = parsed, timeoutMs = 800)
                if (test.isConnected) return parsed
            }
        } catch (_: Exception) {
            // Ignore
        }

        // Auto-detect common ports
        return adbWirelessManager.autoDetectAdbPort()
    }

    private fun testShellPrivilege(): Boolean {
        return try {
            val process = ProcessBuilder("sh", "-c", "echo nexus_ok").start()
            val exited = process.waitFor(1000, TimeUnit.MILLISECONDS)
            exited && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun querySeLinux(): String {
        return try {
            val process = ProcessBuilder("sh", "-c", "getenforce").start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine() ?: "Enforcing"
            process.waitFor(600, TimeUnit.MILLISECONDS)
            output.trim().ifEmpty { "Enforcing" }
        } catch (_: Exception) {
            "Enforcing"
        }
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val process = ProcessBuilder("getprop", key).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine() ?: ""
            process.waitFor(500, TimeUnit.MILLISECONDS)
            output.trim()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Executes an authorized command using the highest available or explicitly preferred privilege level.
     */
    suspend fun execute(
        command: String,
        targetPackage: String? = null,
        operationName: String = "Raw Shell Command",
        preferLevel: PrivilegeLevel? = null
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val effectiveLevel = preferLevel ?: _currentLevel.value

        var exitCode = -1
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        when (effectiveLevel) {
            PrivilegeLevel.ROOT -> {
                try {
                    val process = ProcessBuilder("su", "-c", command).start()
                    val outReader = BufferedReader(InputStreamReader(process.inputStream))
                    val errReader = BufferedReader(InputStreamReader(process.errorStream))

                    outReader.forEachLine { stdoutBuilder.appendLine(it) }
                    errReader.forEachLine { stderrBuilder.appendLine(it) }

                    val completed = process.waitFor(10, TimeUnit.SECONDS)
                    exitCode = if (completed) process.exitValue() else -1
                    if (!completed) {
                        process.destroyForcibly()
                        stderrBuilder.appendLine("Process timed out after 10s")
                    }
                } catch (e: Exception) {
                    stderrBuilder.appendLine("Root execution failed: ${e.message}")
                    exitCode = 1
                }
            }

            PrivilegeLevel.WIRELESS_ADB, PrivilegeLevel.LOCAL_ADB -> {
                val port = _activeAdbPort.value ?: 5555
                val socketResult = adbWirelessManager.executeOverAdbSocket(
                    host = "127.0.0.1",
                    port = port,
                    shellCommand = command
                )
                exitCode = socketResult.first
                stdoutBuilder.append(socketResult.second)
            }

            PrivilegeLevel.APP_SHELL, PrivilegeLevel.RESTRICTED -> {
                try {
                    val process = ProcessBuilder("sh", "-c", command).start()
                    val outReader = BufferedReader(InputStreamReader(process.inputStream))
                    val errReader = BufferedReader(InputStreamReader(process.errorStream))

                    outReader.forEachLine { stdoutBuilder.appendLine(it) }
                    errReader.forEachLine { stderrBuilder.appendLine(it) }

                    val completed = process.waitFor(10, TimeUnit.SECONDS)
                    exitCode = if (completed) process.exitValue() else -1
                    if (!completed) {
                        process.destroyForcibly()
                        stderrBuilder.appendLine("Process timed out after 10s")
                    }
                } catch (e: Exception) {
                    stderrBuilder.appendLine("Shell execution failed: ${e.message}")
                    exitCode = 1
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val result = ExecutionResult(
            command = command,
            exitCode = exitCode,
            stdout = stdoutBuilder.toString().trim(),
            stderr = stderrBuilder.toString().trim(),
            durationMs = duration,
            privilegeLevel = effectiveLevel
        )

        // Save audit log to database
        try {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    packageName = targetPackage,
                    operation = operationName,
                    command = command,
                    privilegeLevel = effectiveLevel.name,
                    exitCode = exitCode,
                    output = result.combinedOutput.take(1500),
                    timestamp = System.currentTimeMillis(),
                    success = result.isSuccess
                )
            )
        } catch (_: Exception) {
            // Log insert non-fatal
        }

        result
    }

    /**
     * Executes a direct shell command specifically over the established ADB connection.
     */
    suspend fun executeAdbDirect(
        command: String,
        host: String = "127.0.0.1",
        port: Int? = null,
        operationName: String = "Direct ADB Shell"
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val targetPort = port ?: _activeAdbPort.value ?: 5555

        val socketResult = adbWirelessManager.executeOverAdbSocket(
            host = host,
            port = targetPort,
            shellCommand = command
        )
        val duration = System.currentTimeMillis() - startTime

        val result = ExecutionResult(
            command = command,
            exitCode = socketResult.first,
            stdout = if (socketResult.first == 0) socketResult.second else "",
            stderr = if (socketResult.first != 0) socketResult.second else "",
            durationMs = duration,
            privilegeLevel = PrivilegeLevel.WIRELESS_ADB
        )

        try {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    packageName = null,
                    operation = operationName,
                    command = command,
                    privilegeLevel = "WIRELESS_ADB",
                    exitCode = socketResult.first,
                    output = result.combinedOutput.take(1500),
                    timestamp = System.currentTimeMillis(),
                    success = result.isSuccess
                )
            )
        } catch (_: Exception) {}

        result
    }

    // --- High-level elevated operations ---

    suspend fun grantPermission(packageName: String, permission: String): ExecutionResult {
        val cmd = "pm grant $packageName $permission"
        return execute(cmd, packageName, "Grant Permission: ${permission.substringAfterLast('.')}")
    }

    suspend fun revokePermission(packageName: String, permission: String): ExecutionResult {
        val cmd = "pm revoke $packageName $permission"
        return execute(cmd, packageName, "Revoke Permission: ${permission.substringAfterLast('.')}")
    }

    suspend fun setAppOp(packageName: String, opName: String, mode: String = "allow"): ExecutionResult {
        val cmd = "cmd appops set $packageName $opName $mode"
        return execute(cmd, packageName, "AppOps: $opName -> $mode")
    }

    suspend fun setAppEnabled(packageName: String, enabled: Boolean): ExecutionResult {
        val cmd = if (enabled) {
            "pm enable $packageName"
        } else {
            "pm disable-user --user 0 $packageName"
        }
        val op = if (enabled) "Defrost / Enable App" else "Freeze / Disable App"
        return execute(cmd, packageName, op)
    }

    suspend fun forceStopApp(packageName: String): ExecutionResult {
        val cmd = "am force-stop $packageName"
        return execute(cmd, packageName, "Force Stop App")
    }

    suspend fun clearAppData(packageName: String): ExecutionResult {
        val cmd = "pm clear $packageName"
        return execute(cmd, packageName, "Clear App Data")
    }

    suspend fun setBatteryOptimization(packageName: String, exempt: Boolean): ExecutionResult {
        val flag = if (exempt) "+" else "-"
        val cmd = "dumpsys deviceidle whitelist $flag$packageName"
        val op = if (exempt) "Exempt from Battery Saver" else "Enforce Battery Saver"
        return execute(cmd, packageName, op)
    }

    suspend fun elevateTermuxBridge(): List<ExecutionResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExecutionResult>()
        val termuxPkg = "com.termux"

        // Grant write secure settings
        results.add(grantPermission(termuxPkg, "android.permission.WRITE_SECURE_SETTINGS"))
        // Grant DUMP
        results.add(grantPermission(termuxPkg, "android.permission.DUMP"))
        // Grant PACKAGE_USAGE_STATS
        results.add(grantPermission(termuxPkg, "android.permission.PACKAGE_USAGE_STATS"))
        // AppOps SYSTEM_ALERT_WINDOW
        results.add(setAppOp(termuxPkg, "SYSTEM_ALERT_WINDOW", "allow"))
        // Device idle whitelist
        results.add(setBatteryOptimization(termuxPkg, true))

        results
    }

    fun checkSelfPrivileges(): SelfPrivilegeStatus {
        val hasWss = ContextCompat.checkSelfPermission(context, "android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
        val hasDump = ContextCompat.checkSelfPermission(context, "android.permission.DUMP") == PackageManager.PERMISSION_GRANTED
        val hasUsage = ContextCompat.checkSelfPermission(context, "android.permission.PACKAGE_USAGE_STATS") == PackageManager.PERMISSION_GRANTED
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        val hasLogs = ContextCompat.checkSelfPermission(context, "android.permission.READ_LOGS") == PackageManager.PERMISSION_GRANTED
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else true

        return SelfPrivilegeStatus(
            hasWriteSecureSettings = hasWss,
            hasDump = hasDump,
            hasPackageUsageStats = hasUsage,
            hasSystemAlertWindow = hasOverlay,
            hasReadLogs = hasLogs,
            isBatteryExempt = isBatteryExempt
        )
    }

    suspend fun selfElevateMaxPrivileges(): List<ExecutionResult> = withContext(Dispatchers.IO) {
        val selfPkg = context.packageName
        superElevateApp(selfPkg)
    }

    suspend fun superElevateApp(targetPkg: String): List<ExecutionResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExecutionResult>()

        // 1. Grant sensitive system permissions
        val permissionsToGrant = listOf(
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.DUMP",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.READ_LOGS",
            "android.permission.CHANGE_CONFIGURATION",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )
        for (perm in permissionsToGrant) {
            results.add(grantPermission(targetPkg, perm))
        }

        // 2. Set elevated AppOps to allow
        val appOpsToAllow = listOf(
            "SYSTEM_ALERT_WINDOW",
            "RUN_IN_BACKGROUND",
            "RUN_ANY_IN_BACKGROUND",
            "START_FOREGROUND",
            "GET_USAGE_STATS",
            "PICTURE_IN_PICTURE",
            "MANAGE_EXTERNAL_STORAGE"
        )
        for (op in appOpsToAllow) {
            results.add(setAppOp(targetPkg, op, "allow"))
        }

        // 3. Prevent OS from revoking unused permissions
        results.add(setAppOp(targetPkg, "AUTO_REVOKE_PERMISSIONS_IF_UNUSED", "ignore"))

        // 4. Device idle whitelist (Never sleep / Doze exempt)
        results.add(setBatteryOptimization(targetPkg, true))

        // 5. Force Standby Bucket to ACTIVE (zero throttling)
        results.add(setAppStandbyBucket(targetPkg, "active"))

        results
    }

    suspend fun setAppStandbyBucket(packageName: String, bucket: String): ExecutionResult {
        val cmd = "am set-standby-bucket $packageName $bucket"
        return execute(cmd, packageName, "Set Standby Bucket: $bucket")
    }

    suspend fun setSeLinuxMode(permissive: Boolean): ExecutionResult {
        val mode = if (permissive) "0" else "1"
        val cmd = "setenforce $mode"
        val res = execute(cmd, null, "Set SELinux: " + if (permissive) "Permissive" else "Enforcing", PrivilegeLevel.ROOT)
        _seLinuxStatus.value = querySeLinux()
        return res
    }
}
