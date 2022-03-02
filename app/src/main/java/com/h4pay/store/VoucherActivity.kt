package com.h4pay.store

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.databinding.ActivityVoucherBinding
import com.h4pay.store.model.Product
import com.h4pay.store.model.Voucher
import com.h4pay.store.networking.H4PayService
import com.h4pay.store.networking.tools.networkInterceptor
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.isOnScreenKeyboardEnabled
import com.h4pay.store.util.itemArrayToJson
import com.h4pay.store.util.itemJsonToArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


val ISODateFormat = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
)
val KoreanDateFormat = SimpleDateFormat(
    "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
)
val moneyFormat: NumberFormat = DecimalFormat("#,###")


class VoucherActivity : AppCompatActivity() {
    private lateinit var view: ActivityVoucherBinding
    private lateinit var recyclerAdapter: itemsRecycler
    private lateinit var voucherId: String
    private lateinit var h4payService: H4PayService
    private var item = JsonObject()


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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            view.idInput.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    fun loadVoucherDetail(
        voucher: Voucher
    ) {
        view.orderUid.text = voucher.receiver.tel
        view.orderDate.text = KoreanDateFormat.format(voucher.date)
        view.orderExpire.text = KoreanDateFormat.format(voucher.expire)
        view.orderExchanged.text = if (voucher.exchanged) "교환 됨" else "교환 안됨"
        val backgroundDrawable =
            if (voucher.exchanged) R.drawable.rounded_red else R.drawable.rounded_green
        view.orderExchanged.background =
            ContextCompat.getDrawable(this@VoucherActivity, backgroundDrawable)

        view.voucherAmount.text = "상품권 금액: ${moneyFormat.format(voucher.amount)} 원"
        if (!voucher.exchanged) {
            view.productArea.visibility = View.VISIBLE
        }
        view.exchangeButton.setOnClickListener {
            exchange(voucher)
        }
        view.openStatus.setOnCheckedChangeListener { _, isOpened ->
            lifecycleScope.launch {
                kotlin.runCatching {
                    val requestBody = JsonObject()
                    requestBody.addProperty("isOpened", isOpened)
                    h4payService.changeStoreStatus(requestBody)
                }.onSuccess {
                    Log.e("VoucherActivity", it.toString())
                    setStoreStatus(it)
                }.onFailure {
                    showServerError(this@VoucherActivity)
                }
            }
        }
    }

