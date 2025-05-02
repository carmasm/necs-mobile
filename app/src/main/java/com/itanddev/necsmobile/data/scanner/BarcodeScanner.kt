package com.itanddev.necsmobile.data.scanner

import android.content.Context
import android.util.Log
import com.opticon.scannersdk.scanner.ScannerManager
import com.opticon.scannersdk.scanner.ScannerType

object BarcodeScanner {
    private const val TAG = "BarcodeScanner"
    var scanner: com.opticon.scannersdk.scanner.Scanner? = null
    private var scannerManager: ScannerManager? = null

    fun initialize(context: Context) {
        scannerManager = ScannerManager.getInstance(context)
        scannerManager?.scannerInfoList?.forEach { info ->
            if (info.type == ScannerType.SOFTWARE_SCANNER) {
                scanner = scannerManager?.getScanner(info)
                Log.d(TAG, "Scanner initialized: ${scanner != null}")
//                break
            }
        }
    }

    fun startScanning() {
        scanner?.startScan()
    }

    fun stopScanning() {
        scanner?.stopScan()
    }

    fun release() {
        scanner?.deinit()
        scanner = null
        scannerManager = null
    }
}