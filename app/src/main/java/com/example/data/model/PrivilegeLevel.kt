package com.example.data.model

enum class PrivilegeLevel(
    val title: String,
    val description: String,
    val badgeColorHex: Long
) {
    ROOT(
        title = "ROOT (SUPERUSER)",
        description = "Highest kernel privilege via su binary. Direct hardware and system control.",
        badgeColorHex = 0xFF00E676 // Bright neon green
    ),
    WIRELESS_ADB(
        title = "WIRELESS DEBUGGING",
        description = "Elevated ADB daemon bridge active via Wi-Fi loopback / pairing port.",
        badgeColorHex = 0xFF00E5FF // Cyan
    ),
    LOCAL_ADB(
        title = "LOCAL ADB DAEMON",
        description = "ADB server active on device TCP port (5555/local bridge).",
        badgeColorHex = 0xFFB388FF // Electric violet
    ),
    APP_SHELL(
        title = "UNPRIVILEGED SHELL",
        description = "Local standard process shell (sh). Limited to sandbox permissions.",
        badgeColorHex = 0xFFFFD740 // Amber
    ),
    RESTRICTED(
        title = "RESTRICTED",
        description = "No execution bridge detected. Enable Wireless Debugging or Root.",
        badgeColorHex = 0xFFFF5252 // Red
    )
}
