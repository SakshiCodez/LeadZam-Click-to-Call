package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Handles intent launching for the native Android phone dialer.
 *
 * CRITICAL SAFETY REQUIREMENT:
 * This helper ONLY uses Intent.ACTION_DIAL with a properly formatted tel: URI.
 * It NEVER uses Intent.ACTION_CALL (which requires runtime CALL_PHONE permission
 * and places calls automatically). The dialer is opened with the number pre-filled,
 * requiring explicit user interaction to place the call.
 */
object DialerHelper {
    private const val TAG = "LeadZamDialer"

    /**
     * Opens the native Android dialer with the provided phone number.
     *
     * @param context Android context
     * @param rawPhoneNumber Raw phone number from the CRM request
     * @return true if dialer activity was successfully started, false otherwise
     */
    fun openNativeDialer(context: Context, rawPhoneNumber: String): Boolean {
        if (!CallRequest.isValidPhoneNumber(rawPhoneNumber)) {
            Log.e(TAG, "Cannot launch dialer: Invalid phone number '$rawPhoneNumber'")
            Toast.makeText(context, "Invalid phone number: $rawPhoneNumber", Toast.LENGTH_SHORT).show()
            return false
        }

        val cleanNumber = CallRequest.sanitizeForDialer(rawPhoneNumber)
        val dialUri = Uri.parse("tel:$cleanNumber")
        val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(dialIntent)
            Log.i(TAG, "Native dialer opened successfully for $cleanNumber")
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No dialer application found on this device", e)
            Toast.makeText(context, "No phone dialer app found on device", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch dialer", e)
            Toast.makeText(context, "Error opening dialer: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