    private fun fetchStoreStatus() {
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getStoreStatus()
            }.onSuccess {
                setStoreStatus(it)
            }.onFailure {
                Log.e("Voucher", it.toString())
                showServerError(this@VoucherActivity)
                return@launch
            }
        }
    }

    private fun setStoreStatus(isOpened: Boolean) {
        view.openStatus.isChecked = isOpened
        view.usable.isGone = !isOpened // 열려있지 않으면 사라지게
        //view.openWarning.isGone = isOpened
        view.openStatusText.text = if (isOpened) "OPEN" else "CLOSED"
    }

    private fun initScan() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.initiateScan()
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

    private fun initUI() {
        view.switchToPurchase.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        view.cameraScan.setOnClickListener {
            initScan()
        }
        view.cameraScanCircle.setOnClickListener {
            initScan()
        }
        view.clearId.setOnClickListener {
            view.idInput.setText("")
        }
        view.clearText.setOnClickListener {
            view.productBarcode.setText("")
        }
        view.idInput.setOnFocusChangeListener { _, hasFocus ->
            lifecycleScope.launch {
                delay(keyboardDetectDelay)
                if (hasFocus && !isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                    openImm()
                }
            }
        }
        view.productBarcode.setOnFocusChangeListener { _, hasFocus ->
            lifecycleScope.launch {
                delay(keyboardDetectDelay)
                if (hasFocus && !isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                    openImm()
                }
            }
        }
        view.openStatus.setOnCheckedChangeListener { _, isChecked: Boolean ->
            val body = JsonObject()
            body.addProperty("isOpened", isChecked)
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.changeStoreStatus(body)
                }.onSuccess {
                    setStoreStatus(it)
                }.onFailure {
                    Toast.makeText(
                        this@VoucherActivity,
                        "매점 개폐점 상태를 설정하지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        view = DataBindingUtil.setContentView(this, R.layout.activity_voucher)
        initUI()
        initService()

        fetchStoreStatus()
        val passedId = intent.getStringExtra("voucherId")
        if (passedId != null) {
            voucherId = passedId
            view.idInput.setText(passedId)
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.getVoucherDetail(voucherId)
                }.onSuccess {
                    if (it.isEmpty()) {
                        Toast.makeText(
                            this@VoucherActivity,
                            "상품권 정보를 불러올 수 없어요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@onSuccess
                    } else {
                        loadVoucherDetail(it[0])
                    }
                }
            }
        }

        view.idInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
                Log.d("TAG", barcode.startsWith("3").toString())
                if (barcode.length == 25) {
                    if (!barcode.startsWith("3")) {
                        val mainIntent = Intent(this@VoucherActivity, MainActivity::class.java);
                        mainIntent.putExtra("orderId", barcode)
                        startActivity(mainIntent)
                        finish()
                        return
                    }
                    Log.d("BARCODE", barcode)
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            h4payService.getVoucherDetail(barcode)
                        }.onSuccess {
                            if (it.isEmpty()) {
                                Toast.makeText(
                                    this@VoucherActivity,
                                    "상품권 정보를 불러올 수 없어요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@onSuccess
                            } else {
                                loadVoucherDetail(it[0])
                                voucherId = barcode
                            }
                        }
                    }

                    p0!!.clear()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        view.productBarcode.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
                if (barcode.length == 13) {
                    val product = findProductByBarcode(barcode)
                    if (product == null) { // 바코드로 상품을 찾아 없으면
                        Toast.makeText(
                            this@VoucherActivity,
                            "바코드로 제품 정보를 찾을 수 없어요. 정확히 스캔했는지 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        p0!!.clear()
                        return // 오류 메시지 표시 후 리턴
                    }
                    view.productArea.visibility = View.VISIBLE

                    addProductToItem(product.id)
                    if (!view.itemsRecyclerView.isActivated) { // RecyclerView가 활성화 되지 않았으면
                        recyclerAdapter = itemsRecycler(
                            true,
                            this@VoucherActivity,
                            itemJsonToArray(item)
                        ) // 리사이클러 어댑터를 생성하고
                        initRecyclerView() // init한다.
                    } else { // 활성화 되어 있으면
                        recyclerAdapter.changeItems(itemJsonToArray(item)) // 어댑터의 아이템을 모두 변경해준다.
                    }
                    p0!!.clear()
                    val totalAmount = calcTotalAmount()

                    view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(totalAmount)} 원"
                    Thread {
                        Thread.sleep(100)
                        runOnUiThread {
                            view.productBarcode.requestFocus()
                        }
                    }.start() //editText focus in
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun initRecyclerView() {
        val lm = LinearLayoutManager(this@VoucherActivity, LinearLayoutManager.HORIZONTAL, false)
        val recyclerView = view.itemsRecyclerView

        recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            // use a linear layout manager
            layoutManager = lm
            // specify an viewAdapter (see also next example)
            adapter = recyclerAdapter
            isVisible = true
            post {
                view.productBarcode.isFocusableInTouchMode = true;
                view.productBarcode.requestFocus()
            }
        }

    }

    fun findProductByBarcode(barcode: String): Product? {
        for (i in prodList.indices) {
            try {
                if (prodList[i].barcode == barcode) {
                    return prodList[i]
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    fun addProductToItem(productId: Int) {
        if (item.has(productId.toString())) { // 해당 제품이 존재하면
            val qty: Int = item[productId.toString()].asInt // 해당 제품의 수를 가져와
            item.addProperty(productId.toString(), qty + 1) // 1을 더한 것을 저장한다
        } else {
            item.addProperty(productId.toString(), 1) // 그 외의 경우에는 해당 제품의 개수를 1로 지정한다
        }
        return
    }

    fun calcTotalAmount(): Int {
        var totalAmount: Int = 0;
        item.keySet().forEach { // 모든 추가된 품목들에 대해
            for (j in prodList.indices) {
                if (prodList[j].id == Integer.parseInt(it)) { // 제품 배열에서 id가 일치하는 것을 찾아
                    totalAmount += item[it].asInt * prodList[j].price // 제품의 가격과 해당 제품이 담긴 갯수를 곱해 totalAmount에 더한다
                }
            }
        }
        return totalAmount
    }

    fun exchange(voucher: Voucher) {
        val totalAmount = calcTotalAmount()

        if (totalAmount > voucher.amount) { // 선택한 제품 금액 총합보다 액면가가 작으면
            // 교환이 불가하다는 메시지를 띄운다.
            Toast.makeText(
                this@VoucherActivity,
                "액면가보다 선택한 제품의 총 금액이 더 많습니다. 제거해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (totalAmount < voucher.amount * 0.6) { // 선택한 제품 금액 총합이 액면가의 60%보다 작으면
            // 교환이 불가하다는 메시지를 띄운다.
            Toast.makeText(this@VoucherActivity, "액면가의 60% 이상을 사용해야 합니다.", Toast.LENGTH_SHORT)
                .show()
            return
        } else {
            val requestBody = JsonObject()
            requestBody.addProperty("id", voucher.id)
            requestBody.add("item", item)
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.exchangeVoucher(requestBody)
                }.onSuccess {
                    Toast.makeText(this@VoucherActivity, "교환 처리에 성공했습니다.", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@VoucherActivity, VoucherActivity::class.java)
                    startActivity(intent)
                    finish()
                }.onFailure {
                    Toast.makeText(this@VoucherActivity, "교환 처리에 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                    return@onFailure
                }
            }
        }
    }

    fun onRecyclerDataChanged() {
        item = itemArrayToJson(recyclerAdapter.getItems())
        Log.d("RECYCLER", item.toString())
        view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(calcTotalAmount())} 원"
    }
}