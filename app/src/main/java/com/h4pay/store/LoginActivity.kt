package com.h4pay.store

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.h4pay.store.databinding.ActivityLoginBinding
import com.h4pay.store.model.School
import com.h4pay.store.model.Version
import com.h4pay.store.networking.tools.permissionManager
import com.h4pay.store.repository.PrefsRepository
import com.h4pay.store.util.isOnScreenKeyboardEnabled
import com.h4pay.store.util.openImm
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response


const val keyboardDetectDelay: Long = 1000

class LoginActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var view: ActivityLoginBinding
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
            viewModel.fetchVersionInfo()

        } else {
            Log.d("DEBUG", "Permission denied");
            // TODO : 퍼미션이 거부되는 경우에 대한 코드
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


    private val prefsRepository: PrefsRepository by lazy {
        PrefsRepository(this)
    }
    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(
            viewModelStore,
            LoginViewModelFactory(prefsRepository)
        )[LoginViewModel::class.java]
    }

    private val versionCollector = FlowCollector<State<Version>> { state ->
        // Error Handling
        if (state is State.Error || (state is State.Success && state.data == null)) {
            customDialogs.yesOnlyDialog(
                this,
                "버전 확인에 실패했어요. 운영사에 문의해주세요.",
                {},
                "오류",
                null
            )
            return@FlowCollector
        }
        if (state is State.Success) {
            val version = state.data!!
            // Recent version found. Start download.
            val currentVersionName = getVersionInfo(this)
            if (currentVersionName.toDouble() < version.versionName.toDouble()) {
                customDialogs.yesOnlyDialog(
                    this,
                    "${version.versionName} 업데이트가 있어요!\n변경점: ${version.changes}",
                    {
                        downloadApp(this, version.versionName.toDouble(), version.url)
                    },
                    "업데이트",
                    R.drawable.ic_baseline_settings_24
                )
            }
        }
    }

    private val loginResultCollector = FlowCollector<State<Response<String>>> { state ->
        if (state is State.Error || state is State.Success && (state.data == null))
            customDialogs.yesOnlyDialog(this, "로그인에 실패했어요. 학교와 비밀번호를 확인해주세요.", {}, "오류", null)
        if (state is State.Success) {
            val res = state.data!!
            when {
                res.code() == 200 -> {
                    val token = res.headers()["x-access-token"]!!
                    // Todo: Save Token to Store
                    prefsRepository.setSchoolToken(token)
                    App.token = token
                    val mainIntent =
                        Intent(this, MainActivity::class.java)
                    startActivity(mainIntent)
                }
                res.code() == 400 -> {
                    Toast.makeText(
                        this,
                        "학교 또는 비밀번호가 올바르지 않습니다. 다시 시도해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    showServerError(this)
                }
            }
        }
    }

    private val schoolCollector = FlowCollector<State<List<School>>> { state ->
        if (state is State.Error || (state is State.Success && state.data == null)) {
            // Todo: Handle Error
            return@FlowCollector

        }
        if (state is State.Success) {
            val list = state.data!!
            val schoolNames: MutableList<String> = listOf<String>().toMutableList()
            list.forEach { school ->
                schoolNames.add(school.name)
            }
            schools = list
            val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                schoolNames
            )
            view.schoolSpinner.adapter = spinnerAdapter
            view.schoolSpinner.onItemSelectedListener = this@LoginActivity
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

        lifecycleScope.launch {
            viewModel.versionState.collect(versionCollector)
        }
        lifecycleScope.launch {
            viewModel.loginState.collect(loginResultCollector)
        }
        lifecycleScope.launch {
            viewModel.schoolsState.collect(schoolCollector)
        }
        checkAuthValid()

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
                viewModel.fetchVersionInfo()
            }
        }
    }

    fun checkAuthValid() {
        lifecycleScope.launch {
            val token = prefsRepository.getSchoolToken().first()
            if (token.isNullOrEmpty()) {
                Toast.makeText(
                    this@LoginActivity,
                    "학교 로그인 정보를 불러올 수 없습니다. 로그인을 재시도해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                loaded = true
                viewModel.fetchSchools()
                view.submit.setOnClickListener {
                    viewModel.login(selectedSchool?.id!!, view.password.text.toString())
                }
                return@launch
            }

            App.token = token
            val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedSchool = schools[position]
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onResume() {
        super.onResume()
        val completeFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(mCompleteReceiver, completeFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mCompleteReceiver)
    }
}