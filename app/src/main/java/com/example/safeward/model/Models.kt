package com.example.safeward.model

/** Data models matching assets/mock_data.json (Week 1 data contract). */

data class RoomTelemetry(
    val roomId: String,
    val patientName: String,
    val age: Int,
    val csiStatus: String,          // NORMAL | MOVEMENT | FALL_DETECTED | OFFLINE
    val fallDetected: Boolean,
    val fallConfidence: Double,
    val heartRateBpm: Int,
    val spo2Percent: Int,
    val temperatureC: Double,
    val breathingRpm: Int,
    val timestamp: String,
    val nonce: String
)

data class Device(
    val deviceId: String,
    val roomId: String,
    val type: String,
    val status: String,
    val rssiDbm: Int,
    val tlsVerified: Boolean,
    val lastSeen: String
)

data class HistoryLog(
    val eventId: String,
    val roomId: String,
    val eventType: String,
    val details: String,
    val heartRateBpm: Int,
    val timestamp: String
)

data class SecurityLog(
    val logId: String,
    val attackType: String,
    val roomId: String,
    val sourceId: String,
    val reason: String,
    val actionTaken: String,
    val timestamp: String
)

data class MockData(
    val version: String,
    val lastUpdated: String,
    val users: List<User>,
    val rooms: List<RoomTelemetry>,
    val devices: List<Device>,
    val historyLogs: List<HistoryLog>,
    val securityLogs: List<SecurityLog>
)
