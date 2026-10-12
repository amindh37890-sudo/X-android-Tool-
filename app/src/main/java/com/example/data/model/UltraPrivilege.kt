package com.example.data.model

enum class RiskLevel(val label: String, val colorHex: Long, val badgeText: String) {
    CRITICAL("CRITICAL RISK", 0xFFFF1744, "خطر حرج"),
    HIGH("HIGH RISK", 0xFFFF9100, "خطر مرتفع"),
    MODERATE("MODERATE RISK", 0xFFFFEA00, "متوسط الخطورة")
}

data class UltraPrivilege(
    val id: String,
    val title: String,
    val titleArabic: String,
    val description: String,
    val descriptionArabic: String,
    val riskLevel: RiskLevel,
    val permissions: List<String>,
    val appOps: List<String>,
    val commandTemplate: String,
    val affectedSubsystems: List<String>
)

data class RiskAssessment(
    val privilege: UltraPrivilege,
    val targetPackage: String,
    val targetAppName: String,
    val isTargetTrusted: Boolean,
    val overallRiskScore: Int, // 0 - 100
    val impactAnalysis: String,
    val impactAnalysisArabic: String,
    val potentialHazards: List<String>,
    val subsystemsAffected: List<String>,
    val securityRecommendations: List<String>
)

object UltraPrivilegeRegistry {
    val privileges = listOf(
        UltraPrivilege(
            id = "silent_installer",
            title = "Silent App Installer & Uninstaller",
            titleArabic = "تثبيت وحذف التطبيقات الصامت بدون تنبيه",
            description = "Allows the target application to install, update, and remove APKs silently without Google Play or user confirmation prompts.",
            descriptionArabic = "يسمح للتطبيق بتثبيت وحذف أي حزمة في النظام بصمت تام دون إظهار نافذة تأكيد للمستخدم.",
            riskLevel = RiskLevel.CRITICAL,
            permissions = listOf(
                "android.permission.INSTALL_PACKAGES",
                "android.permission.DELETE_PACKAGES",
                "android.permission.REQUEST_INSTALL_PACKAGES"
            ),
            appOps = listOf("REQUEST_INSTALL_PACKAGES"),
            commandTemplate = "pm grant {package} android.permission.INSTALL_PACKAGES && pm grant {package} android.permission.DELETE_PACKAGES && appops set {package} REQUEST_INSTALL_PACKAGES allow",
            affectedSubsystems = listOf("Package Manager", "App Security", "Storage")
        ),
        UltraPrivilege(
            id = "media_projection",
            title = "Unrestricted Screen Projection & Media Capture",
            titleArabic = "التقاط وتسجيل الشاشة المباشر بدون إذن",
            description = "Bypasses the system MediaProjection prompt, enabling continuous background screen recording and display frame capture.",
            descriptionArabic = "تجاوز نافذة موافقة تسجيل الشاشة في أندرويد والسماح بتسجيل الشاشة بالكامل في الخلفية.",
            riskLevel = RiskLevel.CRITICAL,
            permissions = listOf("android.permission.CAPTURE_VIDEO_OUTPUT"),
            appOps = listOf("PROJECT_MEDIA"),
            commandTemplate = "cmd appops set {package} PROJECT_MEDIA allow",
            affectedSubsystems = listOf("Display Pipeline", "Privacy & Keystrokes", "Graphics")
        ),
        UltraPrivilege(
            id = "raw_storage_bypass",
            title = "Raw Filesystem & All Files Storage Bypass",
            titleArabic = "تجاوز نظام الملفات والوصول الكامل لجميع البيانات",
            description = "Grants unrestricted access across all user directories, internal app data (/sdcard/Android/data), and mounted storage volumes.",
            descriptionArabic = "وصول مباشر وكامل لجميع مجلدات الهاتف ومجلدات بيانات التطبيقات الداخلية دون قيود SAF.",
            riskLevel = RiskLevel.HIGH,
            permissions = listOf(
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.WRITE_MEDIA_STORAGE"
            ),
            appOps = listOf("MANAGE_EXTERNAL_STORAGE", "LEGACY_STORAGE"),
            commandTemplate = "cmd appops set {package} MANAGE_EXTERNAL_STORAGE allow && cmd appops set {package} LEGACY_STORAGE allow",
            affectedSubsystems = listOf("Filesystem", "Internal Storage", "Private Media")
        ),
        UltraPrivilege(
            id = "input_injection",
            title = "Low-Level Input Injection & Automation",
            titleArabic = "حقن إشارات الإدخال ومحاكاة اللمس والضغط",
            description = "Allows programmatic touch, gesture, and keyboard event injection across all foreground apps and lock screens.",
            descriptionArabic = "القدرة على محاكاة اللمسات والضغطات على الشاشة عبر النظام وتجاوز شاشات التأكيد.",
            riskLevel = RiskLevel.CRITICAL,
            permissions = listOf("android.permission.INJECT_EVENTS"),
            appOps = listOf("SYSTEM_ALERT_WINDOW"),
            commandTemplate = "pm grant {package} android.permission.INJECT_EVENTS && appops set {package} SYSTEM_ALERT_WINDOW allow",
            affectedSubsystems = listOf("Touch Controller", "Window Manager", "Accessibility")
        ),
        UltraPrivilege(
            id = "network_traffic_control",
            title = "Global Network & VPN Interception",
            titleArabic = "التحكم في حركة الشبكة وخدمات VPN عبر النظام",
            description = "Empowers the app to control system-wide network routing, establish silent VPN tunnels, and bridge connections across user profiles.",
            descriptionArabic = "التحكم في شبكة الاتصال وتوجيه البيانات وإنشاء شبكات VPN دون تنبيه مستمر.",
            riskLevel = RiskLevel.HIGH,
            permissions = listOf(
                "android.permission.CONTROL_VPN",
                "android.permission.INTERACT_ACROSS_USERS_FULL",
                "android.permission.CHANGE_NETWORK_STATE"
            ),
            appOps = listOf("ACT_AS_VPN"),
            commandTemplate = "pm grant {package} android.permission.CONTROL_VPN && pm grant {package} android.permission.INTERACT_ACROSS_USERS_FULL",
            affectedSubsystems = listOf("Network Stack", "VPN Gateway", "Multi-User Sandboxing")
        ),
        UltraPrivilege(
            id = "kernel_logcat_dump",
            title = "Full Kernel Logcat & Memory Telemetry",
            titleArabic = "استخراج سجلات النواة Logcat وذاكرة النظام كاملة",
            description = "Grants continuous access to kernel log buffers (dmesg, logcat radio/events/system), exposing sensitive inter-process communications.",
            descriptionArabic = "قراءة كافة سجلات النظام ونواة اللينكس بما فيها المحادثات بين العمليات ومعلومات الاتصال.",
            riskLevel = RiskLevel.MODERATE,
            permissions = listOf(
                "android.permission.READ_LOGS",
                "android.permission.DUMP"
            ),
            appOps = listOf("GET_USAGE_STATS"),
            commandTemplate = "pm grant {package} android.permission.READ_LOGS && pm grant {package} android.permission.DUMP",
            affectedSubsystems = listOf("Kernel Log Buffer", "System Dumpsys", "Memory Diagnostics")
        ),
        UltraPrivilege(
            id = "thermal_doze_bypass",
            title = "Hardware Thermal & Unthrottled CPU Governor",
            titleArabic = "تجاوز قيود المعالج وموفر الطاقة والحرارة",
            description = "Forces the Android battery idle and thermal subsystem to treat the app as non-throttled, locking maximum CPU frequency scaling.",
            descriptionArabic = "منع إيقاف التطبيق نهائياً وتجاوز كبح المعالج عند ارتفاع الحرارة أو انخفاض البطارية.",
            riskLevel = RiskLevel.MODERATE,
            permissions = listOf("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"),
            appOps = listOf("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND"),
            commandTemplate = "dumpsys deviceidle whitelist +{package} && am set-standby-bucket {package} active && cmd appops set {package} RUN_ANY_IN_BACKGROUND allow",
            affectedSubsystems = listOf("Battery Idle (Doze)", "CPU Governor", "Power Management")
        )
    )
}
