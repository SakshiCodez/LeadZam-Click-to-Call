package com.example

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

/**
 * Service that receives Firebase Cloud Messaging pushes for LeadZam.
 *
 * Designed to handle pure "data" messages so that onMessageReceived is triggered
 * in BOTH foreground and background states without system tray interception.
 */
class LeadZamFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "LeadZamFCM"
        const val MESSAGE_TYPE_DIAL_REQUEST = "dial_request"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM registration token generated/refreshed: $token")
        CallRequestRepository.init(applicationContext)
        CallRequestRepository.setFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "Received FCM message with empty data payload. Ignoring.")
            return
        }

        val type = data["type"] ?: ""
        if (type != MESSAGE_TYPE_DIAL_REQUEST) {
            Log.w(TAG, "Ignored message with unexpected type: '$type'. Expected '$MESSAGE_TYPE_DIAL_REQUEST'.")
            return
        }

        val rawPhoneNumber = data["phoneNumber"] ?: data["phone"] ?: ""
        val contactName = data["contactName"] ?: data["name"] ?: "CRM Contact"
        val requestId = data["requestId"] ?: UUID.randomUUID().toString()

        if (!CallRequest.isValidPhoneNumber(rawPhoneNumber)) {
            Log.e(TAG, "Invalid phone number in FCM payload: '$rawPhoneNumber'. Request dropped.")
            return
        }

        val callRequest = CallRequest(
            phoneNumber = rawPhoneNumber,
            contactName = contactName,
            requestId = requestId,
            timestamp = System.currentTimeMillis()
        )

        CallRequestRepository.init(applicationContext)
        CallRequestRepository.postCallRequest(callRequest)

        // If the app is currently in background or killed, post an interactive system notification
        val isForeground = CallRequestRepository.isAppInForeground.get()
        Log.i(TAG, "Processed valid dial_request for $contactName ($rawPhoneNumber). Foreground: $isForeground")

        if (!isForeground) {
            NotificationHelper.showCallRequestNotification(applicationContext, callRequest)
        }
    }
}
