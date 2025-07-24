package com.itanddev.necsmobile.ui
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.getSystemService
import com.itanddev.necsmobile.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UpdateManager {
    private const val REPO_OWNER = "carmasm"  // Replace with your GitHub username
    private const val REPO_NAME = "necs-mobile"          // Replace with your repository name
    private const val APP_NAME = "NECSMobile"

    fun checkForUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.gitHubApiService.getLatestRelease(REPO_OWNER, REPO_NAME)
                val latestVersion = response.tagName.removePrefix("v")
                val apkUrl = response.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl

                if (apkUrl == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "APK not found in release", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val currentVersion = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName

                if (latestVersion != currentVersion) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(context, apkUrl, latestVersion)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Update check failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showUpdateDialog(context: Context, apkUrl: String, newVersion: String) {
        AlertDialog.Builder(context)
            .setTitle("New Version Available")
            .setMessage("Version $newVersion is available. Update now?")
            .setPositiveButton("Update") { _, _ -> downloadUpdate(context, apkUrl, newVersion) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadUpdate(context: Context, apkUrl: String, newVersion: String) {
        val downloadManager = context.getSystemService<DownloadManager>() ?: return

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("$APP_NAME Update")
            .setDescription("Downloading new version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "$APP_NAME-Update-$newVersion.apk"
            )

        downloadManager.enqueue(request)
        Toast.makeText(context, "Download started. Install when complete.", Toast.LENGTH_LONG).show()
    }

    fun installUpdate(context: Context) {
        val apkFile = java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$APP_NAME-Update.apk"
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.fromFile(apkFile),
                "application/vnd.android.package-archive"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}