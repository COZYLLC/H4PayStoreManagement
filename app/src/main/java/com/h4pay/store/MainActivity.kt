package com.h4pay.store

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.h4pay.store.customDialogs.yesOnlyDialog
import com.h4pay.store.networking.getOrderList
import com.h4pay.store.networking.getProdList
import com.h4pay.store.networking.tools.checkUpdate
import com.h4pay.store.networking.tools.permissionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

var prodList : JSONArray = getProdList().execute().get()
val permissionALL = 1
val permissionList = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)



class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var mDownloadQueueId:Long? = null
    private var mFileName:String? = null
    private var lastestVersion:String? = null
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2




    fun checkConnectivity():Boolean{
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val status = networkInfo != null && networkInfo.isConnected
        return status
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(mCompleteReceiver)
    }

    override fun onResume() {
        super.onResume()
        val completeFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(mCompleteReceiver, completeFilter)
    }

    fun getVersionInfo(context: Context): String {
        val i = context.packageManager.getPackageInfo(context.packageName, 0)
        return i.versionName
    }

    fun updateChecker():Int{
        var jsonObject:JSONObject = JSONObject()
        try {
            jsonObject = checkUpdate().execute().get()
        }catch(e: Exception){
            Log.e(TAG, "updateServer dead..")
            yesOnlyDialog(this, "업데이트 서버와 연결할 수 없습니다. 오프라인 모드로 실행합니다.", {}, "오류", null)
            return 0
        }
        val version = jsonObject.getDouble("version")
        val changes = jsonObject.getString("changes")
        val versionName = getVersionInfo(this)

        if (versionName.toDouble() < version){
            yesOnlyDialog(this, "$version 업데이트가 있어요!\n변경점: $changes",
                    { downloadApp(version) }, "업데이트", R.drawable.ic_baseline_settings_24 )

            return 1
        }
        return 0
    }
    fun downloadApp(version:Double){
        var mDownloadManager: DownloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val versionStr = version.toString().split("\\.".toRegex())
        Log.e(TAG, "https://yoon-lab.xyz/h4pay/files/apk/h4pay_manager_" + versionStr[0] + "_" + versionStr[1] + ".apk")
        val url = Uri.parse("https://yoon-lab.xyz/h4pay/files/apk/h4pay_manager_" + versionStr[0] + "_" + versionStr[1] + ".apk")
        val request: DownloadManager.Request = DownloadManager.Request(url)
        request.setTitle("매점 앱 업데이트")
        request.setDescription("다운로드 중입니다...")
        request.setDestinationInExternalPublicDir( Environment.DIRECTORY_DOWNLOADS, "h4pay_manager_" + versionStr[0] + "_" + versionStr[1] + ".apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI and DownloadManager.Request.NETWORK_MOBILE)
        mFileName = "h4pay_manager_" + versionStr[0] + "_" + versionStr[1] + ".apk"
        mDownloadQueueId = mDownloadManager.enqueue(request)
        Log.e(TAG, mFileName + ", " + mDownloadQueueId.toString())
        lastestVersion = version.toString().split("\\.".toRegex())[0] + "_"+ version.toString().split("\\.".toRegex())[1]
    }

    private val mCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e(TAG, "receiver")
            val action = intent.action
            if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val intent = Intent(Intent.ACTION_VIEW)
                val context = applicationContext
                val apkUri = FileProvider.getUriForFile(context, "com.h4pay.store", File(Environment.getExternalStorageDirectory().absolutePath + "/Download/h4pay_manager_$lastestVersion.apk"))
                Log.e(TAG, apkUri.toString())
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                finish()
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Not found. Cannot open file.", Toast.LENGTH_SHORT)
                            .show()
                    e.printStackTrace()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.d("DEBUG","Permission: "+permissions[0]+ "was "+grantResults[0])
            if (updateChecker() == 0){
                checkConnectivity()
            }
        }
        else {
            Log.d("DEBUG","Permission denied");
            // TODO : 퍼미션이 거부되는 경우에 대한 코드
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val time = findViewById<TextView>(R.id.time)

        if (checkConnectivity() == false) {
            Log.d(TAG, "인터넷 연결 X")
            yesOnlyDialog(this, "인터넷에 연결되어있지 않습니다. 확인 후 다시 이용 바랍니다.",
                    { android.os.Process.killProcess(android.os.Process.myPid()) }, "", null )

        }else{
            Log.d(TAG, "업데이트체크")
            if (!permissionManager.hasPermissions(this, permissionList)) {
                ActivityCompat.requestPermissions(this, permissionList, permissionALL)
            } else {
                updateChecker()
            }
        }

        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire()
                }
            }

        val mHandler = Handler()
        val thread = Thread(Runnable{
            run{
                while (true){
                        runOnUiThread{
                            val cal = Calendar.getInstance()
                            var min = cal.get(Calendar.MINUTE).toString()
                            var hour = cal.get(Calendar.HOUR).toString()
                            min = (if (min.toInt() < 10) "0" else "") + min
                            hour = (if (hour.toInt() < 10) "0" else "") + hour

                            time.setText("$hour : $min")
                        }
                    Thread.sleep(10000)
                }
            }
        })
        thread.start()

        val exchangeButton = findViewById<Button>(R.id.exchangeButton)
        exchangeButton.setOnClickListener {
            var exchange = Intent(this, exchangeActivity::class.java)
            startActivity(exchange)
        }

        val orderListButton = findViewById<Button>(R.id.orderListButton)
        orderListButton.setOnClickListener {
            var orderList = Intent(this, orderList::class.java)
            startActivity(orderList)
        }

        val makeNoticeButton = findViewById<Button>(R.id.makeNoticeButton)
        makeNoticeButton.setOnClickListener {
            Toast.makeText(this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show()
        }

        val csButton = findViewById<Button>(R.id.csButton)
        csButton.setOnClickListener {
            Toast.makeText(this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show()
        }

        val callDeveloperButton = findViewById<Button>(R.id.callDeveloperButton)
        callDeveloperButton.setOnClickListener {
            Toast.makeText(this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.stockManager).setOnClickListener {
            var stockManager = Intent(this, stockManagerActivity::class.java)
            startActivity(stockManager)
        }

        findViewById<Button>(R.id.calculation).setOnClickListener {
            calculation().calculate(this, getOrderList().execute().get())
        }
    }
}