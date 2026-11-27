package com.example.safeward.network

import org.junit.Assert.assertTrue
import org.junit.Test

class MessageValidatorTest {

    private val baseTime = MessageValidator.parseTimestamp("2026-11-27T12:30:00+03:00")!!

    @Test
    fun acceptsFreshUniqueMessage() {
        val validator = MessageValidator(maxAgeSeconds = 120, clock = { baseTime })
        val result = validator.validate("n_9f8a12c4", "2026-11-27T12:29:10+03:00")
        assertTrue(result is MessageValidator.Result.Accepted)
    }

    @Test
    fun rejectsDuplicateNonceAsReplayAttack() {
        val validator = MessageValidator(maxAgeSeconds = 120, clock = { baseTime })
        assertTrue(validator.validate("n_dup_01", "2026-11-27T12:29:50+03:00") is MessageValidator.Result.Accepted)

        val second = validator.validate("n_dup_01", "2026-11-27T12:29:55+03:00")
        assertTrue(second is MessageValidator.Result.Rejected && second.attackType == "REPLAY_ATTACK")
    }

    @Test
    fun rejectsExpiredTimestampAsReplayAttack() {
        val validator = MessageValidator(maxAgeSeconds = 120, clock = { baseTime })
        val result = validator.validate("n_old_01", "2026-11-27T12:20:00+03:00")
        assertTrue(result is MessageValidator.Result.Rejected && result.attackType == "REPLAY_ATTACK")
    }

    @Test
    fun rejectsBlankNonce() {
        val validator = MessageValidator(maxAgeSeconds = 120, clock = { baseTime })
        val result = validator.validate("   ", "2026-11-27T12:29:50+03:00")
        assertTrue(result is MessageValidator.Result.Rejected && result.attackType == "INVALID_MESSAGE")
    }
}
