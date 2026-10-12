package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.data.engine.AdbWirelessManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles Interactive Wireless Debugging Pairing and Live Command Execution directly from Android Notifications.
 * Users can view the live ADB status notification, type or paste the 6-digit PIN code into the notification input,
 * open Developer Options directly from the notification, and establish live connection across the entire system.
 */
object AdbNotificationBridge {

    const val CHANNEL_ID = "nexus_adb_wireless_channel"
    const val NOTIFICATION_ID = 4041

    const val KEY_PAIRING_CODE = "key_adb_pairing_code"
    const val KEY_COMMAND_INPUT = "key_adb_quick_cmd"

    const val ACTION_SUBMIT_PAIR_CODE = "com.example.ACTION_SUBMIT_PAIR_CODE"
    const val ACTION_RUN_QUICK_CMD = "com.example.ACTION_RUN_QUICK_CMD"
    const val ACTION_DISCONNECT_ADB = "com.example.ACTION_DISCONNECT_ADB"

    data class PairingEvent(
        val isSuccess: Boolean,
        val pinCode: String,
        val port: Int,
        val message: String
    )

    private val _lastNotificationFeedback = MutableStateFlow<String?>(null)
    val lastNotificationFeedback = _lastNotificationFeedback.asStateFlow()

    private val _pairingEvents = MutableSharedFlow<PairingEvent>(extraBufferCapacity = 10)
    val pairingEvents = _pairingEvents.asSharedFlow()

    fun updateFeedback(msg: String) {
        _lastNotificationFeedback.value = msg
    }

    suspend fun emitPairingEvent(event: PairingEvent) {
        _pairingEvents.emit(event)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NEXUS Wireless Debugging Bridge"
            val descriptionText = "Interactive notification for Wireless Debugging PIN pairing and elevated execution"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPairingNotification(
        context: Context,
        statusText: String = "جاهز للإقران اللاسلكي",
        currentPort: Int? = null,
        isConnected: Boolean = false
    ) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteInput for 6-Digit Pairing PIN Code
        val remoteInput = RemoteInput.Builder(KEY_PAIRING_CODE)
            .setLabel("أدخل رمز الإقران (6 أرقام PIN)")
            .build()

        // PendingIntent for BroadcastReceiver handling PIN submission
        val pairBroadcastIntent = Intent(context, AdbNotificationReceiver::class.java).apply {
            action = ACTION_SUBMIT_PAIR_CODE
        }
        val pairPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            pairBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val pairAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "إدخال الرمز مباشرة ⚡",
            pairPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Action to open Developer Options directly
        val devSettingsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val devSettingsPendingIntent = PendingIntent.getActivity(
            context,
            3,
            devSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val devSettingsAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_preferences,
            "خيارات المطور ⚙️",
            devSettingsPendingIntent
        ).build()

        // Quick Command RemoteInput
        val cmdRemoteInput = RemoteInput.Builder(KEY_COMMAND_INPUT)
            .setLabel("تنفيذ أمر شل فوري")
            .build()

        val cmdBroadcastIntent = Intent(context, AdbNotificationReceiver::class.java).apply {
            action = ACTION_RUN_QUICK_CMD
        }
        val cmdPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            cmdBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val cmdAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_play,
            "أمر شل سريع 🚀",
            cmdPendingIntent
        ).addRemoteInput(cmdRemoteInput).build()

        // Disconnect Action
        val disconnectBroadcastIntent = Intent(context, AdbNotificationReceiver::class.java).apply {
            action = ACTION_DISCONNECT_ADB
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            disconnectBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "قطع الاتصال ✕",
            disconnectPendingIntent
        ).build()

