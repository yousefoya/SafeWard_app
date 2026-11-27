package com.example.safeward.network

import android.os.Handler
import android.os.Looper

/**
 * Week-2 placeholder for the real MQTT-over-TLS connection.
 *
 * Implementation plan (Yousef):
 *  1. Add the client library (HiveMQ MQTT Client or Eclipse Paho) to app/build.gradle.kts.
 *  2. Build an SSLContext that trusts ONLY the SafeWard CA (res/raw/safeward_ca.crt),
 *     TLS 1.2+ only, and verify the broker hostname.
 *  3. Authenticate with the session token from [SessionManager] (MQTT username/password
 *     or client certificate, as agreed with the backend team).
 *  4. Subscribe to [Topics.ROOM_TELEMETRY], [Topics.FALL_ALERTS], [Topics.DEVICE_STATUS],
 *     [Topics.SECURITY_LOG] with QoS 1.
 *  5. Parse each payload with MockDataRepository.parseRoom(...) (same JSON contract),
 *     run it through [MessageValidator] and forward it to the [TelemetryClient.Listener].
 *  6. Auto-reconnect with exponential back-off; report state changes.
 */
class MqttTelemetryClient(
    private val config: ServerConfig
) : TelemetryClient {

    private val mainHandler = Handler(Looper.getMainLooper())

    override var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    override fun connect(listener: TelemetryClient.Listener) {
        state = ConnectionState.ERROR
        mainHandler.post {
            listener.onStateChanged(
                state,
                "Live MQTT (${config.brokerUri}) is not implemented yet — use MOCK mode"
            )
        }
    }

    override fun disconnect() {
        state = ConnectionState.DISCONNECTED
    }
}
