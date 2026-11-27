package com.example.safeward.model

/** Matches an entry in the `users` array of assets/mock_data.json. */
data class User(
    val userId: String,
    val name: String,
    val role: String,
    val email: String,
    val twoFaEnabled: Boolean,
    val mockOtp: String
)
