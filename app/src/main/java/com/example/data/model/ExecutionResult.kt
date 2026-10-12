package com.example.data.model

data class ExecutionResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val privilegeLevel: PrivilegeLevel,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean
        get() = exitCode == 0

    val combinedOutput: String
        get() = buildString {
            if (stdout.isNotBlank()) append(stdout.trim())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("[STDERR]: ").append(stderr.trim())
            }
            if (isEmpty()) append("[Process exited with code $exitCode]")
        }
}
