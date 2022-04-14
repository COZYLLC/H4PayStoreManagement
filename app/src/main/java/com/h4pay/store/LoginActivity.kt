package com.h4pay.store

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Process
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.h4pay.store.databinding.ActivityLoginBinding
import com.h4pay.store.model.School
import com.h4pay.store.model.tokenFromStorageFlow
import com.h4pay.store.networking.H4PayService
import com.h4pay.store.networking.tools.networkInterceptor
import com.h4pay.store.networking.tools.permissionManager
import com.h4pay.store.util.isOnScreenKeyboardEnabled
import com.h4pay.store.util.openImm
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

var token: String? = null
const val keyboardDetectDelay: Long = 1000

class LoginActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var view: ActivityLoginBinding
    private lateinit var h4payService: H4PayService
    private lateinit var schools: List<School>
    private var selectedSchool: School? = null
    private var loaded = false
    private val TAG = "loginActivity"

    fun onKeyboardVisibilityChanged(opened: Boolean) {
        print("keyboard $opened")
    }

    private fun checkConnectivity(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        return networkInfo != null && networkInfo.isConnected
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

    private fun update() {
        val currentVersionName = getVersionInfo(this)

        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getVersionInfo()
            }.onSuccess {
                // Recent version found. Start download.
                if (currentVersionName.toDouble() < it.versionName.toDouble()) {
                    customDialogs.yesOnlyDialog(
                        this@LoginActivity,
                        "${it.versionName} 업데이트가 있어요!\n변경점: ${it.changes}",
                        {
                            downloadApp(this@LoginActivity, it.versionName.toDouble(), it.url)
                        },
                        "업데이트",
                        R.drawable.ic_baseline_settings_24
                    )
                }
            }.onFailure {
                Log.e("UpdateChecker", it.message!!)
                Toast.makeText(this@LoginActivity, "업데이트 검사에 실패했습니다. 앱을 종료합니다.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view.root.viewTreeObserver.addOnGlobalLayoutListener {
            // View의 focus가 변경됐을 때를 observe.
            if (!isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                Log.d("LoginActivity", "keyboard enabled")
                openImm(this, true)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = DataBindingUtil.setContentView(this, R.layout.activity_login)
        view.layout.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (loaded) {
                    view.layout.viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else {
                    false
                }
            }
        })
        view.schoolSpinner.setPopupBackgroundResource(R.drawable.round_button)
        view.schoolSpinner.setTitle("학교 선택")

        view.root.viewTreeObserver.addOnGlobalLayoutListener {
            // View의 focus가 변경됐을 때를 observe.
            if (!isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                Log.d("LoginActivity", "keyboard enabled")
                openImm(this, true)
            }

        }
        initService()
        if (!checkConnectivity()) {
            customDialogs.yesOnlyDialog(
                this, "인터넷에 연결되어있지 않습니다. 확인 후 다시 이용 바랍니다.",
                { Process.killProcess(Process.myPid()) }, "", null
            )
            return
        } else {
            if (!permissionManager.hasPermissions(this, permissionList)) {
                ActivityCompat.requestPermissions(this, permissionList, permissionALL)
            } else {
                update()
            }
        }

        lifecycleScope.launchWhenStarted {
            kotlin.runCatching {
                tokenFromStorageFlow(this@LoginActivity).collect {
                    if (it != null && it != "") {
                        token = it
                        loaded = true
                        val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(mainIntent)
                        finish()
                    } else {
                        throw Exception("token null")
                    }
                }
            }.onFailure {
                if (it.message == "token null") {
                    Toast.makeText(
                        this@LoginActivity,
                        "학교 로그인 정보를 불러올 수 없습니다. 로그인을 재시도해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Log.e(TAG, it.message.toString())
                loaded = true
                getSchools()
                view.submit.setOnClickListener {
                    login(selectedSchool?.id!!, view.password.text.toString())
                }
            }

        }

    }


    private fun encryptToSha256Md5(rawString: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(rawString.toByteArray(StandardCharsets.UTF_8))
        val encrypted = messageDigest.digest()
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun generateSessionId(): String {
        val secureRandom = SecureRandom()
        var rawSessionId = ""
        for (x in 0..30) {
            rawSessionId += secureRandom.nextInt().toChar()
        }
        Log.d(TAG, rawSessionId)
        return rawSessionId
    }

    private fun login(schoolId: String?, passwordRawString: String?) {
        if (schoolId == null || passwordRawString == null) {
            return
        }
        val encryptedPassword = encryptToSha256Md5(passwordRawString)
        var sessionId = generateSessionId()
        sessionId = encryptToSha256Md5(sessionId)

        Log.d(TAG, schoolId)
        Log.d(TAG, encryptedPassword)
        val body = JsonObject()
        body.addProperty("id", schoolId)
        body.addProperty("password", encryptedPassword)
        body.addProperty("sessionId", sessionId)
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.schoolLogin(body)
            }.onSuccess {
                Log.d(TAG, it.headers().toString())
                when {
                    it.code() == 200 -> {
                        token = it.headers()["x-access-token"]!!
                        if (token != null) {
                            Log.d(TAG, token!!)
                            selectedSchool?.token = token as String
                            selectedSchool?.saveToStorage(this@LoginActivity)
                            val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(mainIntent)
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "토큰 검증에 실패했습니다. 로그인을 재시도해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    it.code() == 400 -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "학교 또는 비밀번호가 올바르지 않습니다. 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        showServerError(this@LoginActivity)
                    }
                }
            }.onFailure {
                Log.d(TAG, it.message.toString());
                Toast.makeText(
                    this@LoginActivity,
                    "인증에 실패했습니다. 학교와 비밀번호가 올바른지 확인하세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun initService() {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(networkInterceptor)
            .build()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("${BuildConfig.API_URL}/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        h4payService = retrofit.create(H4PayService::class.java)

    }

    private fun getSchools() {
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getSchools()
            }.onSuccess {
                val schoolNames: MutableList<String> = listOf<String>().toMutableList()
                it.forEach { school ->
                    schoolNames.add(school.name)
                }
                schools = it
                val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
                    this@LoginActivity,
                    android.R.layout.simple_spinner_item,
                    schoolNames
                )
                view.schoolSpinner.adapter = spinnerAdapter
                view.schoolSpinner.onItemSelectedListener = this@LoginActivity
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedSchool = schools[position]
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}
