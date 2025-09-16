package com.itanddev.necsmobile.data.api

import android.content.Context

object SharedPreferencesHelper {
    private const val PREFS_NAME = "AppPreferences"
    private const val KEY_NECS_URL = "nec_url"

    fun saveNecsUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NECS_URL, url).apply()
    }

    fun getNecsUrl(context: Context, defaultUrl: String = "http://erp.necshn.com:8090/"): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NECS_URL, defaultUrl) ?: defaultUrl
    }
}