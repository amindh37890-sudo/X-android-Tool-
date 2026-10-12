package com.example.data.model

data class ElevatedPreset(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val commandTemplate: String,
    val requiredPrivilege: PrivilegeLevel = PrivilegeLevel.LOCAL_ADB,
    val requiresTargetPackage: Boolean = false
)

object PresetRegistry {
    val presets = listOf(
        ElevatedPreset(
            id = "termux_elevate",
            title = "Termux Full Bridge Elevation",
            category = "Terminal & Dev",
            description = "Grants WRITE_SECURE_SETTINGS, DUMP, and SYSTEM_ALERT_WINDOW to Termux.",
            commandTemplate = "pm grant com.termux android.permission.WRITE_SECURE_SETTINGS && pm grant com.termux android.permission.DUMP && appops set com.termux SYSTEM_ALERT_WINDOW allow",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB,
            requiresTargetPackage = false
        ),
        ElevatedPreset(
            id = "refresh_rate_high",
            title = "Enforce Max Refresh Rate (120Hz)",
            category = "Display Tweaks",
            description = "Locks screen refresh rate minimum and peak to 120Hz for zero stutter.",
            commandTemplate = "settings put system min_refresh_rate 120.0 && settings put system peak_refresh_rate 120.0",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB
        ),
        ElevatedPreset(
            id = "anim_speedup",
            title = "Speed Up System Animations (0.5x)",
            category = "Performance",
            description = "Accelerates window, transition, and animator durations across Android.",
            commandTemplate = "settings put global window_animation_scale 0.5 && settings put global transition_animation_scale 0.5 && settings put global animator_duration_scale 0.5",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB
        ),
        ElevatedPreset(
            id = "battery_whitelist",
            title = "Exempt from Battery Optimization",
            category = "Background Execution",
            description = "Prevents Android Doze from suspending background worker services.",
            commandTemplate = "dumpsys deviceidle whitelist +{package}",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB,
            requiresTargetPackage = true
        ),
        ElevatedPreset(
            id = "appops_bg_run",
            title = "Allow Unrestricted Background Execution",
            category = "AppOps Management",
            description = "Configures RUN_IN_BACKGROUND and RUN_ANY_IN_BACKGROUND to allow.",
            commandTemplate = "cmd appops set {package} RUN_IN_BACKGROUND allow && cmd appops set {package} RUN_ANY_IN_BACKGROUND allow",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB,
            requiresTargetPackage = true
        ),
        ElevatedPreset(
            id = "system_ui_tuner",
            title = "Unlock System UI Tuner",
            category = "System Settings",
            description = "Reveals hidden System UI Tuner settings and status bar icon toggles.",
            commandTemplate = "pm enable com.android.systemui/.tuner.TunerActivity",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB
        ),
        ElevatedPreset(
            id = "compile_speed",
            title = "Force ART Speed Compilation",
            category = "Optimization",
            description = "Compiles application DEX ahead-of-time (speed profile) for instant launches.",
            commandTemplate = "cmd package compile -m speed -f {package}",
            requiredPrivilege = PrivilegeLevel.LOCAL_ADB,
            requiresTargetPackage = true
        )
    )
}
