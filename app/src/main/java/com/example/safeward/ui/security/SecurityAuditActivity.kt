package com.example.safeward.ui.security

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeward.MainActivity
import com.example.safeward.NetworkTestActivity
import com.example.safeward.R
import com.example.safeward.data.MockDataRepository
import com.example.safeward.model.Device
import com.example.safeward.model.SecurityLog
import com.example.safeward.network.ConnectionState
import com.example.safeward.network.ServerConfig
import com.example.safeward.network.SessionManager
import com.example.safeward.network.TelemetryClient
import com.example.safeward.network.TelemetryClientFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Security Audit Log screen (Zeina Nasrallah):
 * Displays a live feed of blocked Replay, Cross-Layer Spoofing, and Unauthorized Device
 * attempts received through [TelemetryClient], with filtering and TLS node status.
 */
class SecurityAuditActivity : AppCompatActivity(), TelemetryClient.Listener {

    private lateinit var sessionManager: SessionManager
    private lateinit var statusBanner: LinearLayout
    private lateinit var connectionStatusText: TextView
    private lateinit var tlsNodesText: TextView
    private lateinit var totalEventsCount: TextView
    private lateinit var replayCount: TextView
    private lateinit var spoofCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    private lateinit var filterButtons: Map<String?, TextView>
    private val adapter = SecurityLogAdapter()

    private val allLogs = mutableListOf<SecurityLog>()
    private val devices = LinkedHashMap<String, Device>()
    private var selectedAttackFilter: String? = null
    private var liveBlockCounter = 0

    private var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_security_audit)

        val root = findViewById<View>(R.id.main)
        val basePadding = root.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + basePadding, bars.top + basePadding,
                bars.right + basePadding, bars.bottom + basePadding
            )
            insets
        }

        sessionManager = SessionManager(this)
        val session = sessionManager.current()
        if (session == null) {
            backToLogin()
            return
        }

        val user = MockDataRepository.findById(this, session.userId)
        val displayName = user?.name ?: session.role
        findViewById<TextView>(R.id.officerSubtitleText).text =
            "$displayName • ${session.role} Officer"

        statusBanner = findViewById(R.id.statusBanner)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        tlsNodesText = findViewById(R.id.tlsNodesText)
        totalEventsCount = findViewById(R.id.totalEventsCount)
        replayCount = findViewById(R.id.replayCount)
        spoofCount = findViewById(R.id.spoofCount)
        recyclerView = findViewById(R.id.securityRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        filterButtons = mapOf(
            null to findViewById(R.id.filterAll),
            "CROSS_LAYER_SPOOF" to findViewById(R.id.filterSpoof),
            "REPLAY_ATTACK" to findViewById(R.id.filterReplay),
            "UNAUTHORIZED_DEVICE" to findViewById(R.id.filterUnauthorized)
        )
        filterButtons.forEach { (filterKey, button) ->
            button.setOnClickListener {
                selectedAttackFilter = filterKey
                updateFilterStyles()
                renderList()
            }
        }
        updateFilterStyles()

        findViewById<Button>(R.id.refreshButton).setOnClickListener { connectStream() }
        findViewById<Button>(R.id.networkTestButton).setOnClickListener {
            startActivity(Intent(this, NetworkTestActivity::class.java))
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            sessionManager.clear()
            backToLogin()
        }

        connectStream()
    }

    private fun connectStream() {
        client?.disconnect()
        allLogs.clear()
        devices.clear()
        liveBlockCounter = 0
        updateSummaryAndList()
        tlsNodesText.text = "Verifying ESP32 TLS nodes…"

        client = TelemetryClientFactory.create(this, ServerConfig.DEFAULT)
            .also { it.connect(this) }
    }

    override fun onDestroy() {
        client?.disconnect()
        super.onDestroy()
    }

    // ---- TelemetryClient.Listener ------------------------------------------

    override fun onStateChanged(state: ConnectionState, message: String?) {
        if (sessionManager.current() == null) {
            backToLogin()
            return
        }
        val bannerColorRes = when (state) {
            ConnectionState.CONNECTED -> R.color.status_safe_mint
            ConnectionState.CONNECTING -> R.color.alert_warning_amber
            ConnectionState.ERROR -> R.color.vitals_blush_petal
            ConnectionState.DISCONNECTED -> R.color.security_soft_lavender
        }
        ViewCompat.setBackgroundTintList(
            statusBanner,
            ColorStateList.valueOf(ContextCompat.getColor(this, bannerColorRes))
        )
        connectionStatusText.text = when (state) {
            ConnectionState.CONNECTED -> "✓ Protection Active — ${message.orEmpty()}"
            ConnectionState.CONNECTING -> "Connecting — ${message.orEmpty()}"
            ConnectionState.ERROR -> "Connection Error — ${message.orEmpty()}"
            ConnectionState.DISCONNECTED -> "Disconnected"
        }
    }

    override fun onDeviceStatus(device: Device) {
        devices[device.deviceId] = device
        val verifiedCount = devices.values.count { it.tlsVerified }
        tlsNodesText.text = "$verifiedCount / ${devices.size} ESP32 nodes TLS verified"
    }

    override fun onSecurityEvent(log: SecurityLog) {
        if (allLogs.none { it.logId == log.logId }) {
            allLogs.add(log)
            updateSummaryAndList()
        }
    }

    override fun onMessageRejected(roomId: String, reason: String) {
        liveBlockCounter++
        val attackType = reason.substringBefore(':').trim().ifEmpty { "REPLAY_ATTACK" }
        val details = reason.substringAfter(':', reason).trim()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        val liveEvent = SecurityLog(
            logId = "live_%02d".format(liveBlockCounter),
            attackType = attackType,
            roomId = roomId,
            sourceId = "LIVE_VALIDATOR",
            reason = details,
            actionTaken = "BLOCKED",
            timestamp = now
        )
        // Put newly intercepted live anomalies at the top of the feed.
        allLogs.add(0, liveEvent)
        updateSummaryAndList()
    }

    // ---- UI helpers --------------------------------------------------------

    private fun updateSummaryAndList() {
        totalEventsCount.text = allLogs.size.toString()
        replayCount.text = allLogs.count { it.attackType == "REPLAY_ATTACK" }.toString()
        spoofCount.text = allLogs.count {
            it.attackType == "CROSS_LAYER_SPOOF" || it.attackType == "UNAUTHORIZED_DEVICE"
        }.toString()
        renderList()
    }

    private fun renderList() {
        val filtered = if (selectedAttackFilter == null) {
            allLogs.toList()
        } else {
            allLogs.filter { it.attackType == selectedAttackFilter }
        }
        adapter.submitList(filtered)
        val isEmpty = filtered.isEmpty()
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateFilterStyles() {
        filterButtons.forEach { (key, view) ->
            val selected = key == selectedAttackFilter
            val bgColor = if (selected) R.color.text_soft_charcoal else R.color.surface_warm_linen
            val textColor = if (selected) R.color.white else R.color.text_soft_charcoal
            ViewCompat.setBackgroundTintList(
                view,
                ColorStateList.valueOf(ContextCompat.getColor(this, bgColor))
            )
            view.setTextColor(ContextCompat.getColor(this, textColor))
            view.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun backToLogin() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
