package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
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
import com.example.databinding.ItemCrmLeadBinding
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

    // List of CRM leads displayed in the dashboard
    private val crmLeads = mutableListOf<CrmLead>()

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

        initSampleLeads()
        setupUI()
        checkNotificationPermission()
        fetchFcmToken()
        observeRepository()

        handleIntent(intent)
    }

    private fun initSampleLeads() {
        crmLeads.clear()
        crmLeads.addAll(CrmLead.getSampleLeads())
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
        // Default tab selection
        binding.toggleNavGroup.check(R.id.btnTabPipeline)
        showPipelineView(true)

        binding.toggleNavGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTabPipeline -> {
                        showPipelineView(true)
                        binding.btnTabPipeline.setTextColor(Color.WHITE)
                        binding.btnTabDevice.setTextColor(Color.parseColor("#94A3B8"))
                    }
                    R.id.btnTabDevice -> {
                        showPipelineView(false)
                        binding.btnTabDevice.setTextColor(Color.WHITE)
                        binding.btnTabPipeline.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            }
        }

        // Render Initial Leads
        renderLeads(crmLeads)

        // Search Filter
        binding.etSearchLeads.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLeads(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Add Custom Lead button
        binding.btnAddLeadDialog.setOnClickListener {
            showAddLeadDialog()
        }

        // Permission grant
        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Token Copy
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

        // Register to Backend
        binding.btnRegisterBackend.setOnClickListener {
            registerWithBackend()
        }

        binding.tvBackendUrl.setOnClickListener {
            showBackendUrlDialog()
        }

        // Active Call card actions
        binding.btnCallNow.setOnClickListener {
            val active = CallRequestRepository.activeRequest.value
            if (active != null) {
                DialerHelper.openNativeDialer(this, active.phoneNumber)
            }
        }

        binding.btnDismissActive.setOnClickListener {
            CallRequestRepository.clearActiveRequest()
        }

        binding.tvClearHistory.setOnClickListener {
            CallRequestRepository.clearHistory()
        }
    }

    private fun showPipelineView(isPipeline: Boolean) {
        binding.layoutPipelineView.visibility = if (isPipeline) View.VISIBLE else View.GONE
        binding.layoutDeviceSettingsView.visibility = if (isPipeline) View.GONE else View.VISIBLE
    }

    private fun renderLeads(leadsToRender: List<CrmLead>) {
        binding.layoutLeadsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        binding.tvLeadsCount.text = "Showing ${leadsToRender.size} contacts"

        for (lead in leadsToRender) {
            val itemBinding = ItemCrmLeadBinding.inflate(inflater, binding.layoutLeadsContainer, false)

            itemBinding.tvLeadName.text = lead.name
            itemBinding.tvLeadRoleCompany.text = "${lead.role} • ${lead.company}"
            itemBinding.tvLeadPhone.text = lead.phoneNumber
            itemBinding.tvLeadDealValue.text = lead.dealValue
            itemBinding.tvLeadAvatar.text = lead.initials
            try {
                itemBinding.tvLeadAvatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(lead.avatarBgColorHex)
                )
            } catch (e: Exception) {
                // Keep default
            }

            itemBinding.tvLeadStageBadge.text = lead.stage
            applyStageBadgeColor(itemBinding.tvLeadStageBadge, lead.stage)

            // Direct Call Lead Button -> opens native dialer
            itemBinding.btnLeadCallNow.setOnClickListener {
                DialerHelper.openNativeDialer(this, lead.phoneNumber)
                // Add to history log
                val callReq = CallRequest(lead.phoneNumber, lead.name)
                CallRequestRepository.postCallRequest(callReq)
            }

            // Push Call Simulation -> triggers incoming call banner and notification
            itemBinding.btnLeadSimulatePush.setOnClickListener {
                val callReq = CallRequest(
                    phoneNumber = lead.phoneNumber,
                    contactName = lead.name,
                    requestId = "crm-" + System.currentTimeMillis() % 10000
                )
                CallRequestRepository.postCallRequest(callReq)
                Toast.makeText(this, "Inbound call request triggered for ${lead.name}!", Toast.LENGTH_SHORT).show()
            }

            binding.layoutLeadsContainer.addView(itemBinding.root)
        }
    }

    private fun applyStageBadgeColor(badge: android.widget.TextView, stage: String) {
        when (stage) {
            "Warm Lead" -> {
                badge.setBackgroundResource(R.drawable.bg_badge)
                badge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                badge.setTextColor(Color.parseColor("#92400E"))
            }
            "Contract Sent" -> {
                badge.setBackgroundResource(R.drawable.bg_badge)
                badge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E7FF"))
                badge.setTextColor(Color.parseColor("#3730A3"))
            }
            "Qualified" -> {
                badge.setBackgroundResource(R.drawable.bg_badge)
                badge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                badge.setTextColor(Color.parseColor("#166534"))
            }
            "Follow-up" -> {
                badge.setBackgroundResource(R.drawable.bg_badge)
                badge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE7F3"))
                badge.setTextColor(Color.parseColor("#9D174D"))
            }
            else -> {
                badge.setBackgroundResource(R.drawable.bg_badge)
                badge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                badge.setTextColor(Color.parseColor("#475569"))
            }
        }
    }

    private fun filterLeads(query: String) {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        if (cleanQuery.isEmpty()) {
            renderLeads(crmLeads)
            return
        }

        val filtered = crmLeads.filter {
            it.name.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            it.company.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            it.phoneNumber.contains(cleanQuery) ||
            it.stage.lowercase(Locale.getDefault()).contains(cleanQuery)
        }
        renderLeads(filtered)
    }

    private fun showAddLeadDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etName = EditText(this).apply { hint = "Full Name (e.g. Ramesh Patel)" }
        val etRole = EditText(this).apply { hint = "Role & Company (e.g. CTO, TechCo)" }
        val etPhone = EditText(this).apply { hint = "Phone (e.g. +91 98765 00000)" }
        val etValue = EditText(this).apply { hint = "Deal Value (e.g. ₹2,00,000)" }

        layout.addView(etName)
        layout.addView(etRole)
        layout.addView(etPhone)
        layout.addView(etValue)

        AlertDialog.Builder(this)
            .setTitle("Add New Prospect Lead")
            .setView(layout)
            .setPositiveButton("Add Lead") { _, _ ->
                val name = etName.text.toString().trim()
                val roleComp = etRole.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val deal = etValue.text.toString().trim().ifEmpty { "₹1,00,000" }

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val initials = name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase(Locale.getDefault())

                    val newLead = CrmLead(
                        id = "lead-${System.currentTimeMillis()}",
                        name = name,
                        role = roleComp.ifEmpty { "Prospective Client" },
                        company = "New Inbound Inquiry",
                        phoneNumber = phone,
                        stage = "Warm Lead",
                        dealValue = deal,
                        initials = initials.ifEmpty { "NL" },
                        avatarBgColorHex = "#2563EB"
                    )

                    crmLeads.add(0, newLead)
                    renderLeads(crmLeads)
                    binding.tvMetricTotalLeads.text = "${crmLeads.size} Contacts"
                    Toast.makeText(this, "Added $name to CRM pipeline", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter both name and phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                    binding.tvDeviceStatusChip.text = "Ready"
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.i(TAG, "Received FCM registration token: $token")
                CallRequestRepository.setFcmToken(token)
                displayToken(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase messaging initialization exception", e)
            binding.tvFcmStatus.text = "FCM initialization: Ready for local mock testing or Firebase setup"
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

    private fun observeRepository() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    CallRequestRepository.activeRequest.collectLatest { request ->
                        if (request != null) {
                            binding.cardActiveCallRequest.visibility = View.VISIBLE

                            binding.tvActiveContactName.text = if (request.contactName.isNotBlank()) request.contactName else "CRM Lead"
                            binding.tvActivePhoneNumber.text = request.phoneNumber
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(request.timestamp))
                            binding.tvActiveRequestMeta.text = "Received at $timeStr • Ref: #${request.requestId.take(8)}"
                        } else {
                            binding.cardActiveCallRequest.visibility = View.GONE
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
