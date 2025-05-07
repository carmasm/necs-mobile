package com.itanddev.necsmobile.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityHomeBinding
import com.itanddev.necsmobile.data.scanner.BarcodeScanner
import com.opticon.scannersdk.scanner.BarcodeEventListener
import com.opticon.scannersdk.scanner.ReadData
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.itanddev.necsmobile.data.api.RetrofitClient.necsApiService
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), BarcodeEventListener {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var tvApiResponse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tvApiResponse = binding.tvApiResponse

        // Initialize scanner
        BarcodeScanner.initialize(this)
        BarcodeScanner.scanner?.addBarcodeEventListener(this)

        binding.btnScan.setOnClickListener {
            if (BarcodeScanner.scanner?.isConnected == true) {
                BarcodeScanner.startScanning()
            } else {
                Toast.makeText(this, "Scanner not connected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCallApi.setOnClickListener {
            callNecsApi("499189") // Hardcoded invoice ID for example
        }
    }

    private fun callNecsApi(invoiceId: String) {
        lifecycleScope.launch {
            try {
                val response = necsApiService.getInvoiceDetails(invoiceId)
                val statusCode = response.code()
                val message = when {
                    response.isSuccessful -> {
                        val responseStr = response.body()?.string() ?: ""
                        tvApiResponse.text = responseStr

                        "API Call Successful (${statusCode})"
                    }
                    else -> {
                        tvApiResponse.text = "Error: ${response.errorBody()?.string()}"
                        "API Error (${statusCode})"
                    }
                }

                showToast(message)
            } catch (e: Exception) {
                tvApiResponse.text = "Exception: ${e.localizedMessage}"
                showToast("API Call Failed: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@HomeActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        BarcodeScanner.scanner?.init()
    }

    override fun onPause() {
        super.onPause()
        BarcodeScanner.scanner?.deinit()
    }

    override fun onDestroy() {
        super.onDestroy()
        BarcodeScanner.release()
    }

    // BarcodeEventListener implementation
    override fun onReadData(readData: ReadData) {
        runOnUiThread {
            binding.tvScanResult.text = "Scanned: ${readData.text}"
            binding.btnScan.text = "Start Scanning"
        }
    }

    override fun onTimeout() {
        runOnUiThread {
            binding.btnScan.text = "Start Scanning"
            Toast.makeText(this, "Scan timed out", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDecodeStart() {
        runOnUiThread {
            binding.btnScan.text = "Scanning..."
        }
    }

    override fun onDecodeStop() {
        runOnUiThread {
            binding.btnScan.text = "Start Scanning"
        }
    }

    // Other required interface methods (empty implementations)
    override fun onConnect() {}
    override fun onDisconnect() {}
    override fun onImageData(bitmap: Bitmap?, byteArray: ByteArray?, i: Int) {}
}