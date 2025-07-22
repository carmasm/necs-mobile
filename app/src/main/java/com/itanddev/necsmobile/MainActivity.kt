package com.itanddev.necsmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.api.RetrofitClient
import com.itanddev.necsmobile.ui.HomeActivity

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

        setContentView(binding.root)


        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(LoginRequest("${username}@necshn.com", password))

                    if (response.isSuccessful) {
                        val token = response.body()?.token
                        Toast.makeText(
                            this@MainActivity,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Save token and navigate to next screen
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java).apply {
                            putExtra("AUTH_TOKEN", token)
                        })
                        finish()

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

//    private fun checkForUpdates() {
//        firebaseAppDistribution.checkForNewRelease()
//            .addOnSuccessListener { release ->
//                if (release != null) {
//                    showUpdateDialog() // New release available
//                }
//            }
//            .addOnFailureListener { exception ->
//                // Handle error (log it or show message)
////                Log.e("UpdateCheck", "Failed to check for updates", exception)
//                Toast.makeText(this, "Failed to check for updates: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun showUpdateDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("New Update Available")
//            .setMessage("Download and install the latest version?")
//            .setPositiveButton("Update") { _, _ ->
//                downloadAndInstallUpdate()
//            }
//            .setNegativeButton("Later", null)
//            .show()
//    }
//
//    private fun downloadAndInstallUpdate() {
//        firebaseAppDistribution.updateApp().addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Toast.makeText(this, "Download and install failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

//    private fun checkForUpdates() {
//        appDistribution.updateIfNewerAvailable()
//            .addOnProgressListener { updateProgress ->
//                // Show progress if needed
//            }
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful && task.result == true) {
//                    // Update available, show dialog
//                    showUpdateDialog()
//                }
//            }
//    }
//
//    private fun showUpdateDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("New Update Available")
//            .setMessage("A new version of the app is available. Update now?")
//            .setPositiveButton("Update") { _, _ ->
//                startUpdate()
//            }
//            .setNegativeButton("Later") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//    private fun startUpdate() {
//        firebaseAppDist.startUpdate()
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
}