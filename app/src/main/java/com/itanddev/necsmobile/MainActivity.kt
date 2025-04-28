package com.itanddev.necsmobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.itanddev.necsmobile.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.api.RetrofitClient

class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val response = RetrofitClient.apiService.login(
//                        LoginRequest(username, password)
//                    )
//                    if (response.isSuccessful) {
//                        val token = response.body()?.token
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Login successful!",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        // Save token and navigate to next screen
//                    } else {
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Invalid credentials",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                } catch (e: Exception) {
//                    runOnUiThread {
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Network error: ${e.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(LoginRequest(username, password))

                    if (response.isSuccessful) {
                        val token = response.body()?.token
                        Toast.makeText(
                            this@MainActivity,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Save token and navigate to next screen
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Invalid credentials",
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
}