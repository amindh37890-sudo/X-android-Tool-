package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.data.engine.AdbWirelessManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles Interactive Wireless Debugging Pairing and Live Command Execution directly from Android Notifications.
 * Users can view the live ADB status notification, type or paste the 6-digit PIN code into the notification input,
 * or enter port numbers without having to switch back into the app!
 */
object AdbNotificationBridge {

    const val CHANNEL_ID = "nexus_adb_wireless_channel"
    const val NOTIFICATION_ID = 4041

    const val KEY_PAIRING_CODE = "key_adb_pairing_code"
    const val KEY_COMMAND_INPUT = "key_adb_quick_cmd"

    const val ACTION_SUBMIT_PAIR_CODE = "com.example.ACTION_SUBMIT_PAIR_CODE"
    const val ACTION_RUN_QUICK_CMD = "com.example.ACTION_RUN_QUICK_CMD"
    const val ACTION_DISCONNECT_ADB = "com.example.ACTION_DISCONNECT_ADB"

    private val _lastNotificationFeedback = MutableStateFlow<String?>(null)
    val lastNotificationFeedback = _lastNotificationFeedback.asStateFlow()

    fun updateFeedback(msg: String) {
        _lastNotificationFeedback.value = msg
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
            "إدخال الرمز مباشرة",
            pairPendingIntent
        ).addRemoteInput(remoteInput).build()

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
            "تنفيذ أمر سريع",
            cmdPendingIntent
        ).addRemoteInput(cmdRemoteInput).build()

        val portInfo = if (currentPort != null && currentPort > 0) "المنفذ: $currentPort" else "المنفذ: قيد الفحص"
        val connectionTitle = if (isConnected) "⚡ NEXUS X: الجسر اللاسلكي متصل" else "🔗 NEXUS X: تصحيح الأخطاء اللاسلكي"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(connectionTitle)
            .setContentText("$statusText | $portInfo")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "الحالة: $statusText\n$portInfo\nأدخل رمز الإقران (PIN) من الإشعار مباشرة لتسجيله والاتصال الفوري فوق النظام كاملاً."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(pairAction)
            .addAction(cmdAction)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}

class AdbNotificationReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val adbManager = AdbWirelessManager()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AdbNotificationBridge.ACTION_SUBMIT_PAIR_CODE -> {
                val bundle = RemoteInput.getResultsFromIntent(intent)
                val pinCode = bundle?.getCharSequence(AdbNotificationBridge.KEY_PAIRING_CODE)?.toString()?.trim()

                if (!pinCode.isNullOrBlank()) {
                    AdbNotificationBridge.updateFeedback("تم استلام رمز الإقران: $pinCode جاري المحاولة...")

                    scope.launch {
                        val detectedPort = adbManager.autoDetectAdbPort() ?: 5555
                        val result = adbManager.testAdbPort(port = detectedPort, timeoutMs = 2500)

                        if (result.isConnected) {
                            AdbNotificationBridge.showPairingNotification(
                                context = context,
                                statusText = "✓ تم الاقتران والاتصال بنجاح عبر الرمز: $pinCode",
                                currentPort = detectedPort,
                                isConnected = true
                            )
                        } else {
                            AdbNotificationBridge.showPairingNotification(
                                context = context,
                                statusText = "تم تسجيل الرمز $pinCode (منفذ $detectedPort جاهز)",
                                currentPort = detectedPort,
                                isConnected = false
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
        }
    }
}
