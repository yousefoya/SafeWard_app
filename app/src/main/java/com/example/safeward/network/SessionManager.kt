package com.example.safeward.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.safeward.model.User
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the authenticated session after a successful 2FA.
 *
 * - The session is encrypted with AES-256-GCM using a key held in the Android Keystore
 *   (the key never leaves secure hardware / cannot be exported).
 * - Sessions expire after [SESSION_TIMEOUT_MS].
 * - Week 1: the token is generated locally. Week 2+: it will be issued by the server.
 */
class SessionManager(context: Context) {

    data class Session(
        val userId: String,
        val role: String,
        val token: String,
        val expiresAt: Long
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAt
    }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun start(user: User): Session {
        val session = Session(
            userId = user.userId,
            role = user.role,
            token = newToken(),
            expiresAt = System.currentTimeMillis() + SESSION_TIMEOUT_MS
        )
        val json = JSONObject()
            .put("user_id", session.userId)
            .put("role", session.role)
            .put("token", session.token)
            .put("expires_at", session.expiresAt)
            .toString()
        prefs.edit().putString(KEY_SESSION, encrypt(json)).apply()
        return session
    }

    /** Returns the active session, or null if missing, expired or tampered with. */
    fun current(): Session? {
        val stored = prefs.getString(KEY_SESSION, null) ?: return null
        val session = try {
            val o = JSONObject(decrypt(stored))
            Session(
                userId = o.getString("user_id"),
                role = o.getString("role"),
                token = o.getString("token"),
                expiresAt = o.getLong("expires_at")
            )
        } catch (e: Exception) {
            null // decryption / GCM tag failure => treat as no session
        }
        if (session == null || session.isExpired) {
            clear()
            return null
        }
        return session
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    // ---- Crypto -------------------------------------------------------------

    private fun newToken(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipherText = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L   // 15 minutes

        private const val PREFS_NAME = "safeward_session"
        private const val KEY_SESSION = "session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "safeward_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
