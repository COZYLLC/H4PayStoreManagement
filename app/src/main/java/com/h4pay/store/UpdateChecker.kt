package com.h4pay.store

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import kotlin.system.exitProcess

private val TAG = "UPDATER"
private var mDownloadQueueId: Long? = null
private var mFileName: String? = null
private var latestVersion: String? = null
const val permissionALL = 1
val permissionList =
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)


fun getVersionInfo(context: Context): String {
    val i = context.packageManager.getPackageInfo(context.packageName, 0)
    return i.versionName
}

fun downloadApp(context: Context, version: Double, url: String) {
    val mDownloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val versionStr = version.toString().split("\\.".toRegex())
    Log.e(
        TAG,
        url
    )
    val uri =
        Uri.parse(url)
    val request: DownloadManager.Request = DownloadManager.Request(uri)
    request.setTitle("매점 앱 업데이트")
    request.setDescription("다운로드 중입니다...")
    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        "h4pay_manager_" + versionStr[0] + "." + versionStr[1] + ".apk"
    );
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI and DownloadManager.Request.NETWORK_MOBILE)
    mFileName = "h4pay_manager_" + versionStr[0] + "." + versionStr[1] + ".apk"
    mDownloadQueueId = mDownloadManager.enqueue(request)
    Log.e(TAG, mFileName + ", " + mDownloadQueueId.toString())
    latestVersion = version.toString().split("\\.".toRegex())[0] + "." + version.toString()
        .split("\\.".toRegex())[1]
}

val mCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val intent = Intent(Intent.ACTION_VIEW)
            val context = context!!.applicationContext
            val apkUri = FileProvider.getUriForFile(
                context,
                "com.h4pay.store",
                File(Environment.getExternalStorageDirectory().absolutePath + "/Download/h4pay_manager_$latestVersion.apk")
            )
            Log.e(TAG, apkUri.toString())
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            try {
                context.startActivity(intent)
                exitProcess(0);
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_update_cannotOpen),
                    Toast.LENGTH_SHORT
                )
                    .show()
                e.printStackTrace()
            }
        }
    }
}
