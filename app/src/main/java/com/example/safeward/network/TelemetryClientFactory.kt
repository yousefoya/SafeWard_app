package com.example.safeward.network

import android.content.Context

/** Picks the [TelemetryClient] implementation based on [ServerConfig.mode]. */
object TelemetryClientFactory {
    fun create(context: Context, config: ServerConfig = ServerConfig.DEFAULT): TelemetryClient =
        when (config.mode) {
            ServerConfig.Mode.MOCK -> MockTelemetryClient(context, config)
            ServerConfig.Mode.LIVE -> MqttTelemetryClient(config)
        }
}
