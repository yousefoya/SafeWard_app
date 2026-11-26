package com.example.safeward

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TwoFactorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor)

        val email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        val role = intent.getStringExtra(EXTRA_ROLE).orEmpty()

        findViewById<TextView>(R.id.accountText).text =
            "Verification code for $email ($role)"

        val codeInput = findViewById<EditText>(R.id.verificationCodeInput)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        verifyButton.setOnClickListener {
            if (codeInput.text.toString() == DEMO_CODE) {
                Toast.makeText(this, "2FA verified for $role", Toast.LENGTH_LONG).show()
                verifyButton.isEnabled = false
                verifyButton.text = "Verified"
            } else {
                codeInput.error = "Incorrect code. Try 123456."
            }
        }
    }

    companion object {
        const val EXTRA_EMAIL = "com.example.safeward.EXTRA_EMAIL"
        const val EXTRA_ROLE = "com.example.safeward.EXTRA_ROLE"
        private const val DEMO_CODE = "123456"
    }
}