package com.example.safeward.network

import com.example.safeward.model.Device
import com.example.safeward.model.RoomTelemetry
import com.example.safeward.model.SecurityLog

/**
 * Single entry point the UI uses to receive live data.
 * Implementations: [MockTelemetryClient] (Week 1) and [MqttTelemetryClient] (Week 2+).
 * The UI must not care which one is active.
 */
interface TelemetryClient {

    val state: ConnectionState

    /** Starts the connection. Callbacks are delivered on the main thread. */
    fun connect(listener: Listener)

    fun disconnect()

    interface Listener {
        fun onStateChanged(state: ConnectionState, message: String? = null) {}
        fun onRoomTelemetry(room: RoomTelemetry) {}
        fun onDeviceStatus(device: Device) {}
        fun onSecurityEvent(log: SecurityLog) {}
        /** A message was dropped by [MessageValidator] (e.g. replay attempt). */
        fun onMessageRejected(roomId: String, reason: String) {}
    }
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
