package com.example.safeward.ui.patient

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Device & Room Management screen (Qasem Shqair):
 * Displays ESP32 CSI and medical sensor node health from mock_data.json / TelemetryClient,
 * and provides an Add Room form that registers a new patient room & ESP32 nodes.
 */
class DeviceManagementActivity : AppCompatActivity(), TelemetryClient.Listener {

    private enum class Filter { ALL, CSI, MED, OFFLINE }

    private lateinit var sessionManager: SessionManager
    private lateinit var totalNodesCount: TextView
    private lateinit var onlineNodesCount: TextView
    private lateinit var tlsNodesCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyDevicesText: TextView

    private lateinit var filterButtons: Map<Filter, TextView>
    private var selectedFilter = Filter.ALL

    private val devices = LinkedHashMap<String, Device>()
    private lateinit var adapter: DeviceAdapter
    private var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_management)

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

        totalNodesCount = findViewById(R.id.totalNodesCount)
        onlineNodesCount = findViewById(R.id.onlineNodesCount)
        tlsNodesCount = findViewById(R.id.tlsNodesCount)
        recyclerView = findViewById(R.id.devicesRecyclerView)
        emptyDevicesText = findViewById(R.id.emptyDevicesText)

        adapter = DeviceAdapter { device ->
            startActivity(
                Intent(this, PatientProfileActivity::class.java)
                    .putExtra(PatientProfileActivity.EXTRA_ROOM_ID, device.roomId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        filterButtons = mapOf(
            Filter.ALL to findViewById(R.id.filterAll),
            Filter.CSI to findViewById(R.id.filterCsi),
            Filter.MED to findViewById(R.id.filterMed),
            Filter.OFFLINE to findViewById(R.id.filterOffline)
        )
        filterButtons.forEach { (filter, button) ->
            button.setOnClickListener {
                selectedFilter = filter
                updateFilterStyles()
                renderDevices()
            }
        }
        updateFilterStyles()

        findViewById<Button>(R.id.addRoomButton).setOnClickListener { showAddRoomDialog() }
        findViewById<Button>(R.id.patientProfileButton).setOnClickListener {
            startActivity(
                Intent(this, PatientProfileActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }

        loadInitialDevices()
        connectStream()
    }

    override fun onDestroy() {
        client?.disconnect()
        super.onDestroy()
    }

    private fun loadInitialDevices() {
        devices.clear()
        MockDataRepository.getDevices(this).forEach { devices[it.deviceId] = it }
        renderDevices()
    }

    private fun connectStream() {
        client?.disconnect()
        client = TelemetryClientFactory.create(this, ServerConfig.DEFAULT).also { it.connect(this) }
    }

    private fun renderDevices() {
        val all = devices.values.toList()
        totalNodesCount.text = all.size.toString()
        onlineNodesCount.text = all.count { it.status.equals("ONLINE", ignoreCase = true) }.toString()
        tlsNodesCount.text = all.count { it.tlsVerified }.toString()

        val filtered = when (selectedFilter) {
            Filter.ALL -> all
            Filter.CSI -> all.filter { it.type.contains("CSI", ignoreCase = true) }
            Filter.MED -> all.filter { it.type.contains("MAX30102", ignoreCase = true) || it.type.contains("Temp", ignoreCase = true) }
            Filter.OFFLINE -> all.filter { !it.status.equals("ONLINE", ignoreCase = true) }
        }

        adapter.submitList(filtered)
        emptyDevicesText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddRoomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null)
        val inputRoomId = dialogView.findViewById<EditText>(R.id.inputRoomId)
        val inputPatientName = dialogView.findViewById<EditText>(R.id.inputPatientName)
        val inputPatientAge = dialogView.findViewById<EditText>(R.id.inputPatientAge)
        val inputCsiDeviceId = dialogView.findViewById<EditText>(R.id.inputCsiDeviceId)
        val checkboxMedSensor = dialogView.findViewById<CheckBox>(R.id.checkboxMedSensor)

        val nextRoomNum = (MockDataRepository.getRooms(this).mapNotNull { it.roomId.toIntOrNull() }.maxOrNull() ?: 103) + 1
        inputRoomId.setText(nextRoomNum.toString())
        inputCsiDeviceId.setText("ESP32_CSI_$nextRoomNum")

        AlertDialog.Builder(this)
            .setTitle("Add New Room")
            .setView(dialogView)
            .setPositiveButton("Save Room") { _, _ ->
                val roomId = inputRoomId.text.toString().trim()
                val patientName = inputPatientName.text.toString().trim()
                val age = inputPatientAge.text.toString().trim().toIntOrNull()
                val csiId = inputCsiDeviceId.text.toString().trim().ifEmpty { "ESP32_CSI_$roomId" }

                if (roomId.isEmpty() || patientName.isEmpty() || age == null) {
                    Toast.makeText(this, "Please fill Room Number, Patient Name, and Age.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val newRoom = RoomTelemetry(
                    roomId = roomId,
                    patientName = patientName,
                    age = age,
                    csiStatus = "NORMAL",
                    fallDetected = false,
                    fallConfidence = 0.02,
                    heartRateBpm = 76,
                    spo2Percent = 98,
                    temperatureC = 36.8,
                    breathingRpm = 16,
                    timestamp = nowIso,
                    nonce = "n_local_$roomId"
                )

                val newDevices = mutableListOf(
                    Device(
                        deviceId = csiId,
                        roomId = roomId,
                        type = "Wi-Fi CSI Node",
                        status = "ONLINE",
                        rssiDbm = -49,
                        tlsVerified = true,
                        lastSeen = nowIso
                    )
                )
                if (checkboxMedSensor.isChecked) {
                    newDevices.add(
                        Device(
                            deviceId = "ESP32_MED_$roomId",
                            roomId = roomId,
                            type = "MAX30102 + Temp",
                            status = "ONLINE",
                            rssiDbm = -51,
                            tlsVerified = true,
                            lastSeen = nowIso
                        )
                    )
                }

                MockDataRepository.addRoom(newRoom, newDevices)
                newDevices.forEach { devices[it.deviceId] = it }
                renderDevices()
                Toast.makeText(this, "Room $roomId ($patientName) added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFilterStyles() {
        val activeBg = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_soft_charcoal))
        val inactiveBg = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_warm_linen))
        val activeText = ContextCompat.getColor(this, R.color.white)
        val inactiveText = ContextCompat.getColor(this, R.color.text_soft_charcoal)

        filterButtons.forEach { (filter, button) ->
            val selected = filter == selectedFilter
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

    override fun onDeviceStatus(device: Device) {
        devices[device.deviceId] = device
        renderDevices()
    }

    override fun onRoomTelemetry(room: RoomTelemetry) = Unit
    override fun onSecurityEvent(log: SecurityLog) = Unit
    override fun onMessageRejected(roomId: String, reason: String) = Unit
}