        val portInfo = if (currentPort != null && currentPort > 0) "المنفذ: $currentPort" else "المنفذ: قيد الفحص التلقائي"
        val connectionTitle = if (isConnected) "⚡ NEXUS X: الجسر اللاسلكي متصل بنجاح" else "🔗 NEXUS X: اقتران تصحيح الأخطاء اللاسلكي"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(connectionTitle)
            .setContentText("$statusText | $portInfo")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "الحالة: $statusText\n$portInfo\n" +
                if (isConnected) {
                    "✓ الجسر نشط بصلاحيات عليا فوق النظام كاملاً. يمكنك تنفيذ أوامر شل فورية من الإشعار مباشرة."
                } else {
                    "اضغط 'إدخال الرمز مباشرة' واكتب الرمز (PIN) المكون من 6 أرقام للاتصال الفوري بدون كمبيوتر."
                }
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        if (!isConnected) {
            builder.addAction(pairAction)
            builder.addAction(devSettingsAction)
        } else {
            builder.addAction(cmdAction)
            builder.addAction(disconnectAction)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}

class AdbNotificationReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val adbManager = AdbWirelessManager.default

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AdbNotificationBridge.ACTION_SUBMIT_PAIR_CODE -> {
                val bundle = RemoteInput.getResultsFromIntent(intent)
                val pinCode = bundle?.getCharSequence(AdbNotificationBridge.KEY_PAIRING_CODE)?.toString()?.trim()

                if (!pinCode.isNullOrBlank()) {
                    AdbNotificationBridge.updateFeedback("تم استلام رمز الإقران: $pinCode جاري الاقتران والاتصال التلقائي...")
                    AdbNotificationBridge.showPairingNotification(
                        context = context,
                        statusText = "جاري الاقتران بالرمز: $pinCode وتأكيد الاتصال... ⏳",
                        currentPort = null,
                        isConnected = false
                    )

                    scope.launch {
                        // Perform real pairing & connection
                        val pairResult = adbManager.pairDevice(
                            host = "127.0.0.1",
                            pairPort = null,
                            pairingCode = pinCode
                        )

                        if (pairResult.isSuccess) {
                            AdbNotificationBridge.updateFeedback(pairResult.message)
                            AdbNotificationBridge.showPairingNotification(
                                context = context,
                                statusText = "✓ تم الاقتران والاتصال بنجاح عبر الرمز: $pinCode",
                                currentPort = pairResult.activePort,
                                isConnected = true
                            )
                            AdbNotificationBridge.emitPairingEvent(
                                AdbNotificationBridge.PairingEvent(
                                    isSuccess = true,
                                    pinCode = pinCode,
                                    port = pairResult.activePort,
                                    message = pairResult.message
                                )
                            )
                        } else {
                            AdbNotificationBridge.updateFeedback("فشل الاقتران: ${pairResult.message}")
                            AdbNotificationBridge.showPairingNotification(
                                context = context,
                                statusText = "فشل بالرمز $pinCode: ${pairResult.message}",
                                currentPort = pairResult.activePort,
                                isConnected = false
                            )
                            AdbNotificationBridge.emitPairingEvent(
                                AdbNotificationBridge.PairingEvent(
                                    isSuccess = false,
                                    pinCode = pinCode,
                                    port = pairResult.activePort,
                                    message = pairResult.message
                                )
                            )
                        }
                    }
                }
            }

            AdbNotificationBridge.ACTION_RUN_QUICK_CMD -> {
                val bundle = RemoteInput.getResultsFromIntent(intent)
                val cmd = bundle?.getCharSequence(AdbNotificationBridge.KEY_COMMAND_INPUT)?.toString()?.trim()

                if (!cmd.isNullOrBlank()) {
                    scope.launch {
                        val detectedPort = adbManager.autoDetectAdbPort() ?: 5555
                        val result = adbManager.executeOverAdbSocket("127.0.0.1", detectedPort, cmd)
                        val summary = if (result.first == 0) "نجح الأمر: ${cmd.take(15)}..." else "فشل: ${result.second.take(20)}"

                        AdbNotificationBridge.showPairingNotification(
                            context = context,
                            statusText = summary,
                            currentPort = detectedPort,
                            isConnected = true
                        )
                    }
                }
            }

            AdbNotificationBridge.ACTION_DISCONNECT_ADB -> {
                AdbNotificationBridge.updateFeedback("تم قطع اتصال الجسر اللاسلكي")
                AdbNotificationBridge.showPairingNotification(
                    context = context,
                    statusText = "تم قطع الاتصال - جاهز لإعادة الاقتران",
                    currentPort = null,
                    isConnected = false
                )
                scope.launch {
                    AdbNotificationBridge.emitPairingEvent(
                        AdbNotificationBridge.PairingEvent(
                            isSuccess = false,
                            pinCode = "",
                            port = 0,
                            message = "Disconnected"
                        )
                    )
                }
            }
        }
    }
}
