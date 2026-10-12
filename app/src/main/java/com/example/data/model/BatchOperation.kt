package com.example.data.model

enum class BatchActionType(
    val title: String,
    val titleArabic: String,
    val description: String,
    val iconName: String,
    val isDestructive: Boolean = false,
    val badgeColorHex: Long = 0xFF00E5FF
) {
    SUPERCHARGE_MAX_PRIVILEGES(
        title = "Bulk Supercharge Max Privileges",
        titleArabic = "ترقية قصوى مجمعة لكافة الصلاحيات",
        description = "Grants all secure permissions, enables unrestricted background AppOps, sets ACTIVE standby, and exempts from Doze.",
        iconName = "Bolt",
        badgeColorHex = 0xFF00E676
    ),
    AUTHORIZE_NEXUS(
        title = "Authorize for NEXUS X",
        titleArabic = "تفعيل صلاحيات وسيط NEXUS X",
        description = "Enables NEXUS elevated operations bridge for all selected packages.",
        iconName = "VpnKey",
        badgeColorHex = 0xFF00E5FF
    ),
    EXEMPT_BATTERY_SAVER(
        title = "Exempt from Battery Doze",
        titleArabic = "استثناء جماعي من موفر الطاقة",
        description = "Whitelists all selected applications from Android Doze background suspension.",
        iconName = "BatteryAlert",
        badgeColorHex = 0xFF00E5FF
    ),
    SET_STANDBY_ACTIVE(
        title = "Lock Standby Bucket to ACTIVE",
        titleArabic = "تثبيت وضع الاستعداد على النشط ACTIVE",
        description = "Eliminates all background scheduling and network throttling across selected apps.",
        iconName = "Speed",
        badgeColorHex = 0xFFB388FF
    ),
    FORCE_STOP(
        title = "Force Stop Selected Apps",
        titleArabic = "إيقاف إجباري للتطبيقات المحددة",
        description = "Immediately terminates all background services and processes for selected packages.",
        iconName = "Block",
        isDestructive = true,
        badgeColorHex = 0xFFFF9100
    ),
    FREEZE_APPS(
        title = "Freeze / Disable Apps",
        titleArabic = "تجميد وتعطيل التطبيقات المحددة",
        description = "Disables and hides selected applications without uninstalling them.",
        iconName = "AcUnit",
        isDestructive = true,
        badgeColorHex = 0xFFB388FF
    ),
    DEFROST_APPS(
        title = "Defrost / Enable Apps",
        titleArabic = "إلغاء تجميد وتشغيل التطبيقات",
        description = "Restores and re-enables previously frozen applications.",
        iconName = "Whatshot",
        badgeColorHex = 0xFF00E676
    ),
    REVOKE_NEXUS(
        title = "Revoke NEXUS X Authorization",
        titleArabic = "إلغاء تصريح وسيط NEXUS X",
        description = "Removes elevated bridge authorization for selected packages.",
        iconName = "Close",
        isDestructive = true,
        badgeColorHex = 0xFFFF5252
    ),
    CLEAR_DATA(
        title = "Clear App Data & Caches",
        titleArabic = "مسح بيانات التطبيقات والذاكرة المؤقتة",
        description = "Wipes all user data and runtime caches for selected packages.",
        iconName = "Delete",
        isDestructive = true,
        badgeColorHex = 0xFFFF1744
    )
}

data class BatchProgressState(
    val isExecuting: Boolean = false,
    val currentPackageName: String = "",
    val currentAppName: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val successfulCount: Int = 0,
    val failedCount: Int = 0,
    val actionType: BatchActionType? = null,
    val isCompleted: Boolean = false
) {
    val progressPercent: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}
