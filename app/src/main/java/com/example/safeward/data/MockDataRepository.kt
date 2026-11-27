package com.example.safeward.data

import android.content.Context
import com.example.safeward.model.Device
import com.example.safeward.model.HistoryLog
import com.example.safeward.model.MockData
import com.example.safeward.model.RoomTelemetry
import com.example.safeward.model.SecurityLog
import com.example.safeward.model.User
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the shared Week-1 data contract (assets/mock_data.json).
 * Will be replaced by live MQTT/REST data in later milestones.
 */
object MockDataRepository {

    private const val FILE_NAME = "mock_data.json"

    @Volatile
    private var cached: MockData? = null

    fun load(context: Context): MockData {
        cached?.let { return it }
        val text = context.applicationContext.assets.open(FILE_NAME)
            .bufferedReader()
            .use { it.readText() }
        return parse(text).also { cached = it }
    }

    fun getUsers(context: Context): List<User> = load(context).users

    fun findByEmail(context: Context, email: String): User? =
        getUsers(context).firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

    fun findById(context: Context, userId: String): User? =
        getUsers(context).firstOrNull { it.userId == userId }

    // ---- JSON parsing -------------------------------------------------------

    fun parse(json: String): MockData {
        val root = JSONObject(json)
        return MockData(
            version = root.optString("version"),
            lastUpdated = root.optString("last_updated"),
            users = root.optJSONArray("users").mapObjects(::parseUser),
            rooms = root.optJSONArray("rooms").mapObjects(::parseRoom),
            devices = root.optJSONArray("devices").mapObjects(::parseDevice),
            historyLogs = root.optJSONArray("history_logs").mapObjects(::parseHistory),
            securityLogs = root.optJSONArray("security_logs").mapObjects(::parseSecurity)
        )
    }

    fun parseRoom(o: JSONObject) = RoomTelemetry(
        roomId = o.getString("room_id"),
        patientName = o.optString("patient_name"),
        age = o.optInt("age"),
        csiStatus = o.optString("csi_status", "OFFLINE"),
        fallDetected = o.optBoolean("fall_detected"),
        fallConfidence = o.optDouble("fall_confidence", 0.0),
        heartRateBpm = o.optInt("heart_rate_bpm"),
        spo2Percent = o.optInt("spo2_percent"),
        temperatureC = o.optDouble("temperature_c", 0.0),
        breathingRpm = o.optInt("breathing_rpm"),
        timestamp = o.getString("timestamp"),
        nonce = o.getString("nonce")
    )

    private fun parseUser(o: JSONObject) = User(
        userId = o.getString("user_id"),
        name = o.getString("name"),
        role = o.getString("role"),
        email = o.getString("email"),
        twoFaEnabled = o.optBoolean("two_fa_enabled", false),
        mockOtp = o.optString("mock_otp")
    )

    private fun parseDevice(o: JSONObject) = Device(
        deviceId = o.getString("device_id"),
        roomId = o.optString("room_id"),
        type = o.optString("type"),
        status = o.optString("status"),
        rssiDbm = o.optInt("rssi_dbm"),
        tlsVerified = o.optBoolean("tls_verified"),
        lastSeen = o.optString("last_seen")
    )

    private fun parseHistory(o: JSONObject) = HistoryLog(
        eventId = o.getString("event_id"),
        roomId = o.optString("room_id"),
        eventType = o.optString("event_type"),
        details = o.optString("details"),
        heartRateBpm = o.optInt("heart_rate_bpm"),
        timestamp = o.optString("timestamp")
    )

    private fun parseSecurity(o: JSONObject) = SecurityLog(
        logId = o.getString("log_id"),
        attackType = o.optString("attack_type"),
        roomId = o.optString("room_id"),
        sourceId = o.optString("source_id"),
        reason = o.optString("reason"),
        actionTaken = o.optString("action_taken"),
        timestamp = o.optString("timestamp")
    )

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return List(length()) { i -> transform(getJSONObject(i)) }
    }
}
