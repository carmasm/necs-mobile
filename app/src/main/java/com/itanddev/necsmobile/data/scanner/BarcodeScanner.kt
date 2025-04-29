package com.itanddev.necsmobile.data.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object BarcodeScanner {
    private const val SCAN_ACTION = "com.opticon.emdk.scan.ACTION"
    private const val SCAN_RESULT = "com.opticon.emdk.scan.RESULT"

    private val _scanResult = MutableLiveData<String>()
    val scanResult: LiveData<String> = _scanResult

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val barcode = intent.getStringExtra(SCAN_RESULT)
            barcode?.let {
                _scanResult.postValue(it)
            }
        }
    }

    fun registerScanner(context: Context) {
//        val filter = IntentFilter(SCAN_ACTION)
//        context.registerReceiver(receiver, filter)
    }

    fun unregisterScanner(context: Context) {
//        context.unregisterReceiver(receiver)
    }

    fun triggerScan(context: Context) {
//        val intent = Intent(SCAN_ACTION)
//        intent.putExtra("com.opticon.emdk.scan.TRIGGER", true)
//        context.sendBroadcast(intent)
    }
}