package com.h4pay.store

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.h4pay.store.networking.RetrofitInstance
import com.h4pay.store.repository.PrefsRepository
import kotlinx.coroutines.launch

class H4PayInfo : AppCompatActivity(){

    private lateinit var logoImageView: ImageView;
    private lateinit var currentVerTextView: TextView;
    private lateinit var latestVerTextView: TextView
    private lateinit var updateButton:LinearLayout
    private lateinit var signout: LinearLayout

    private fun uiInit() {
        logoImageView = findViewById(R.id.LogoImageView)
        currentVerTextView = findViewById(R.id.currVersion)
        latestVerTextView = findViewById(R.id.latestVersion)
        updateButton = findViewById(R.id.updateButton)
        currentVerTextView.text = BuildConfig.VERSION_NAME
        signout = findViewById(R.id.signout)
        signout.setOnClickListener {
           lifecycleScope.launch {
               PrefsRepository(this@H4PayInfo).signOut()
               Toast.makeText(this@H4PayInfo, "로그아웃이 완료되었습니다. 앱을 재실행합니다.", Toast.LENGTH_SHORT).show()
               val intent = Intent(this@H4PayInfo, LoginActivity::class.java)
               startActivity(intent)
               finish()
           }
        }
        updateButton.setOnClickListener {
            update()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_info)
        uiInit()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("DEBUG", "Permission: " + permissions[0] + "was " + grantResults[0])
            update()

        } else {
            Log.d("DEBUG", "Permission denied");
            // TODO : 퍼미션이 거부되는 경우에 대한 코드
        }
    }

    private fun update() {
        val currentVersionName = getVersionInfo(this)

        lifecycleScope.launch {
            kotlin.runCatching {
                RetrofitInstance.service.getVersionInfo()
            }.onSuccess {
                // Recent version found. Start download.
                latestVerTextView.text = it.versionName
                if (currentVersionName.toDouble() < it.versionName.toDouble()) {
                    customDialogs.yesOnlyDialog(
                        this@H4PayInfo,
                        "${it.versionName} 업데이트가 있어요!\n변경점: ${it.changes}",
                        {
                            downloadApp(this@H4PayInfo, it.versionName.toDouble(), it.url)
                        },
                        "업데이트",
                        R.drawable.ic_baseline_settings_24
                    )
                } else {
                    Toast.makeText(this@H4PayInfo, "최신버전을 사용하고 있습니다.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Log.e("UpdateChecker", it.message!!)
                Toast.makeText(this@H4PayInfo, "업데이트 검사에 실패했습니다. 앱을 종료합니다.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


}