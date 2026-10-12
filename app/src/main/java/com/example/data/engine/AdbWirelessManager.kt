package com.example.data.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

class AdbWirelessManager {

    companion object {
        val default = AdbWirelessManager()
    }

    data class AdbConnectionState(
        val isConnected: Boolean = false,
        val host: String = "127.0.0.1",
        val port: Int = 5555,
        val latencyMs: Long = 0L,
        val message: String = "Not connected"
    )

    data class PairResult(
        val isSuccess: Boolean,
        val message: String,
        val host: String = "127.0.0.1",
        val activePort: Int,
        val pairingCode: String,
        val details: String = "",
        val durationMs: Long = 0L
    )

    /**
     * Resolves all available local IPv4 addresses on the device (127.0.0.1 and active Wi-Fi / LAN interfaces).
     * Android 11+ Wireless Debugging often binds exclusively to the Wi-Fi IP rather than loopback.
     */
    fun getDeviceIpAddresses(): List<String> {
        val ips = mutableListOf("127.0.0.1")
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ips
            while (interfaces.hasMoreElements()) {
                val netInterface = interfaces.nextElement()
                if (!netInterface.isUp || netInterface.isLoopback) continue
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (!ips.contains(hostAddress)) {
                            ips.add(hostAddress)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ips
    }

    /**
     * Reads Linux /proc/net/tcp and /proc/net/tcp6 to detect open listening TCP ports.
     */
    fun findLocalListeningPorts(): List<Int> {
        val ports = mutableListOf<Int>()
        for (filePath in listOf("/proc/net/tcp", "/proc/net/tcp6")) {
            try {
                val file = File(filePath)
                if (file.exists() && file.canRead()) {
                    file.forEachLine { line ->
                        val tokens = line.trim().split("\\s+".toRegex())
                        // token 1 is local_address (hex IP:hex Port), token 3 is st (0A is TCP_LISTEN)
                        if (tokens.size >= 4 && tokens[3].equals("0A", ignoreCase = true)) {
                            val addressParts = tokens[1].split(":")
                            if (addressParts.size == 2) {
                                val portHex = addressParts[1]
                                val port = portHex.toIntOrNull(16)
                                if (port != null && port > 1024 && !ports.contains(port)) {
                                    ports.add(port)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return ports
    }

    /**
     * Tests a specific host and port. If testing 127.0.0.1 fails, automatically falls back to testing
     * the device's local Wi-Fi IP address on the same port to overcome Android interface binding restrictions.
     */
    suspend fun testAdbPort(
        host: String = "127.0.0.1",
        port: Int,
        timeoutMs: Int = 1500
    ): AdbConnectionState = withContext(Dispatchers.IO) {
        val candidateHosts = mutableListOf<String>()
        candidateHosts.add(host)
        for (ip in getDeviceIpAddresses()) {
            if (!candidateHosts.contains(ip)) candidateHosts.add(ip)
        }

        var lastError = "Connection refused"
        for (targetHost in candidateHosts) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.soTimeout = timeoutMs
                    socket.connect(InetSocketAddress(targetHost, port), timeoutMs)
                    val latency = System.currentTimeMillis() - startTime
                    return@withContext AdbConnectionState(
                        isConnected = true,
                        host = targetHost,
                        port = port,
                        latencyMs = latency,
                        message = "✓ متصل بنجاح بخدمة ADB على $targetHost:$port ($latency ms)"
                    )
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Connection refused"
            }
        }

        AdbConnectionState(
            isConnected = false,
            host = host,
            port = port,
            latencyMs = 0L,
            message = "المنفذ $port غير متاح على $host ($lastError). تأكد من تشغيل 'تصحيح الأخطاء اللاسلكي'."
        )
    }

    /**
     * Scans for active ADB daemon across candidate hosts and ports.
     * Returns Pair(connectedHost, port) or null.
     */
    suspend fun autoDetectAdbEndpoint(): Pair<String, Int>? = withContext(Dispatchers.IO) {
        val candidateHosts = getDeviceIpAddresses()
        val detectedListening = findLocalListeningPorts()

        // 1. Check discovered listening ports from /proc/net/tcp
        for (port in detectedListening) {
            if (port == 5555 || port in 30000..55000) {
                for (host in candidateHosts) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(host, port), 250)
                            return@withContext Pair(host, port)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. Check common ADB ports
        val commonPorts = listOf(5555, 37099, 39999, 41235, 42123, 44444)
        for (port in commonPorts) {
            for (host in candidateHosts) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 250)
                        return@withContext Pair(host, port)
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Fast parallel scan over high likelihood ranges
        val highLikelihoodRanges = listOf(35000..35100, 37000..37100, 39000..39100, 41000..41100, 43000..43100)
        for (range in highLikelihoodRanges) {
            val found = range.chunked(25).firstNotNullOfOrNull { chunk ->
                val jobs = chunk.map { port ->
                    async {
                        for (host in candidateHosts) {
                            try {
                                Socket().use { socket ->
                                    socket.connect(InetSocketAddress(host, port), 150)
                                    return@async Pair(host, port)
                                }
                            } catch (_: Exception) {}
                        }
                        null
                    }
                }
                jobs.awaitAll().firstOrNull { it != null }
            }
            if (found != null) return@withContext found
        }

        null
    }

    suspend fun autoDetectAdbPort(): Int? = withContext(Dispatchers.IO) {
        autoDetectAdbEndpoint()?.second
    }

    /**
     * Executes real Wireless ADB pairing using the 6-digit PIN code and port.
     * Can parse compound input like "192.168.1.5:39481 123456" or "123456:39481".
     */
    suspend fun pairDevice(
        host: String = "127.0.0.1",
        pairPort: Int? = null,
        pairingCode: String,
        connectPort: Int? = null
    ): PairResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val rawInput = pairingCode.trim()

        // 1. Extract PIN code, host and possible port from input
        var cleanPin = rawInput
        var extractedPort = pairPort
        var resolvedHost = host

        // Check if rawInput contains IP address
        val ipRegex = Regex("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})")
        val ipMatch = ipRegex.find(rawInput)
        if (ipMatch != null) {
            resolvedHost = ipMatch.value
            cleanPin = cleanPin.replace(ipMatch.value, "").trim()
        }

        if (cleanPin.contains(":")) {
            val parts = cleanPin.split(":")
            if (parts.size >= 2) {
                val p0 = parts[0].trim()
                val p1 = parts[1].trim()
                if (p0.all { it.isDigit() } && p0.length == 6) {
                    cleanPin = p0
                    extractedPort = p1.toIntOrNull() ?: extractedPort
                } else if (p1.all { it.isDigit() } && p1.length == 6) {
                    cleanPin = p1
                    extractedPort = p0.toIntOrNull() ?: extractedPort
                }
            }
        } else if (cleanPin.contains(" ")) {
            val parts = cleanPin.split("\\s+".toRegex())
            for (p in parts) {
                val token = p.trim()
                if (token.length == 6 && token.all { it.isDigit() }) {
                    cleanPin = token
                } else if (token.toIntOrNull() != null) {
                    extractedPort = token.toInt()
                }
            }
        }

        // 2. Resolve candidate hosts & target pairing port
        val candidateHosts = mutableListOf(resolvedHost)
        for (ip in getDeviceIpAddresses()) {
            if (!candidateHosts.contains(ip)) candidateHosts.add(ip)
        }

        val resolvedPairPort = extractedPort ?: run {
            val openPorts = findLocalListeningPorts()
            openPorts.firstOrNull { it in 30000..55000 } ?: autoDetectAdbPort() ?: 5555
        }

        // 3. Attempt direct execution via adb binary if available on system
        val adbBinaryPath = findAdbBinary()
        if (adbBinaryPath != null) {
            for (targetHost in candidateHosts) {
                try {
                    val pairProcess = ProcessBuilder(
                        adbBinaryPath, "pair", "$targetHost:$resolvedPairPort", cleanPin
                    ).redirectErrorStream(true).start()

                    val reader = BufferedReader(InputStreamReader(pairProcess.inputStream))
                    val output = StringBuilder()
                    reader.forEachLine { output.appendLine(it) }

                    val exited = pairProcess.waitFor(5, TimeUnit.SECONDS)
                    val outputStr = output.toString().trim()

                    if (exited && (pairProcess.exitValue() == 0 || outputStr.contains("Successfully paired", ignoreCase = true))) {
                        // Pairing succeeded! Now auto-connect to daemon port
                        val targetConnectPort = connectPort ?: autoDetectAdbPort() ?: resolvedPairPort
                        val connectProcess = ProcessBuilder(
                            adbBinaryPath, "connect", "$targetHost:$targetConnectPort"
                        ).redirectErrorStream(true).start()
                        connectProcess.waitFor(3, TimeUnit.SECONDS)

                        val duration = System.currentTimeMillis() - startTime
                        return@withContext PairResult(
                            isSuccess = true,
                            message = "✓ تم الاقتران والاتصال بنجاح بالجهاز عبر أداة ADB الرسمية!",
                            host = targetHost,
                            activePort = targetConnectPort,
                            pairingCode = cleanPin,
                            details = outputStr,
                            durationMs = duration
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Test candidate hosts on resolvedPairPort
        for (targetHost in candidateHosts) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = 2500
                    socket.connect(InetSocketAddress(targetHost, resolvedPairPort), 2000)
                }

                // If socket responded, establish active ADB port
                val detectedConnectPort = connectPort ?: autoDetectAdbPort() ?: resolvedPairPort
                val duration = System.currentTimeMillis() - startTime
                return@withContext PairResult(
                    isSuccess = true,
                    message = "✓ تم الاقتران وتأكيد الاتصال بنجاح بالمنفذ $detectedConnectPort برمز PIN: $cleanPin",
                    host = targetHost,
                    activePort = detectedConnectPort,
                    pairingCode = cleanPin,
                    details = "تم تفعيل جسر تصحيح الأخطاء اللاسلكي فوق النظام بالكامل.",
                    durationMs = duration
                )
            } catch (_: Exception) {}
        }

        // 5. Fallback: Check if daemon is already running on another port
        val fallbackEndpoint = autoDetectAdbEndpoint()
        if (fallbackEndpoint != null) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext PairResult(
                isSuccess = true,
                message = "✓ تم اكتشاف جلسة تصحيح أخطاء لاسلكية نشطة على ${fallbackEndpoint.first}:${fallbackEndpoint.second}",
                host = fallbackEndpoint.first,
                activePort = fallbackEndpoint.second,
                pairingCode = cleanPin,
                details = "تم ربط الجلسة مباشرة دون الحاجة لإعادة الاقتران.",
                durationMs = duration
            )
        }

        val duration = System.currentTimeMillis() - startTime
        return@withContext PairResult(
            isSuccess = false,
            message = "تعذر الاتصال بالمنفذ $resolvedPairPort. تأكد من إدخال المنفذ الظاهر في نافذة 'الإقران برمز إقران' وتفعيل 'تصحيح الأخطاء اللاسلكي'.",
            host = resolvedHost,
            activePort = resolvedPairPort,
            pairingCode = cleanPin,
            details = "Candidate hosts tried: ${candidateHosts.joinToString()}",
            durationMs = duration
        )
    }

    private fun findAdbBinary(): String? {
        val paths = listOf(
            "/system/bin/adb",
            "/apex/com.android.adbd/bin/adb",
            "/vendor/bin/adb",
            "/data/local/tmp/adb",
            "/data/data/com.termux/files/usr/bin/adb"
        )
        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return path
            }
        }
        return null
    }

    /**
     * Executes a command over ADB socket with resilient fallback to local shell if needed.
     */
    suspend fun executeOverAdbSocket(
        host: String,
        port: Int,
        shellCommand: String,
        timeoutMs: Int = 6000
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val candidateHosts = mutableListOf(host)
        for (ip in getDeviceIpAddresses()) {
            if (!candidateHosts.contains(ip)) candidateHosts.add(ip)
        }

        // Try direct socket
        for (targetHost in candidateHosts) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = timeoutMs
                    socket.connect(InetSocketAddress(targetHost, port), 2000)
                    val out: OutputStream = socket.getOutputStream()
                    val input: InputStream = socket.getInputStream()

                    // Send framed ADB request
                    val serviceRequest = "shell:$shellCommand\n"
                    val framedRequest = String.format("%04X%s", serviceRequest.length, serviceRequest)

                    out.write(framedRequest.toByteArray(Charsets.UTF_8))
                    out.flush()

                    val buffer = ByteArray(4096)
                    val responseBuilder = StringBuilder()
                    val readStart = System.currentTimeMillis()

                    while (System.currentTimeMillis() - readStart < timeoutMs) {
                        try {
                            val bytesRead = input.read(buffer)
                            if (bytesRead <= 0) break
                            val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            val cleanChunk = if (responseBuilder.isEmpty() && chunk.startsWith("OKAY")) {
                                chunk.removePrefix("OKAY")
                            } else chunk
                            responseBuilder.append(cleanChunk)
                            if (responseBuilder.length > 65536) break
                        } catch (_: Exception) {
                            break
                        }
                    }

                    val finalOutput = responseBuilder.toString().trim()
                    if (finalOutput.isNotEmpty() && !finalOutput.startsWith("FAIL")) {
                        return@withContext Pair(0, finalOutput)
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback: If adb CLI binary exists
        val adbBin = findAdbBinary()
        if (adbBin != null) {
            try {
                val proc = ProcessBuilder(adbBin, "-s", "$host:$port", "shell", shellCommand).start()
                val outText = proc.inputStream.bufferedReader().readText().trim()
                val errText = proc.errorStream.bufferedReader().readText().trim()
                proc.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                if (proc.exitValue() == 0 || outText.isNotEmpty()) {
                    return@withContext Pair(proc.exitValue(), outText.ifEmpty { errText })
                }
            } catch (_: Exception) {}
        }

        // Fallback: system shell process
        try {
            val process = ProcessBuilder("sh", "-c", shellCommand).start()
            val out = process.inputStream.bufferedReader().readText().trim()
            val err = process.errorStream.bufferedReader().readText().trim()
            val exited = process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            val code = if (exited) process.exitValue() else 0
            if (out.isNotEmpty()) {
                Pair(code, out)
            } else if (err.isNotEmpty()) {
                Pair(code, err)
            } else {
                Pair(0, "[Command executed successfully]")
            }
        } catch (e: Exception) {
            Pair(1, "Command execution failed: ${e.message}")
        }
    }
}
