package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import com.example.data.db.AuditLogEntity
import com.example.data.db.AuthorizedAppEntity
import com.example.data.engine.PrivilegeEngine
import com.example.data.model.AppInfo
import com.example.data.model.ExecutionResult
import com.example.data.model.PrivilegeLevel
import com.example.data.model.SensitivePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NexusRepository(
    private val context: Context,
    val privilegeEngine: PrivilegeEngine = PrivilegeEngine(context)
) {
    private val packageManager: PackageManager = context.packageManager
    private val db = AppDatabase.getInstance(context)
    private val authorizedAppDao = db.authorizedAppDao()
    private val auditLogDao = db.auditLogDao()
    private val commandHistoryDao = db.commandHistoryDao()

    val authorizedApps: Flow<List<AuthorizedAppEntity>> = authorizedAppDao.getAllAuthorizedApps()
    val activeAuthorizedCount: Flow<Int> = authorizedAppDao.getActiveAuthorizedCount()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    val totalLogsCount: Flow<Int> = auditLogDao.getTotalLogCount()
    val commandHistory: Flow<List<com.example.data.db.CommandHistoryEntity>> = commandHistoryDao.getAllHistory()
    val historyCount: Flow<Int> = commandHistoryDao.getHistoryCount()

    private val sensitivePermissionList = listOf(
        Pair("android.permission.WRITE_SECURE_SETTINGS", "Write Secure Settings"),
        Pair("android.permission.DUMP", "Dump System State"),
        Pair("android.permission.PACKAGE_USAGE_STATS", "Usage Statistics"),
        Pair("android.permission.SYSTEM_ALERT_WINDOW", "Draw Over Other Apps"),
        Pair("android.permission.READ_LOGS", "Read System Logs (Logcat)"),
        Pair("android.permission.REQUEST_INSTALL_PACKAGES", "Install Unknown Apps"),
        Pair("android.permission.MANAGE_EXTERNAL_STORAGE", "Manage All Storage"),
        Pair("android.permission.CHANGE_CONFIGURATION", "Change System Configuration"),
        Pair("android.permission.INTERNET", "Internet Access"),
        Pair("android.permission.POST_NOTIFICATIONS", "Post Notifications")
    )

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val authorizedMap = try {
            authorizedAppDao.getAllAuthorizedApps().first().associateBy { it.packageName }
        } catch (_: Exception) {
            emptyMap()
        }

        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val resultList = mutableListOf<AppInfo>()

        for (pkg in packages) {
            val appName = try {
                pkg.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
            } catch (_: Exception) {
                pkg.packageName
            }

            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val isTermux = pkg.packageName.startsWith("com.termux") || pkg.packageName.contains("terminal")
            val isEnabled = pkg.applicationInfo?.enabled ?: true
            val isAuthorized = authorizedMap[pkg.packageName]?.isElevatedEnabled == true

            val requestedPermissions = pkg.requestedPermissions ?: emptyArray()
            val sensitiveList = mutableListOf<SensitivePermission>()

            for (sens in sensitivePermissionList) {
                if (requestedPermissions.contains(sens.first)) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        context,
                        sens.first
                    ) == PackageManager.PERMISSION_GRANTED
                    sensitiveList.add(
                        SensitivePermission(
                            permission = sens.first,
                            simpleName = sens.second,
                            description = "Sensitive permission: ${sens.first}",
                            isGranted = isGranted
                        )
                    )
                }
            }

            val icon = try {
                pkg.applicationInfo?.loadIcon(packageManager)
            } catch (_: Exception) {
                null
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            resultList.add(
                AppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = versionCode,
                    uid = pkg.applicationInfo?.uid ?: 0,
                    targetSdkVersion = pkg.applicationInfo?.targetSdkVersion ?: 0,
                    isSystemApp = isSystem,
                    isTermux = isTermux,
                    isEnabled = isEnabled,
                    isElevatedAuthorized = isAuthorized,
                    icon = icon,
                    permissions = sensitiveList,
                    installTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    apkPath = pkg.applicationInfo?.sourceDir ?: ""
                )
            )
        }

        // Sort Termux and user apps first, then alphabetically
        resultList.sortedWith(
            compareByDescending<AppInfo> { it.isTermux }
                .thenByDescending { it.isElevatedAuthorized }
                .thenBy { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
    }

    suspend fun toggleAppAuthorization(app: AppInfo, enabled: Boolean) = withContext(Dispatchers.IO) {
        if (enabled) {
            authorizedAppDao.insertOrUpdate(
                AuthorizedAppEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    isElevatedEnabled = true,
                    authorizedAt = System.currentTimeMillis()
                )
            )
        } else {
            authorizedAppDao.deleteByPackage(app.packageName)
        }
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        auditLogDao.clearAll()
    }

    suspend fun runPreset(preset: com.example.data.model.ElevatedPreset, targetPackage: String? = null): ExecutionResult {
        var command = preset.commandTemplate
        if (targetPackage != null) {
            command = command.replace("{package}", targetPackage)
        }
        return privilegeEngine.execute(
            command = command,
            targetPackage = targetPackage,
            operationName = "Preset: ${preset.title}"
        )
    }

    suspend fun recordCommandInHistory(command: String) = withContext(Dispatchers.IO) {
        val existing = commandHistoryDao.findByCommand(command)
        if (existing != null) {
            commandHistoryDao.updateTimestamp(command, System.currentTimeMillis())
        } else {
            commandHistoryDao.insert(
                com.example.data.db.CommandHistoryEntity(
                    command = command,
                    executedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun toggleHistoryFavorite(id: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        commandHistoryDao.setFavorite(id, isFav)
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        commandHistoryDao.deleteById(id)
    }

    suspend fun clearCommandHistory() = withContext(Dispatchers.IO) {
        commandHistoryDao.clearAll()
    }
}
