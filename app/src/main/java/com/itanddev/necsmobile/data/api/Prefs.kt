package com.itanddev.necsmobile.data.api

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "necs_prefs"
    private const val KEY_NECS_URL = "necs_url"
    private const val DEFAULT_NECS_URL = "http://erp.necshn.com:8090/"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NECS_URL, DEFAULT_NECS_URL) ?: DEFAULT_NECS_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NECS_URL, normalized).apply()
    }

    fun getDefault(): String = DEFAULT_NECS_URL
}
