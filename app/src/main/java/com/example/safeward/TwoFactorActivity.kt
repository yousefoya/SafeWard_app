package com.example.safeward

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safeward.data.MockDataRepository
import com.example.safeward.network.SessionManager
import com.example.safeward.ui.security.SecurityAuditActivity

class TwoFactorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor)

        val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        val user = MockDataRepository.findById(this, userId)
        if (user == null) {
            Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.accountText).text =
            "Verification code for ${user.email} (${user.role})"

        // Demo only: there is no SMS/email gateway yet, so show where the code comes from.
        findViewById<TextView>(R.id.demoHintText).text =
            "Demo only: use the mock_otp for ${user.name} in mock_data.json"

        val codeInput = findViewById<EditText>(R.id.verificationCodeInput)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        verifyButton.setOnClickListener {
            if (codeInput.text.toString().trim() == user.mockOtp) {
                Toast.makeText(this, "Welcome ${user.name} (${user.role})", Toast.LENGTH_LONG).show()
                // 2FA passed => open an encrypted session (Android Keystore, AES-GCM).
                SessionManager(this).start(user)
                // Route Security role to Zeina's Security Audit Log; other roles to Network Test until dashboards land.
                val targetActivity = if (user.role.equals("Security", ignoreCase = true)) {
                    SecurityAuditActivity::class.java
                } else {
                    NetworkTestActivity::class.java
                }
                startActivity(
                    Intent(this, targetActivity)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            } else {
                codeInput.error = "Incorrect verification code"
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "com.example.safeward.EXTRA_USER_ID"
    }
}
