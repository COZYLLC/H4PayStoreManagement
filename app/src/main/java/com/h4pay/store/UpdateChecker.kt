package com.h4pay.store

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
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
import com.h4pay.store.networking.Get
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class Version(val versionName: Double, val changes: String, val url: String) {

}

private val TAG = "UPDATER"
private var mDownloadQueueId: Long? = null
private var mFileName: String? = null
private var lastestVersion: String? = null
val permissionALL = 1
val permissionList =
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

fun getRecentVersion(context: Context): Version? {
    var jsonObject: JSONObject? = Get("${BuildConfig.API_URL}/version").execute().get()
    if (jsonObject == null) {
        AlertDialog.Builder(context)
            .setTitle("서버 오류")
            .setMessage("서버 오류로 인해 엡데이트 확인이 불가능합니다. 개발자에게 문의해주세요.")
        return null
    } else {
        val result = jsonObject.getJSONObject("result")
        val version = result.getDouble("version")
        val changes = result.getString("changes")
        val url = result.getString("url")
        return Version(versionName = version, changes = changes, url = url)
    }
    return null
}

fun updateChecker(context: Context): Version? {
    try {
        val versionName = getVersionInfo(context)
        val recentVersion: Version = getRecentVersion(context)!!;
        if (versionName.toDouble() < recentVersion.versionName) {
            return recentVersion
        } else {
            return null;
        }
    }catch (e:Exception) {
        Toast.makeText(context, "업데이트 검사에 실패했습니다. 앱을 종료합니다.", Toast.LENGTH_SHORT).show()
        return null;
    }

}

fun getVersionInfo(context: Context): String {
    val i = context.packageManager.getPackageInfo(context.packageName, 0)
    return i.versionName
}


fun downloadApp(context: Context, version: Double, url: String) {
    var mDownloadManager: DownloadManager =
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
    lastestVersion = version.toString().split("\\.".toRegex())[0] + "." + version.toString()
        .split("\\.".toRegex())[1]
}

private val mCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val intent = Intent(Intent.ACTION_VIEW)
            val context = context!!.applicationContext
            val apkUri = FileProvider.getUriForFile(
                context,
                "com.h4pay.store",
                File(Environment.getExternalStorageDirectory().absolutePath + "/Download/h4pay_manager_$lastestVersion.apk")
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
