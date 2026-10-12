package com.example.data.manager

import com.example.data.engine.PrivilegeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SystemNodeInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val canRead: Boolean,
    val canWrite: Boolean,
    val lastModified: Long,
    val permissionsStr: String = "",
    val isElevatedAccess: Boolean = false
)

sealed class FileOpResult {
    data class Success(val message: String) : FileOpResult()
    data class Error(val message: String) : FileOpResult()
}

class SystemFileManager(
    private val privilegeEngine: PrivilegeEngine? = null
) {

    /**
     * Lists directory files and folders with graceful fallback to elevated shell (ADB / Root)
     * when standard Java File API gets permission denied (common on Android 11+ for /data, /storage/emulated/0/Android/data, and obb).
     */
    suspend fun listNodes(directoryPath: String): List<SystemNodeInfo> = withContext(Dispatchers.IO) {
        val normalizedPath = directoryPath.trim().removeSuffix("/").ifEmpty { "/" }
        val dir = File(normalizedPath)

        // 1. Try normal Java File API
        val javaFiles = try {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()
            } else null
        } catch (_: Exception) {
            null
        }

        if (javaFiles != null && javaFiles.isNotEmpty()) {
            return@withContext javaFiles.map { f ->
                SystemNodeInfo(
                    name = f.name,
                    path = f.absolutePath,
                    isDirectory = f.isDirectory,
                    size = if (f.isFile) f.length() else 0L,
                    canRead = f.canRead(),
                    canWrite = f.canWrite(),
                    lastModified = f.lastModified(),
                    permissionsStr = "${if (f.canRead()) "r" else "-"}${if (f.canWrite()) "w" else "-"}${if (f.canExecute()) "x" else "-"}",
                    isElevatedAccess = false
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }

        // 2. If empty or null (e.g. permission restricted on /data, /data/data, Android/data, Android/obb),
        // try listing via elevated Shell (su / adb socket / app shell)
        val shellNodes = listViaShell(normalizedPath)
        if (shellNodes.isNotEmpty()) {
            return@withContext shellNodes.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }

        // 3. Fallback: return empty or single parent if exists
        emptyList()
    }

    private suspend fun listViaShell(directoryPath: String): List<SystemNodeInfo> {
        val cmd = "ls -la \"$directoryPath\" 2>/dev/null || ls -l \"$directoryPath\""
        val output = executeShellCommand(cmd)

        val nodes = mutableListOf<SystemNodeInfo>()
        val lines = output.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("total ") || trimmed.startsWith("ls: ")) continue

            // Parse typical ls -l:
            // drwxrwx--x  4 root  sdcard_rw 4096 2026-10-12 12:00 com.whatsapp
            // -rw-rw----  1 u0_a123 u0_a123 1234 2026-10-12 12:00 main.obb
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 8) {
                val perms = parts[0]
                val isDir = perms.startsWith("d")
                // file name is the last component (or joined if spaces)
                val name = parts.subList(7, parts.size).joinToString(" ")
                if (name == "." || name == "..") continue

                val size = parts[4].toLongOrNull() ?: 0L
                val nodePath = if (directoryPath == "/") "/$name" else "$directoryPath/$name"

                nodes.add(
                    SystemNodeInfo(
                        name = name,
                        path = nodePath,
                        isDirectory = isDir,
                        size = size,
                        canRead = perms.contains("r"),
                        canWrite = perms.contains("w"),
                        lastModified = System.currentTimeMillis(),
                        permissionsStr = perms,
                        isElevatedAccess = true
                    )
                )
            } else if (parts.size >= 2) {
                // simple name fallback
                val name = parts.last()
                if (name != "." && name != "..") {
                    val nodePath = if (directoryPath == "/") "/$name" else "$directoryPath/$name"
                    nodes.add(
                        SystemNodeInfo(
                            name = name,
                            path = nodePath,
                            isDirectory = !name.contains("."),
                            size = 0L,
                            canRead = true,
                            canWrite = false,
                            lastModified = System.currentTimeMillis(),
                            permissionsStr = "rwx",
                            isElevatedAccess = true
                        )
                    )
                }
            }
        }

        return nodes
    }

    /**
     * Reads text file contents with fallback to elevated shell cat
     */
    suspend fun readFileContent(filePath: String, maxChars: Int = 100_000): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists() && file.canRead() && file.isFile) {
            try {
                val content = file.readText()
                val truncated = if (content.length > maxChars) content.take(maxChars) + "\n... [تم اقتطاع باقي الملف لكبر الحجم]" else content
                return@withContext Pair(true, truncated)
            } catch (_: Exception) {}
        }

        // Try shell cat
        val cmd = "head -c $maxChars \"$filePath\" 2>/dev/null || cat \"$filePath\""
        val result = executeShellCommand(cmd)
        if (result.isNotBlank() && !result.startsWith("cat: ") && !result.startsWith("head: ")) {
            return@withContext Pair(true, result)
        }

        Pair(false, "تعذر قراءة محتوى الملف مباشرة أو الملف فارغ. قد يتطلب تصريح ADB أو Root للوصول إلى هذا المسار المحمي.")
    }

    /**
     * Writes or overwrites a file with content, using Java File or elevated shell echo / dd / base64
     */
    suspend fun writeFileContent(filePath: String, content: String): FileOpResult = withContext(Dispatchers.IO) {
        val file = File(filePath)
        try {
            if (file.parentFile?.exists() == false) {
                file.parentFile?.mkdirs()
            }
            file.writeText(content)
            return@withContext FileOpResult.Success("تم حفظ وتحديث الملف بنجاح.")
        } catch (_: Exception) {
            // Elevated write fallback
            val b64 = android.util.Base64.encodeToString(content.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            val parentDir = file.parent ?: "/"
            val shellCmd = "mkdir -p \"$parentDir\" && echo \"$b64\" | base64 -d > \"$filePath\" 2>&1 || (echo -n \"$content\" > \"$filePath\" 2>&1)"
            val out = executeShellCommand(shellCmd)

            if (out.contains("Permission denied") || out.contains("Read-only file system")) {
                FileOpResult.Error("فشلت الكتابة في المسار: $out")
            } else {
                FileOpResult.Success("تمت كتابة الملف عبر محرك الامتيازات الشل المرتفعة.")
            }
        }
    }

    /**
     * Creates a new directory (mkdir)
     */
    suspend fun createDirectory(parentPath: String, dirName: String): FileOpResult = withContext(Dispatchers.IO) {
        val cleanParent = parentPath.trim().removeSuffix("/")
        val targetPath = "$cleanParent/${dirName.trim()}"
        val dir = File(targetPath)

        if (dir.exists()) {
            return@withContext FileOpResult.Error("المجلد موجود بالفعل بهذا الاسم.")
        }

        try {
            if (dir.mkdirs()) {
                return@withContext FileOpResult.Success("تم إنشاء المجلد بنجاح: $dirName")
            }
        } catch (_: Exception) {}

        // Fallback to shell
        val cmd = "mkdir -p \"$targetPath\""
        val out = executeShellCommand(cmd)
        if (out.contains("Permission denied") || out.contains("Read-only file system")) {
            FileOpResult.Error("تعذر إنشاء المجلد: $out")
        } else {
            FileOpResult.Success("تم إنشاء المجلد عبر مسار الأوامر.")
        }
    }

    /**
     * Creates a new blank file
     */
    suspend fun createNewFile(parentPath: String, fileName: String, initialContent: String = ""): FileOpResult = withContext(Dispatchers.IO) {
        val cleanParent = parentPath.trim().removeSuffix("/")
        val targetPath = "$cleanParent/${fileName.trim()}"
        val file = File(targetPath)

        if (file.exists()) {
            return@withContext FileOpResult.Error("الملف موجود بالفعل بهذا الاسم.")
        }

        try {
            file.parentFile?.mkdirs()
            if (file.createNewFile()) {
                if (initialContent.isNotEmpty()) file.writeText(initialContent)
                return@withContext FileOpResult.Success("تم إنشاء الملف بنجاح: $fileName")
            }
        } catch (_: Exception) {}

        // Fallback shell touch
        val cmd = "touch \"$targetPath\" 2>&1"
        val out = executeShellCommand(cmd)
        if (out.contains("Permission denied") || out.contains("Read-only file system")) {
            FileOpResult.Error("تعذر إنشاء الملف: $out")
        } else {
            if (initialContent.isNotEmpty()) {
                writeFileContent(targetPath, initialContent)
            }
            FileOpResult.Success("تم إنشاء الملف عبر محرك الشل بنجاح.")
        }
    }

    /**
     * Deletes a file or directory
     */
    suspend fun deleteNode(targetPath: String, isDirectory: Boolean): FileOpResult = withContext(Dispatchers.IO) {
        val target = File(targetPath)
        try {
            if (isDirectory) {
                if (target.deleteRecursively()) return@withContext FileOpResult.Success("تم حذف المجلد ومحتوياته.")
            } else {
                if (target.delete()) return@withContext FileOpResult.Success("تم حذف الملف بنجاح.")
            }
        } catch (_: Exception) {}

        // Fallback shell rm
        val cmd = if (isDirectory) "rm -rf \"$targetPath\" 2>&1" else "rm -f \"$targetPath\" 2>&1"
        val out = executeShellCommand(cmd)
        if (out.contains("Permission denied") || out.contains("Read-only file system")) {
            FileOpResult.Error("تعذر الحذف: $out")
        } else {
            FileOpResult.Success("تم الحذف بنجاح.")
        }
    }

    private suspend fun executeShellCommand(cmd: String): String {
        privilegeEngine?.let { engine ->
            try {
                val res = engine.execute(cmd, operationName = "File Browser Op")
                if (res.isSuccess) {
                    return res.stdout
                }
            } catch (_: Exception) {}
        }

        return try {
            val process = ProcessBuilder("sh", "-c", cmd).start()
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val text = reader.readText()
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            text
        } catch (_: Exception) {
            ""
        }
    }
}
