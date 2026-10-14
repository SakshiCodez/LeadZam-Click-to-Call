package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.databinding.ActivityMainBinding
import com.example.databinding.ItemCallRequestBinding
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LeadZamMain"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_AUTO_OPEN_DIALER = "extra_auto_open_dialer"
    }

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            binding.cardNotificationPermission.visibility = View.GONE
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            binding.cardNotificationPermission.visibility = View.VISIBLE
            Toast.makeText(this, "Notification permission required for background alerts", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CallRequestRepository.init(this)
        NotificationHelper.createNotificationChannel(this)

        setupUI()
        checkNotificationPermission()
        fetchFcmToken()
        observeRepository()

        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        CallRequestRepository.isAppInForeground.set(true)
    }

    override fun onStop() {
        super.onStop()
        CallRequestRepository.isAppInForeground.set(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "CRM Lead"
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
        val autoOpenDialer = intent.getBooleanExtra(EXTRA_AUTO_OPEN_DIALER, false)

        val request = CallRequest(
            phoneNumber = phoneNumber,
            contactName = contactName,
            requestId = requestId,
            timestamp = System.currentTimeMillis()
        )

        CallRequestRepository.postCallRequest(request)

        if (autoOpenDialer) {
            DialerHelper.openNativeDialer(this, phoneNumber)
        }
    }

    private fun setupUI() {
        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.btnCopyToken.setOnClickListener {
            val token = CallRequestRepository.fcmToken.value
            if (!token.isNullOrBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("LeadZam FCM Token", token)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.token_copied, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "FCM Token not yet ready", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegisterBackend.setOnClickListener {
            registerWithBackend()
        }

        binding.tvBackendUrl.setOnClickListener {
            showBackendUrlDialog()
        }

        binding.btnCallNow.setOnClickListener {
            val active = CallRequestRepository.activeRequest.value
            if (active != null) {
                DialerHelper.openNativeDialer(this, active.phoneNumber)
            }
        }

        binding.btnDismissActive.setOnClickListener {
            CallRequestRepository.clearActiveRequest()
        }

        binding.btnSimulateTest.setOnClickListener {
            simulateTestDialRequest()
        }

        binding.tvClearHistory.setOnClickListener {
            CallRequestRepository.clearHistory()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            binding.cardNotificationPermission.visibility = if (isGranted) View.GONE else View.VISIBLE
        } else {
            binding.cardNotificationPermission.visibility = View.GONE
        }
    }

    private fun fetchFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    binding.tvFcmStatus.text = "FCM status: ${task.exception?.localizedMessage ?: "Initializing"}"
                    binding.tvFcmToken.text = "Token will appear once google-services.json is configured."
                    binding.tvDeviceStatusChip.text = "Standby"
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.i(TAG, "Received FCM registration token: $token")
                CallRequestRepository.setFcmToken(token)
                displayToken(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase messaging initialization exception", e)
            binding.tvFcmStatus.text = "FCM initialization note: Add google-services.json from Firebase Console"
            binding.tvFcmToken.text = "Ready for local mock testing or Firebase setup"
            binding.tvDeviceStatusChip.text = "Ready"
        }
    }

    private fun displayToken(token: String) {
        binding.tvFcmStatus.text = getString(R.string.fcm_status_connected)
        binding.tvFcmToken.text = token
        binding.tvDeviceStatusChip.text = "Online"
        binding.tvDeviceStatusChip.setBackgroundResource(R.drawable.bg_badge)
    }

    private fun registerWithBackend() {
        val token = CallRequestRepository.fcmToken.value
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "FCM token is not available yet to register", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegisterBackend.isEnabled = false
        binding.btnRegisterBackend.text = getString(R.string.registering)

        lifecycleScope.launch {
            val url = CallRequestRepository.backendUrl.value
            val result = BackendRegistrationClient.registerDevice(url, token)
            binding.btnRegisterBackend.isEnabled = true
            binding.btnRegisterBackend.text = getString(R.string.register_backend)

            if (result.isSuccess) {
                Toast.makeText(this@MainActivity, R.string.registered_success, Toast.LENGTH_LONG).show()
                binding.tvDeviceStatusChip.text = "Registered"
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                Toast.makeText(this@MainActivity, getString(R.string.registered_failed, url) + "\n$err", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showBackendUrlDialog() {
        val currentUrl = CallRequestRepository.backendUrl.value
        val input = EditText(this).apply {
            setText(currentUrl)
            setSelection(currentUrl.length)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.change_backend_url)
            .setMessage("Set the base URL for the LeadZam Spring Boot backend API.\n(Default for Android Emulator is http://10.0.2.2:8080)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotBlank()) {
                    CallRequestRepository.setBackendUrl(newUrl)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun simulateTestDialRequest() {
        val testRequest = CallRequest(
            phoneNumber = "+919876543210",
            contactName = "Rahul Sharma (Simulated)",
            requestId = "test-" + System.currentTimeMillis() % 10000
        )
        CallRequestRepository.postCallRequest(testRequest)
        Toast.makeText(this, "Simulated call request from Rahul Sharma (+919876543210)", Toast.LENGTH_SHORT).show()
    }

    private fun observeRepository() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    CallRequestRepository.activeRequest.collectLatest { request ->
                        if (request != null) {
                            binding.cardActiveCallRequest.visibility = View.VISIBLE
                            binding.cardEmptyState.visibility = View.GONE

                            binding.tvActiveContactName.text = if (request.contactName.isNotBlank()) request.contactName else "CRM Lead"
                            binding.tvActivePhoneNumber.text = request.phoneNumber
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(request.timestamp))
                            binding.tvActiveRequestMeta.text = "Received at $timeStr • Ref: #${request.requestId.take(8)}"
                        } else {
                            binding.cardActiveCallRequest.visibility = View.GONE
                            binding.cardEmptyState.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    CallRequestRepository.fcmToken.collectLatest { token ->
                        if (!token.isNullOrBlank()) {
                            displayToken(token)
                        }
                    }
                }

                launch {
                    CallRequestRepository.backendUrl.collectLatest { url ->
                        binding.tvBackendUrl.text = getString(R.string.backend_url_label, url) + " (Tap to change)"
                    }
                }

                launch {
                    CallRequestRepository.requestHistory.collectLatest { history ->
                        renderHistory(history)
                    }
                }
            }
        }
    }

    private fun renderHistory(history: List<CallRequest>) {
        binding.layoutHistoryList.removeAllViews()

        if (history.isEmpty()) {
            binding.layoutHistoryList.addView(binding.tvEmptyHistory)
            return
        }

        val inflater = LayoutInflater.from(this)
        for (item in history) {
            val itemBinding = ItemCallRequestBinding.inflate(inflater, binding.layoutHistoryList, false)
            itemBinding.tvHistoryName.text = if (item.contactName.isNotBlank()) item.contactName else "CRM Lead"
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
            itemBinding.tvHistoryPhone.text = "${item.phoneNumber} • $timeStr"

            itemBinding.btnHistoryCall.setOnClickListener {
                DialerHelper.openNativeDialer(this, item.phoneNumber)
            }

            binding.layoutHistoryList.addView(itemBinding.root)
        }
    }
}
