package com.example.data.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class AdbWirelessManager {

    data class AdbConnectionState(
        val isConnected: Boolean = false,
        val host: String = "127.0.0.1",
        val port: Int = 5555,
        val latencyMs: Long = 0L,
        val message: String = "Not connected"
    )

    suspend fun testAdbPort(host: String = "127.0.0.1", port: Int, timeoutMs: Int = 1200): AdbConnectionState =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    val latency = System.currentTimeMillis() - startTime
                    AdbConnectionState(
                        isConnected = true,
                        host = host,
                        port = port,
                        latencyMs = latency,
                        message = "ADB daemon listening on $host:$port ($latency ms)"
                    )
                }
            } catch (e: Exception) {
                AdbConnectionState(
                    isConnected = false,
                    host = host,
                    port = port,
                    latencyMs = 0L,
                    message = "Port $port unreachable: ${e.message ?: "Connection refused"}"
                )
            }
        }

    suspend fun autoDetectAdbPort(): Int? = withContext(Dispatchers.IO) {
        val commonPorts = listOf(5555, 37099, 39999, 41235, 42123, 44444)
        for (port in commonPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 300)
                    return@withContext port
                }
            } catch (_: Exception) {
                // Continue scanning
            }
        }
        null
    }

    /**
     * Executes a direct shell command over the ADB daemon socket.
     * Uses framed ADB packet "0000shell:<cmd>\n" or direct raw service socket.
     */
    suspend fun executeOverAdbSocket(
        host: String,
        port: Int,
        shellCommand: String,
        timeoutMs: Int = 5000
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), 2000)
                val out: OutputStream = socket.getOutputStream()
                val input: InputStream = socket.getInputStream()

                // Format framed request: length in 4-hex chars followed by command
                val serviceRequest = "shell:$shellCommand\n"
                val framedRequest = String.format("%04X%s", serviceRequest.length, serviceRequest)

                // Write framed request
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
                        // Filter out ADB OKAY / FAIL header if present
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
                if (finalOutput.isEmpty()) {
                    Pair(0, "[Command executed successfully via ADB socket - no stdout returned]")
                } else {
                    Pair(0, finalOutput)
                }
            }
        } catch (e: Exception) {
            Pair(1, "ADB Socket Error: ${e.message ?: "Failed to connect to $host:$port"}")
        }
    }
}
