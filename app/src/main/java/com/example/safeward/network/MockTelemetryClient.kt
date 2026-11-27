package com.example.safeward.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.safeward.data.MockDataRepository
import java.util.concurrent.Executors

/**
 * Week-1 implementation of [TelemetryClient]: replays assets/mock_data.json as if the
 * messages arrived from the MQTT broker, passing every room message through
 * [MessageValidator] exactly like live traffic will be.
 *
 * It also re-sends one message at the end to demonstrate replay rejection.
 */
class MockTelemetryClient(
    context: Context,
    private val config: ServerConfig = ServerConfig.DEFAULT,
    private val simulatedDelayMs: Long = 400
) : TelemetryClient {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    override var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    @Volatile
    private var listener: TelemetryClient.Listener? = null

    override fun connect(listener: TelemetryClient.Listener) {
        this.listener = listener
        setState(ConnectionState.CONNECTING, "Loading mock_data.json…")

        executor.execute {
            try {
                val data = MockDataRepository.load(appContext)

                // Mock data has fixed timestamps, so use its "last_updated" as the clock.
                val mockNow = MessageValidator.parseTimestamp(data.lastUpdated)
                    ?: System.currentTimeMillis()
                val validator = MessageValidator(config.maxMessageAgeSeconds, { mockNow })

                pause()
                setState(ConnectionState.CONNECTED, "Mock broker (data v${data.version})")

                data.devices.forEach { device -> post { it.onDeviceStatus(device) } }

                data.rooms.forEach { room ->
                    pause()
                    deliverRoom(validator, room)
                }

                data.securityLogs.forEach { log -> post { it.onSecurityEvent(log) } }

                // Replay simulation: an attacker re-sends a captured packet.
                data.rooms.firstOrNull()?.let { captured ->
                    pause()
                    deliverRoom(validator, captured)
                }
            } catch (e: Exception) {
                setState(ConnectionState.ERROR, "Failed to read mock data: ${e.message}")
            }
        }
    }

    private fun deliverRoom(validator: MessageValidator, room: com.example.safeward.model.RoomTelemetry) {
        when (val result = validator.validate(room.nonce, room.timestamp)) {
            is MessageValidator.Result.Accepted -> post { it.onRoomTelemetry(room) }
            is MessageValidator.Result.Rejected ->
                post { it.onMessageRejected(room.roomId, "${result.attackType}: ${result.reason}") }
        }
    }

    override fun disconnect() {
        listener = null
        state = ConnectionState.DISCONNECTED
        executor.shutdownNow()
    }

    private fun pause() {
        if (simulatedDelayMs > 0) Thread.sleep(simulatedDelayMs)
    }

    private fun setState(newState: ConnectionState, message: String?) {
        state = newState
        post { it.onStateChanged(newState, message) }
    }

    private fun post(action: (TelemetryClient.Listener) -> Unit) {
        mainHandler.post { listener?.let(action) }
    }
}
