package com.example

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles communication with the Spring Boot backend for device registration.
 */
object BackendRegistrationClient {
    private const val TAG = "BackendRegClient"

    suspend fun registerDevice(
        baseUrl: String,
        token: String,
        deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${baseUrl.trimEnd('/')}/api/register-device"
            Log.i(TAG, "Registering FCM token with backend at: $endpoint")

            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("token", token)
                put("deviceName", deviceName)
                put("registeredAt", System.currentTimeMillis())
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = BufferedReader(InputStreamReader(responseStream)).use { it.readText() }
            connection.disconnect()

            if (responseCode in 200..299) {
                Log.i(TAG, "Registration succeeded: $responseBody")
                Result.success("Device successfully registered with backend.")
            } else {
                val errorMsg = "Backend returned HTTP $responseCode: $responseBody"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to backend", e)
            Result.failure(e)
        }
    }
}
