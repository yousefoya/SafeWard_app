package com.example.safeward

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

            when {
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    emailInput.error = "Enter a valid email address"

                password.isBlank() ->
                    passwordInput.error = "Enter your password"

                else -> {
                    val role = roleSpinner.selectedItem.toString()
                    val intent = Intent(this, TwoFactorActivity::class.java).apply {
                        putExtra(TwoFactorActivity.EXTRA_EMAIL, email)
                        putExtra(TwoFactorActivity.EXTRA_ROLE, role)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}