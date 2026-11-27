package com.example.safeward.network

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Client-side anti-replay check for incoming telemetry, mirroring the backend:
 *  - every message must carry a nonce that was never seen before
 *  - its timestamp must be within [maxAgeSeconds] of the current clock
 *
 * @param clock returns "now" in epoch millis (injectable so mock data can be replayed).
 */
class MessageValidator(
    private val maxAgeSeconds: Long,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxRememberedNonces: Int = 1_000
) {

    sealed class Result {
        object Accepted : Result()
        data class Rejected(val attackType: String, val reason: String) : Result()
    }

    // Insertion-ordered so the oldest nonces are evicted first.
    private val seenNonces = LinkedHashSet<String>()

    @Synchronized
    fun validate(nonce: String, timestamp: String): Result {
        if (nonce.isBlank()) {
            return Result.Rejected("INVALID_MESSAGE", "Missing nonce")
        }
        if (nonce in seenNonces) {
            return Result.Rejected("REPLAY_ATTACK", "Duplicate nonce ($nonce)")
        }
        val sentAt = parseTimestamp(timestamp)
            ?: return Result.Rejected("INVALID_MESSAGE", "Invalid timestamp ($timestamp)")

        val ageSeconds = (clock() - sentAt) / 1000
        if (ageSeconds > maxAgeSeconds) {
            return Result.Rejected("REPLAY_ATTACK", "Expired timestamp (${ageSeconds}s old)")
        }
        if (ageSeconds < -maxAgeSeconds) {
            return Result.Rejected("INVALID_MESSAGE", "Timestamp is in the future")
        }

        seenNonces.add(nonce)
        if (seenNonces.size > maxRememberedNonces) {
            seenNonces.remove(seenNonces.first())
        }
        return Result.Accepted
    }

    companion object {
        /** Parses ISO-8601 with offset, e.g. 2026-11-27T12:29:50+03:00 (works on API 24+). */
        fun parseTimestamp(value: String): Long? = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(value)?.time
        } catch (e: ParseException) {
            null
        }
    }
}
