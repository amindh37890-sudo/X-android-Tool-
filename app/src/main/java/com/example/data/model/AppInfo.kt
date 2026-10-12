package com.example.data.model

import android.graphics.drawable.Drawable

data class SensitivePermission(
    val permission: String,
    val simpleName: String,
    val description: String,
    val isGranted: Boolean,
    val isElevatedGrantable: Boolean = true
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val uid: Int,
    val targetSdkVersion: Int,
    val isSystemApp: Boolean,
    val isTermux: Boolean,
    val isEnabled: Boolean,
    val isElevatedAuthorized: Boolean,
    val icon: Drawable? = null,
    val permissions: List<SensitivePermission> = emptyList(),
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val apkPath: String = ""
)
