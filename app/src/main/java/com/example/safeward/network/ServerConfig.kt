package com.example.safeward.network

/**
 * Server / broker connection settings.
 *
 * Week 1: [Mode.MOCK] reads assets/mock_data.json.
 * Week 2+: switch to [Mode.LIVE] once the backend team shares the broker address
 * and CA certificate.
 */
data class ServerConfig(
    val mode: Mode = Mode.MOCK,
    val brokerHost: String = "broker.safeward.local",   // TODO: real broker host from backend team
    val brokerPort: Int = 8883,                         // MQTT over TLS
    val useTls: Boolean = true,
    val clientIdPrefix: String = "safeward-android-",
    val keepAliveSeconds: Int = 30,
    /** Messages older than this are rejected (anti-replay). */
    val maxMessageAgeSeconds: Long = 120
) {
    enum class Mode { MOCK, LIVE }

    val brokerUri: String
        get() = "${if (useTls) "ssl" else "tcp"}://$brokerHost:$brokerPort"

    companion object {
        val DEFAULT = ServerConfig()
    }
}

/** MQTT topic contract (to be confirmed with the backend team). */
object Topics {
    const val ROOM_TELEMETRY = "safeward/ward/+/telemetry"   // + = room_id
    const val FALL_ALERTS = "safeward/ward/+/alerts"
    const val DEVICE_STATUS = "safeward/devices/+/status"
    const val SECURITY_LOG = "safeward/security/log"

    fun roomTelemetry(roomId: String) = "safeward/ward/$roomId/telemetry"
}
