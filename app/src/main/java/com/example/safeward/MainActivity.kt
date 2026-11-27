package com.example.safeward

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safeward.data.MockDataRepository

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val roleSpinner = findViewById<Spinner>(R.id.roleSpinner)
        val continueButton = findViewById<Button>(R.id.continueButton)

        continueButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            val selectedRole = roleSpinner.selectedItem.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email address"
                return@setOnClickListener
            }
            if (password.isBlank()) {
                passwordInput.error = "Enter your password"
                return@setOnClickListener
            }

            // Week 1: authenticate against the shared mock_data.json contract.
            val user = MockDataRepository.findByEmail(this, email)
            when {
                user == null ->
                    emailInput.error = "No SafeWard account found for this email"

                !user.role.equals(selectedRole, ignoreCase = true) ->
                    Toast.makeText(
                        this,
                        "This account is not registered as $selectedRole",
                        Toast.LENGTH_LONG
                    ).show()

                else -> {
                    val intent = Intent(this, TwoFactorActivity::class.java).apply {
                        putExtra(TwoFactorActivity.EXTRA_USER_ID, user.userId)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}