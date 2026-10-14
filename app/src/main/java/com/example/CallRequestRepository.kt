package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-memory and persistent state store for LeadZam call requests and FCM configuration.
 */
object CallRequestRepository {
    private const val PREFS_NAME = "leadzam_prefs"
    private const val KEY_FCM_TOKEN = "key_fcm_token"
    private const val KEY_BACKEND_URL = "key_backend_url"
    private const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8080" // Standard Android emulator loopback to host

    private var sharedPrefs: SharedPreferences? = null

    // Tracks whether MainActivity is currently active in the foreground
    val isAppInForeground = AtomicBoolean(false)

    // Current active call request to display in UI
    private val _activeRequest = MutableStateFlow<CallRequest?>(null)
    val activeRequest: StateFlow<CallRequest?> = _activeRequest.asStateFlow()

    // History of requests received in this session (max 20)
    private val _requestHistory = MutableStateFlow<List<CallRequest>>(emptyList())
    val requestHistory: StateFlow<List<CallRequest>> = _requestHistory.asStateFlow()

    // Cached FCM registration token
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    // Backend API base URL
    private val _backendUrl = MutableStateFlow(DEFAULT_BACKEND_URL)
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    fun init(context: Context) {
        if (sharedPrefs == null) {
            val appContext = context.applicationContext
            sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _fcmToken.value = sharedPrefs?.getString(KEY_FCM_TOKEN, null)
            _backendUrl.value = sharedPrefs?.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        }
    }

    fun setFcmToken(token: String) {
        _fcmToken.value = token
        sharedPrefs?.edit()?.putString(KEY_FCM_TOKEN, token)?.apply()
    }

    fun setBackendUrl(url: String) {
        val sanitized = url.trim().trimEnd('/')
        _backendUrl.value = sanitized
        sharedPrefs?.edit()?.putString(KEY_BACKEND_URL, sanitized)?.apply()
    }

    fun postCallRequest(request: CallRequest) {
        _activeRequest.value = request
        val current = _requestHistory.value.toMutableList()
        // Deduplicate recent identical requests within 5 seconds if needed
        current.add(0, request)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        _requestHistory.value = current
    }

    fun clearActiveRequest() {
        _activeRequest.value = null
    }

    fun clearHistory() {
        _requestHistory.value = emptyList()
    }
}
