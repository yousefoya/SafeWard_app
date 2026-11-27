package com.example.safeward

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeward.model.Device
import com.example.safeward.model.RoomTelemetry
import com.example.safeward.model.SecurityLog
import com.example.safeward.network.ConnectionState
import com.example.safeward.network.ServerConfig
import com.example.safeward.network.SessionManager
import com.example.safeward.network.TelemetryClient
import com.example.safeward.network.TelemetryClientFactory

/**
 * Week-1 milestone screen (Yousef): verifies the Network Layer end-to-end by reading
 * the mock data through [TelemetryClient], exactly as the dashboards will.
 */
class NetworkTestActivity : AppCompatActivity(), TelemetryClient.Listener {

    private lateinit var sessionManager: SessionManager
    private lateinit var stateText: TextView
    private lateinit var logText: TextView
    private var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_network_test)
        val root = findViewById<android.view.View>(R.id.main)
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

        stateText = findViewById(R.id.stateText)
        logText = findViewById(R.id.logText)
        findViewById<TextView>(R.id.sessionText).text =
            "Signed in as ${session.role} • token ${session.token.take(8)}…"

        findViewById<Button>(R.id.reconnectButton).setOnClickListener { connect() }
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            sessionManager.clear()
            backToLogin()
        }

        connect()
    }

    private fun connect() {
        client?.disconnect()
        logText.text = ""
        val config = ServerConfig.DEFAULT
        log("Mode: ${config.mode}   Broker: ${config.brokerUri}")
        client = TelemetryClientFactory.create(this, config).also { it.connect(this) }
    }

    override fun onDestroy() {
        client?.disconnect()
        super.onDestroy()
    }

    // ---- TelemetryClient.Listener ------------------------------------------

    override fun onStateChanged(state: ConnectionState, message: String?) {
        // Re-check the session on every state change: expired session => force logout.
        if (sessionManager.current() == null) {
            backToLogin()
            return
        }
        stateText.text = if (message != null) "$state — $message" else state.name
        stateText.setBackgroundColor(
            getColor(
                when (state) {
                    ConnectionState.CONNECTED -> R.color.status_safe_mint
                    ConnectionState.CONNECTING -> R.color.alert_warning_amber
                    ConnectionState.ERROR -> R.color.vitals_blush_petal
                    ConnectionState.DISCONNECTED -> R.color.security_soft_lavender
                }
            )
        )
        log("[STATE] $state ${message.orEmpty()}")
    }

    override fun onDeviceStatus(device: Device) {
        log("[DEVICE] ${device.deviceId} room ${device.roomId} ${device.status} " +
            "${device.rssiDbm}dBm TLS=${if (device.tlsVerified) "OK" else "FAIL"}")
    }

    override fun onRoomTelemetry(room: RoomTelemetry) {
        val alert = if (room.fallDetected) "  ⚠ FALL ${(room.fallConfidence * 100).toInt()}%" else ""
        log("[ROOM ${room.roomId}] ${room.patientName} ${room.csiStatus}$alert\n" +
            "    HR ${room.heartRateBpm}  SpO2 ${room.spo2Percent}%  " +
            "T ${room.temperatureC}°C  BR ${room.breathingRpm}")
    }

    override fun onSecurityEvent(log: SecurityLog) {
        log("[SECURITY] ${log.attackType} room ${log.roomId} → ${log.actionTaken}\n    ${log.reason}")
    }

    override fun onMessageRejected(roomId: String, reason: String) {
        log("[BLOCKED] room $roomId — $reason")
    }

    // ------------------------------------------------------------------------

    private fun log(line: String) {
        logText.append(line + "\n")
    }

    private fun backToLogin() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
