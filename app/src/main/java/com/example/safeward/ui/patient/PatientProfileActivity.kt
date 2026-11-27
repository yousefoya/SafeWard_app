package com.example.safeward.ui.patient

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeward.MainActivity
import com.example.safeward.R
import com.example.safeward.data.MockDataRepository
import com.example.safeward.model.Device
import com.example.safeward.model.RoomTelemetry
import com.example.safeward.model.SecurityLog
import com.example.safeward.network.ConnectionState
import com.example.safeward.network.ServerConfig
import com.example.safeward.network.SessionManager
import com.example.safeward.network.TelemetryClient
import com.example.safeward.network.TelemetryClientFactory
import java.util.Locale

/**
 * Patient Profile & Vital Signs Chart screen (Qasem Shqair):
 * Displays patient vitals, Wi-Fi CSI status, an interactive Vital Signs trend chart,
 * and recent room events from mock_data.json (plus live TelemetryClient updates).
 */
class PatientProfileActivity : AppCompatActivity(), TelemetryClient.Listener {

    private enum class Metric { HR, SPO2, TEMP, BREATH }

    private lateinit var sessionManager: SessionManager
    private lateinit var roomSpinner: Spinner
    private lateinit var patientNameText: TextView
    private lateinit var patientMetaText: TextView
    private lateinit var csiStatusBadge: TextView
    private lateinit var fallConfidenceText: TextView
    private lateinit var heartRateValue: TextView
    private lateinit var spo2Value: TextView
    private lateinit var tempValue: TextView
    private lateinit var breathingValue: TextView
    private lateinit var chartTitleText: TextView
    private lateinit var vitalChartView: VitalSignsChartView
    private lateinit var recentEventsText: TextView

    private lateinit var metricButtons: Map<Metric, TextView>
    private var selectedMetric: Metric = Metric.HR

