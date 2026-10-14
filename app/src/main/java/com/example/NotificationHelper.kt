package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Handles creation and display of system notifications for background call requests.
 */
object NotificationHelper {
    const val CHANNEL_ID = "leadzam_call_requests_channel"
    private const val NOTIFICATION_ID_BASE = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a high-priority heads-up notification for an incoming call request.
     */
    fun showCallRequestNotification(context: Context, request: CallRequest) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Tap action: Opens MainActivity with the incoming call details
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_PHONE_NUMBER, request.phoneNumber)
            putExtra(MainActivity.EXTRA_CONTACT_NAME, request.contactName)
            putExtra(MainActivity.EXTRA_REQUEST_ID, request.requestId)
            putExtra(MainActivity.EXTRA_AUTO_OPEN_DIALER, false)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            request.requestId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct action button: Opens MainActivity and triggers ACTION_DIAL immediately
        val dialActionIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_PHONE_NUMBER, request.phoneNumber)
            putExtra(MainActivity.EXTRA_CONTACT_NAME, request.contactName)
            putExtra(MainActivity.EXTRA_REQUEST_ID, request.requestId)
            putExtra(MainActivity.EXTRA_AUTO_OPEN_DIALER, true)
        }
        val dialPendingIntent = PendingIntent.getActivity(
            context,
            (request.requestId.hashCode() + 1),
            dialActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contactDisplay = if (request.contactName.isNotBlank()) request.contactName else "CRM Lead"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle("Incoming Call: $contactDisplay")
            .setContentText("Number: ${request.phoneNumber} • Tap to view in LeadZam")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "LeadZam CRM requested a phone call with $contactDisplay (${request.phoneNumber}). Tap Call to open your phone dialer."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_phone, "Open Dialer", dialPendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE, builder.build())
        } catch (e: SecurityException) {
            // Handled if permission revoked
        }
    }
}
