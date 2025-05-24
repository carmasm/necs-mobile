package com.itanddev.necsmobile.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.itanddev.necsmobile.databinding.ActivityHomeBinding
import com.itanddev.necsmobile.data.scanner.BarcodeScanner
import com.opticon.scannersdk.scanner.BarcodeEventListener
import com.opticon.scannersdk.scanner.ReadData
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.itanddev.necsmobile.data.api.RetrofitClient.necsApiService
import com.itanddev.necsmobile.data.model.Invoice
import kotlinx.coroutines.launch
import retrofit2.Response
import okhttp3.ResponseBody
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HomeActivity : AppCompatActivity(), BarcodeEventListener {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var tvApiResponse: TextView
    private lateinit var etManualInvoiceId: TextInputEditText
    private lateinit var btnManualSearch: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tvApiResponse = binding.tvApiResponse
        etManualInvoiceId = binding.etManualInvoiceId
        btnManualSearch = binding.btnManualSearch

        setupUi()
        setupScanner()
    }

    private fun setupUi() {
        binding.btnScan.setOnClickListener {
            startScanning()
        }

        btnManualSearch.setOnClickListener {
            val invoiceId = etManualInvoiceId.text.toString().trim()
            if (invoiceId.isNotEmpty()) {
                callNecsApi(invoiceId)
            } else {
                etManualInvoiceId.error = "Please enter an invoice ID"
            }
        }
    }

    private fun startScanning() {
        if (BarcodeScanner.scanner?.isConnected == true) {
            BarcodeScanner.startScanning()
            binding.btnScan.text = "Scanning..."
        } else {
            Toast.makeText(this, "Scanner not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupScanner() {
        BarcodeScanner.initialize(this)
        BarcodeScanner.scanner?.addBarcodeEventListener(this)
    }

    private fun callNecsApi(invoiceId: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = necsApiService.getInvoiceDetails(invoiceId)
                handleApiResponse(response)
            } catch (e: Exception) {
                handleApiError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleApiResponse(response: Response<ResponseBody>) {
        runOnUiThread {
            if (response.isSuccessful) {
                val jsonString = response.body()?.string()
                jsonString?.let {
                    displayInvoiceInfo(it)
                    binding.tvApiResponse.text = Gson().toJson(JsonParser.parseString(it))
                }
            } else {
                showError("API Error: ${response.code()}")
            }
        }
    }

    private fun displayInvoiceInfo(jsonString: String) {
        try {
            val invoice = Gson().fromJson(jsonString, Invoice::class.java)

            with(binding) {
                tvCustomerName.text = invoice.customerName
                tvTotal.text = "Total: ${formatCurrency(invoice.total)}"
                tvStatus.text = "Status: ${invoice.status}"
                tvInvoiceDate.text = "Date: ${formatDate(invoice.invoiceDate)}"
            }
        } catch (e: Exception) {
            showError("Error parsing response")
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun handleApiError(e: Exception) {
        runOnUiThread {
            showError("Error: ${e.localizedMessage}")
            binding.tvApiResponse.text = e.stackTraceToString()
        }
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.btnScan.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
        val scannedText = readData.text

        runOnUiThread {
            binding.tvScanResult.text = "Scanned: ${readData.text}"
            binding.btnScan.text = "Start Scanning"
            callNecsApi(scannedText)
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