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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.databinding.ActivityMainBinding
import com.h4pay.store.model.*
import com.h4pay.store.networking.H4PayService
import com.h4pay.store.networking.tools.networkInterceptor
import com.h4pay.store.networking.tools.permissionManager
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.isOnScreenKeyboardEnabled
import com.h4pay.store.util.itemJsonToArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var viewAdapter: itemsRecycler
    private lateinit var view: ActivityMainBinding
    private lateinit var h4payService: H4PayService

    val ISODateFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
    )
    val KoreanDateFormat = SimpleDateFormat(
        "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
    )


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
        view.usable.isVisible = isOpened
        view.openWarning.isVisible = !isOpened
        view.openStatusText.text = if (isOpened) "OPEN" else "CLOSED"
    }

    private fun setExchangeButtonListener(orderId: String) {
        //------Cancel and Exchange Button OnClick Event----------
        val context = this@MainActivity
        view.exchangeButton.setOnClickListener {
            customDialogs.yesNoDialog(context, "확인", "정말로 교환처리 하시겠습니까?", {
                val requestBody = JsonObject()
                val orderIdArray = JsonArray()
                orderIdArray.add(orderId)
                requestBody.add("orderId", orderIdArray)
                if (isGift(orderId) == true) { //선물인 경우
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            h4payService.exchangeGift(requestBody)
                        }.onSuccess {
                            exchangeSuccess(true)
                        }.onFailure {
                            Log.e(TAG, it.message!!)
                            showServerError(this@MainActivity)
                            return@launch
                        }
                    }
                } else { //선물이 아닌 경우
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            h4payService.exchangeOrder(requestBody)
                        }.onSuccess {
                            exchangeSuccess(true)
                        }.onFailure {
                            Log.e(TAG, it.message!!)
                            showServerError(this@MainActivity)
                            return@launch
                        }
                    }
                }
            }, {})
        }
    }

    fun UiInit() {
        view.cameraScan.setOnClickListener {
            initScan()
        }
        view.clearId.setOnClickListener {
            view.orderIdInput.setText("")
        }
        view.cameraScanCircle.setOnClickListener {
            initScan()
        }
        view.callDeveloper.setOnClickListener {
            val intent = Intent(this, CallDeveloper::class.java)
            startActivity(intent)
        }
        view.showInfo.setOnClickListener {
            val intent = Intent(this, H4PayInfo::class.java)
            startActivity(intent)
        }

        view.switchToVoucher.setOnClickListener {
            val intent = Intent(this, VoucherActivity::class.java)
            startActivity(intent)
        }

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

        view.goToDashboard.setOnClickListener {
            val intent = Intent()
            intent.data = Uri.parse("https://manager.h4pay.co.kr")
            startActivity(intent)
        }

        view.exchangeButton.isVisible = false
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.orderIdInput.windowToken, 0)
        view.orderIdInput.setOnFocusChangeListener { _, hasFocus ->
            lifecycleScope.launch {
                delay(keyboardDetectDelay)
                if (hasFocus && !isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                    openImm()
                }
            }
        }
        view.orderIdInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                // 입력이 끝났을 때
                val inputtedOrderId = editable.toString()
                view.orderIdInput.requestFocus();
                if (inputtedOrderId.length < 0 || inputtedOrderId.length > 25) {
                    Toast.makeText(this@MainActivity, "올바른 주문번호가 아닙니다!", Toast.LENGTH_SHORT).show()
                } else if (inputtedOrderId.length == 25) {
                    if (inputtedOrderId.startsWith("3")) { // Voucher
                        val voucherIntent = Intent(this@MainActivity, VoucherActivity::class.java);
                        voucherIntent.putExtra("voucherId", inputtedOrderId)
                        startActivity(voucherIntent)
                        finish()
                        return
                    }
                    //Handling Numbers
                    val f = NumberFormat.getInstance()
                    f.isGroupingUsed = false

                    when {
                        isGift(inputtedOrderId) == false -> { // general order
                            lifecycleScope.launch {
                                kotlin.runCatching {
                                    h4payService.getOrderDetail(inputtedOrderId)
                                }.onSuccess {
                                    if (it.size == 1)
                                        loadOrderDetail(it[0])
                                }.onFailure {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "주문 내역을 불러올 수 없습니다!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                            //API CALL
                        }
                        isGift(inputtedOrderId) == true -> { // gift
                            lifecycleScope.launch {
                                kotlin.runCatching {
                                    h4payService.getGiftDetail(inputtedOrderId)
                                }.onSuccess {
                                    if (it.size == 1)
                                        loadOrderDetail(it[0])
                                }.onFailure {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "주문 내역을 불러올 수 없습니다!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                        }
                        else -> {
                            Toast.makeText(
                                this@MainActivity,
                                "올바른 주문번호가 아닙니다! 1 혹은 2로 시작해야 합니다!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        })
    }

    fun isGift(input: String): Boolean? {
        return if (input.startsWith("1") || input.startsWith("2")) input.startsWith("2") else null
    }

    private fun setButton(exchanged: Boolean) {
        view.exchangeButton.isVisible = !exchanged
        view.exchangeButton.isEnabled = !exchanged
    }

    private fun fetchProduct() {
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getProducts()
            }.onSuccess {
                prodList = it
            }.onFailure {
                Log.e(TAG, it.message!!)
                showServerError(this@MainActivity)
                return@launch
            }
        }
    }

    fun loadOrderDetail(
        purchase: Purchase
    ) {
        view.orderUid.text = purchase.uid ?: view.orderUid.text
        view.orderDate.text = KoreanDateFormat.format(purchase.date) ?: view.orderDate.text
        view.orderExpire.text =
            KoreanDateFormat.format(purchase.expire) ?: view.orderExpire.text
        view.orderAmount.text = "${moneyFormat.format(purchase.amount)} 원"
        if (purchase.exchanged) {
            view.orderExchanged.text = "교환 됨"
            view.orderExchanged.setTextColor(Color.WHITE)
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_red)
            view.orderIdInput.requestFocus()
            setButton(purchase.exchanged)
        } else if (!purchase.exchanged) {
            view.orderExchanged.text = "교환 안됨"
            view.orderExchanged.setTextColor(Color.BLACK)
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_green)
            view.orderIdInput.requestFocus()
            setButton(purchase.exchanged)
        }
        var itemObject = purchase.item // stash item array
        val itemArray = itemJsonToArray(itemObject)
        viewAdapter =
            itemsRecycler(false, this@MainActivity, itemArray) // use item array
        recyclerViewInit()
        setExchangeButtonListener(purchase.orderId)
        view.orderIdInput.setText("")
    }

    private fun recyclerViewInit() {
        val lm = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        view.itemsRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = lm
            adapter = viewAdapter
        }

        view.itemsRecyclerView.isVisible = true

        view.itemsRecyclerView.post {
            view.orderIdInput.isFocusableInTouchMode = true;
            view.orderIdInput.requestFocus()
        } //RecyclerView focus release

        Thread(Runnable {
            Thread.sleep(1000)
            runOnUiThread {
                view.orderIdInput.requestFocus()
            }
        }).start() //view.orderIdInputText focus in
    }

    private fun exchangeSuccess(status: Boolean) {
        if (status) {
            Toast.makeText(this, "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
            makeEmpty()
        } else {
            Toast.makeText(this, "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initScan() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.initiateScan()
    }


    private fun makeEmpty() {
        view.orderExchanged.text = ""
        view.orderAmount.text = ""
        view.orderExpire.text = ""
        view.orderDate.text = ""
        view.orderUid.text = ""
        view.orderExchanged.setBackgroundColor(Color.TRANSPARENT)

        view.exchangeButton.isVisible = false
        try {
            view.itemsRecyclerView.isVisible = false
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            view.orderIdInput.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun openImm() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
        Toast.makeText(
            this,
            "\"스크린 키보드\" 옵션이 켜져있어 가상 키보드가 올라옵니다. 해당 옵션을 꺼주세요.",
            Toast.LENGTH_LONG
        ).show()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, token, Toast.LENGTH_SHORT).show()

        view = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        initService()
        fetchProduct()
        UiInit()
        fetchStoreStatus()

        val passedOrderId = intent.getStringExtra("orderId") ?: return
        if (passedOrderId.length != 25) return
        if (isGift(passedOrderId) == true) {
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.getGiftDetail(passedOrderId)
                }.onSuccess {
                    if (it.size == 1)
                        loadOrderDetail(it[0])
                }.onFailure {
                    Toast.makeText(this@MainActivity, "주문 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.getOrderDetail(passedOrderId)
                }.onSuccess {
                    if (it.size == 1)
                        loadOrderDetail(it[0])
                }.onFailure {
                    Toast.makeText(this@MainActivity, "주문 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }
}