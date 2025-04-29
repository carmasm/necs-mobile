package com.itanddev.necsmobile

import android.os.Bundle
//import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.itanddev.necsmobile.databinding.ActivityHomeBinding
import com.itanddev.necsmobile.data.scanner.BarcodeScanner

class activity_home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize scanner
        BarcodeScanner.registerScanner(this)

        // Observe scan results
        BarcodeScanner.scanResult.observe(this, Observer { result ->
            binding.tvScanResult.text = "Scanned: $result"
        })

        binding.btnScan.setOnClickListener {
            BarcodeScanner.triggerScan(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BarcodeScanner.unregisterScanner(this)
    }
}