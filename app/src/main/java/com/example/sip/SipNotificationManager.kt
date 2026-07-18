package com.example.sip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object SipNotificationManager {
    private const val CHANNEL_ID = "secure_sip_calls"
    private const val CHANNEL_NAME = "Incoming Secure Calls"
    private const val NOTIFICATION_ID = 44101

    fun showIncomingCallNotification(context: Context, callerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming secure peer-to-peer discussions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Set up the intent that triggers when user clicks the notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("LAUNCH_TAB", "incoming_call")
            putExtra("CALLER_NAME", callerName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build a highly tactical heads-up style secure alert notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // standard secure call icon
            .setContentTitle("Incoming SECURE SIP Call")
            .setContentText("E2E Encrypted discussion with $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Makes it show up as high priority heads-up
            .setStyle(NotificationCompat.BigTextStyle().bigText("E2E encrypted discussion with $callerName\nHandshake key: AES_GCM_256 (256-bit Key).\nTap to join secure session."))
            .setColor(0xFF00E676.toInt()) // Cyber Green theme accent

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
