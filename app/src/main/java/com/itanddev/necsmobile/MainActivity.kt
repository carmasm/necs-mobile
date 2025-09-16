package com.itanddev.necsmobile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.itanddev.necsmobile.data.api.Prefs
import com.itanddev.necsmobile.data.api.RetrofitClient.necsApiService

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.api.RetrofitClient
import com.itanddev.necsmobile.data.api.SharedPreferencesHelper
import com.itanddev.necsmobile.ui.HomeActivity
import com.itanddev.necsmobile.ui.UpdateManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UpdateManager.checkForUpdate(this)

        binding.settingsButton.setOnClickListener {
            showChangeUrlDialog()
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = necsApiService.loginNecs(LoginRequest(username, password))

                    if (response.isSuccessful) {
                        val type = response.body()?.type
                        val message = response.body()?.message

                        if (type == "success") {

                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()

                            // Save token and navigate to next screen
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                            finish()
                        }
                        else {
                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val message = response.body()?.message

                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showChangeUrlDialog() {
        val current = Prefs.getBaseUrl(this)
        val editText = EditText(this).apply {
            setText(current)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setSelection(text.length)
            hint = "https://your-host:port/"
        }

        AlertDialog.Builder(this)
            .setTitle("NECS base URL")
            .setMessage("Change the server base URL used for NECS API calls.")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                var newUrl = editText.text.toString().trim()
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!newUrl.endsWith("/")) newUrl += "/"
                Prefs.setBaseUrl(this, newUrl)

                // Optionally show a toast and/or re-create any cached retrofit instance:
                Toast.makeText(this, "Base URL saved: $newUrl", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Default") { _, _ ->
                Prefs.setBaseUrl(this, Prefs.getDefault())
                Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}