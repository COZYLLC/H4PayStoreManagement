package com.h4pay.store

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class H4PayInfo : AppCompatActivity(){

    private lateinit var logoImageView: ImageView;
    private lateinit var currentVerTextView: TextView;
    private lateinit var latestVerTextView: TextView
    private lateinit var updateButton:LinearLayout

    fun UiInit() {
        logoImageView = findViewById(R.id.LogoImageView)
        currentVerTextView = findViewById(R.id.currVersion)
        latestVerTextView = findViewById(R.id.latestVersion)
        updateButton = findViewById(R.id.updateButton)
        currentVerTextView.text = BuildConfig.VERSION_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_info)
        UiInit()
        val recentVersion = getRecentVersion(this)
        if (recentVersion != null) {
            latestVerTextView.text = recentVersion.versionName.toString()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("DEBUG", "Permission: " + permissions[0] + "was " + grantResults[0])
            update()

        } else {
            Log.d("DEBUG", "Permission denied");
            // TODO : 퍼미션이 거부되는 경우에 대한 코드
        }
    }

    fun update() {
        val versionToUpdate = updateChecker(this)
        if (versionToUpdate != null) {
            customDialogs.yesOnlyDialog(
                this,
                "${versionToUpdate.versionName} 업데이트가 있어요!\n변경점: ${versionToUpdate.changes}",
                { downloadApp(this, versionToUpdate.versionName, versionToUpdate.url) },
                "업데이트",
                R.drawable.ic_baseline_settings_24
            )
        } else {
            Toast.makeText(this, "최신버전을 사용하고 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }


}