    private val rooms = mutableListOf<RoomTelemetry>()
    private val liveSeries = mutableMapOf<String, MutableList<RoomTelemetry>>()
    private var selectedRoomId: String? = null
    private var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_profile)

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
        if (sessionManager.current() == null) {
            backToLogin()
            return
        }

        selectedRoomId = intent.getStringExtra(EXTRA_ROOM_ID)

        roomSpinner = findViewById(R.id.roomSpinner)
        patientNameText = findViewById(R.id.patientNameText)
        patientMetaText = findViewById(R.id.patientMetaText)
        csiStatusBadge = findViewById(R.id.csiStatusBadge)
        fallConfidenceText = findViewById(R.id.fallConfidenceText)
        heartRateValue = findViewById(R.id.heartRateValue)
        spo2Value = findViewById(R.id.spo2Value)
        tempValue = findViewById(R.id.tempValue)
        breathingValue = findViewById(R.id.breathingValue)
        chartTitleText = findViewById(R.id.chartTitleText)
        vitalChartView = findViewById(R.id.vitalChartView)
        recentEventsText = findViewById(R.id.recentEventsText)

        metricButtons = mapOf(
            Metric.HR to findViewById(R.id.metricHr),
            Metric.SPO2 to findViewById(R.id.metricSpo2),
            Metric.TEMP to findViewById(R.id.metricTemp),
            Metric.BREATH to findViewById(R.id.metricBr)
        )
        metricButtons.forEach { (metric, button) ->
            button.setOnClickListener {
                selectedMetric = metric
                updateMetricPills()
                renderSelectedRoom()
            }
        }
        updateMetricPills()

        findViewById<Button>(R.id.devicesButton).setOnClickListener {
            startActivity(Intent(this, DeviceManagementActivity::class.java))
        }
        // This is the Doctor's home screen, so "Back" here means sign out.
        findViewById<Button>(R.id.backButton).setOnClickListener {
            sessionManager.clear()
            backToLogin()
        }

        roomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                rooms.getOrNull(pos)?.let {
                    selectedRoomId = it.roomId
                    renderSelectedRoom()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    override fun onResume() {
        super.onResume()
        loadRooms()
        connectStream()
    }

    override fun onPause() {
        client?.disconnect()
        super.onPause()
    }

    private fun loadRooms() {
        rooms.clear()
        rooms.addAll(MockDataRepository.getRooms(this))
        if (rooms.isEmpty()) return

        val labels = rooms.map { "Room ${it.roomId} — ${it.patientName}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        roomSpinner.adapter = adapter

        val targetIndex = rooms.indexOfFirst { it.roomId == selectedRoomId }.takeIf { it >= 0 } ?: 0
        selectedRoomId = rooms[targetIndex].roomId
        roomSpinner.setSelection(targetIndex)
        renderSelectedRoom()
    }

    private fun connectStream() {
        client?.disconnect()
        client = TelemetryClientFactory.create(this, ServerConfig.DEFAULT).also { it.connect(this) }
    }

    private fun renderSelectedRoom() {
        val room = rooms.firstOrNull { it.roomId == selectedRoomId } ?: rooms.firstOrNull() ?: return

        patientNameText.text = room.patientName
        patientMetaText.text = "Room ${room.roomId} • Age ${room.age}"
        csiStatusBadge.text = room.csiStatus

        val badgeColor = when (room.csiStatus) {
            "FALL_DETECTED" -> R.color.alert_critical_rose
            "MOVEMENT" -> R.color.alert_warning_amber
            "OFFLINE" -> R.color.security_soft_lavender
            else -> R.color.status_safe_mint
        }
        val badgeTextColor = if (room.csiStatus == "FALL_DETECTED") R.color.white else R.color.text_soft_charcoal
        csiStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, badgeColor))
        csiStatusBadge.setTextColor(ContextCompat.getColor(this, badgeTextColor))

        val confPct = (room.fallConfidence * 100).toInt()
        val timeShort = room.timestamp.substringAfter('T').take(5).ifEmpty { "--:--" }
        fallConfidenceText.text = "Wi-Fi CSI Fall Confidence: $confPct% • Updated $timeShort"

        heartRateValue.text = "${room.heartRateBpm} bpm"
        spo2Value.text = "${room.spo2Percent} %"
        tempValue.text = String.format(Locale.US, "%.1f °C", room.temperatureC)
        breathingValue.text = "${room.breathingRpm} rpm"

        renderChart(room)
        renderHistory(room.roomId)
    }

    private fun renderChart(room: RoomTelemetry) {
        val history = liveSeries[room.roomId]
        val points = if (history != null && history.size >= 3) {
            history.takeLast(8).mapIndexed { index, item ->
                val label = item.timestamp.substringAfter('T').take(5).ifEmpty { "#${index + 1}" }
                VitalSignsChartView.DataPoint(label, extractMetric(item, selectedMetric))
            }
        } else {
            buildBaselinePoints(room, selectedMetric)
        }

        when (selectedMetric) {
            Metric.HR -> {
                chartTitleText.text = "Heart Rate Trend (BPM)"
                vitalChartView.setData(points, "bpm", R.color.alert_critical_rose, decimals = 0)
            }
            Metric.SPO2 -> {
                chartTitleText.text = "Blood Oxygen SpO2 Trend (%)"
                vitalChartView.setData(points, "%", R.color.text_soft_charcoal, decimals = 0)
            }
            Metric.TEMP -> {
                chartTitleText.text = "Body Temperature Trend (°C)"
                vitalChartView.setData(points, "°C", R.color.alert_critical_rose, decimals = 1)
            }
            Metric.BREATH -> {
                chartTitleText.text = "Breathing Rate Trend (RPM)"
                vitalChartView.setData(points, "rpm", R.color.text_soft_charcoal, decimals = 0)
            }
        }
    }

    private fun buildBaselinePoints(room: RoomTelemetry, metric: Metric): List<VitalSignsChartView.DataPoint> {
        val offsets = when (metric) {
            Metric.HR -> listOf(-4f, -2f, 1f, -1f, 3f, 0f)
            Metric.SPO2 -> listOf(0f, -1f, 0f, 1f, 0f, 0f)
            Metric.TEMP -> listOf(-0.2f, -0.1f, 0.0f, 0.1f, 0.0f, 0.0f)
            Metric.BREATH -> listOf(-1f, 0f, 1f, 0f, 1f, 0f)
        }
        val labels = listOf("-25m", "-20m", "-15m", "-10m", "-5m", "Now")
        val base = extractMetric(room, metric)
        return labels.mapIndexed { i, label ->
            VitalSignsChartView.DataPoint(label, base + offsets[i])
        }
    }

    private fun extractMetric(room: RoomTelemetry, metric: Metric): Float = when (metric) {
        Metric.HR -> room.heartRateBpm.toFloat()
        Metric.SPO2 -> room.spo2Percent.toFloat()
        Metric.TEMP -> room.temperatureC.toFloat()
        Metric.BREATH -> room.breathingRpm.toFloat()
    }

    private fun renderHistory(roomId: String) {
        val events = MockDataRepository.getHistoryForRoom(this, roomId)
        recentEventsText.text = if (events.isEmpty()) {
            "No recorded events for Room $roomId in mock_data.json."
        } else {
            events.joinToString("\n\n") { evt ->
                val time = evt.timestamp.substringAfter('T').take(5)
                "• [${evt.eventType}] ($time) — ${evt.details} (HR ${evt.heartRateBpm} bpm)"
            }
        }
    }

    private fun updateMetricPills() {
        val activeBg = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_soft_charcoal))
        val inactiveBg = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_warm_linen))
        val activeText = ContextCompat.getColor(this, R.color.white)
        val inactiveText = ContextCompat.getColor(this, R.color.text_soft_charcoal)

        metricButtons.forEach { (metric, button) ->
            val selected = metric == selectedMetric
            button.backgroundTintList = if (selected) activeBg else inactiveBg
            button.setTextColor(if (selected) activeText else inactiveText)
        }
    }

    private fun backToLogin() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    // ---- TelemetryClient.Listener ------------------------------------------

    override fun onStateChanged(state: ConnectionState, message: String?) {
        if (sessionManager.current() == null) backToLogin()
    }

    override fun onRoomTelemetry(room: RoomTelemetry) {
        val idx = rooms.indexOfFirst { it.roomId == room.roomId }
        if (idx >= 0) rooms[idx] = room else rooms.add(room)

        val series = liveSeries.getOrPut(room.roomId) { mutableListOf() }
        series.add(room)
        if (series.size > 20) series.removeAt(0)

        if (room.roomId == selectedRoomId) {
            renderSelectedRoom()
        }
    }

    override fun onDeviceStatus(device: Device) = Unit
    override fun onSecurityEvent(log: SecurityLog) = Unit
    override fun onMessageRejected(roomId: String, reason: String) = Unit

    companion object {
        const val EXTRA_ROOM_ID = "com.example.safeward.EXTRA_ROOM_ID"
    }
}
