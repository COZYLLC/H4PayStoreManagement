package com.h4pay.store

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.databinding.ActivityMainBinding
import com.h4pay.store.fragments.ClosedFragment
import com.h4pay.store.fragments.PurchaseFragment
import com.h4pay.store.model.*
import com.h4pay.store.networking.H4PayService
import com.h4pay.store.networking.initService
import com.h4pay.store.networking.tools.networkInterceptor
import com.h4pay.store.networking.tools.permissionManager
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.currentFragmentType
import com.h4pay.store.util.isOnScreenKeyboardEnabled
import com.h4pay.store.util.itemJsonToArray
import com.h4pay.store.util.swapFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

lateinit var prodList: List<Product>

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "store")

fun showServerError(context: Activity) {
    AlertDialog.Builder(context, R.style.AlertDialogTheme)
        .setTitle("서버 오류")
        .setMessage("서버 오류로 인해 사용할 수 없습니다. 개발자에게 문의 바랍니다.")
        .setPositiveButton("확인") { _: DialogInterface, _: Int ->
            context.finish()
        }.show()
}

enum class FragmentType {
    Purchase,
    Voucher,
    Closed
}

val ISODateFormat = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
)
val KoreanDateFormat = SimpleDateFormat(
    "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
)
val moneyFormat: NumberFormat = DecimalFormat("#,###")

var storeStatus:Boolean = false

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var view: ActivityMainBinding
    private lateinit var h4payService: H4PayService

    private fun fetchStoreStatus() {
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getStoreStatus()
            }.onSuccess {
                Log.e("storestatus", it.toString())
                setStoreStatus(it)
            }.onFailure {
                Log.e(TAG, it.message!!)
                showServerError(this@MainActivity)
            }
        }
    }

    private fun setStoreStatus(isOpened: Boolean) {
        view.openStatus.isChecked = isOpened
        view.openStatusText.text = if (isOpened) "OPEN" else "CLOSED"
        if (!isOpened) swapFragment(this, FragmentType.Closed, Bundle()) // 닫기
        else swapFragment(this, currentFragmentType, Bundle()) // Closed를 제외한 것만 Type만 넣는 "currentFragmentType"으로 Fragment 변경.
    }

    private fun initUI() {
        view.openStatus.setOnCheckedChangeListener { _, isOpened ->
            lifecycleScope.launch {
                kotlin.runCatching {
                    val requestBody = JsonObject()
                    requestBody.addProperty("isOpened", isOpened)
                    h4payService.changeStoreStatus(requestBody)
                }.onSuccess {
                    Log.e(TAG, it.toString())
                    setStoreStatus(it)
                }.onFailure {
                    showServerError(this@MainActivity)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, token, Toast.LENGTH_SHORT).show()

        currentFragmentType = FragmentType.Purchase
        view = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        supportFragmentManager.beginTransaction().replace(R.id.fragment_view, PurchaseFragment()).commit()

        h4payService = initService()
        initUI()
        fetchStoreStatus()
    }

}