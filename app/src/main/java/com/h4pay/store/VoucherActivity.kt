package com.h4pay.store

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.databinding.ActivityVoucherBinding
import com.h4pay.store.networking.Get
import com.h4pay.store.networking.Post
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.itemArrayToJson
import com.h4pay.store.util.itemJsonToArray
import org.json.JSONArray
import org.json.JSONObject
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
    private lateinit var view:ActivityVoucherBinding
    private lateinit var recyclerAdapter: itemsRecycler
    private lateinit var voucherId:String
    private var item = JSONObject()
    private var voucher = JSONObject()

    fun loadVoucherDetail(
        uid: String?,
        date: String?,
        expire: String?,
        amount: Int?,
        exchanged: Boolean?
    ) {
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val prettifiedDate = ISODateFormat.parse(date)
        val prettifiedExpire = ISODateFormat.parse(expire)

        view.orderUid.text = uid
        view.orderDate.text = KoreanDateFormat.format(prettifiedDate)
        view.orderExpire.text = KoreanDateFormat.format(prettifiedExpire)

        if (exchanged == true) {
            view.orderExchanged.text = "교환 됨"
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@VoucherActivity, R.drawable.rounded_red)
        } else if (exchanged == false) {
            view.orderExchanged.text = "교환 안됨"
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@VoucherActivity, R.drawable.rounded_green)
        }
        view.voucherAmount.text = "상품권 금액: ${moneyFormat.format(amount)} 원"

        view.exchangeButton.setOnClickListener {
            exchange()
        }
    }

    fun fetchStoreStatus() {
        val status: JSONObject? = Get("${BuildConfig.API_URL}/store").execute().get()
        if (status == null) {
            showServerError(this)
            return
        } else {
            view.openStatus.isChecked = status.getBoolean("isOpened")
            when (status.getBoolean("isOpened")) {
                true -> {
                    view.openStatusText.text = "OPEN"
                    view.usable.isVisible = true
                    view.openWarning.isVisible = false
                }
                false -> {
                    view.openStatusText.text = "CLOSED"
                    view.usable.isVisible = false
                    view.openWarning.isVisible = true
                }
            }
        }
        view.openStatus.setOnCheckedChangeListener {
            val status = JSONObject()
            status.put("isOpened", it)
            val result: JSONObject? =
                Post("${BuildConfig.API_URL}/store/change", status).execute().get()
            if (result == null) {
                showServerError(this)
                return@setOnCheckedChangeListener
            } else {
                if (!result.getBoolean("status")) {
                    Toast.makeText(this, "상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "상태가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    when (it) {
                        true -> {
                            view.openStatusText.text = "OPEN"
                            view.usable.isVisible = true
                            view.openWarning.isVisible = false
                        }
                        false -> {
                            view.openStatusText.text = "CLOSED"
                            view.usable.isVisible = false
                            view.openWarning.isVisible = true
                        }
                    }
                }
            }
        }
    }

    fun initScan() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.initiateScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = DataBindingUtil.setContentView(this, R.layout.activity_voucher)
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

        fetchStoreStatus()
        val passedId = intent.getStringExtra("voucherId")

        if (passedId != null) {
            voucherId = passedId
            view.idInput.setText(passedId)
            val voucherResult = Get("${BuildConfig.API_URL}/voucher/filter?id=${voucherId}").execute().get()!!
            voucher = if (voucherResult.getBoolean("status") ) voucherResult.getJSONArray("result").getJSONObject(0) else JSONObject()
            if (voucher == null) {
                Toast.makeText(this@VoucherActivity, "상품권 정보를 불러올 수 없어요.", Toast.LENGTH_SHORT).show()
                return
            }
            loadVoucherDetail(
                voucher.getJSONObject("issuer").getString("uid"),
                voucher.getString("date"),
                voucher.getString("expire"),
                voucher.getInt("amount"),
                voucher.getBoolean("exchanged")
            )
            view.productBarcode.requestFocus()
            view.exchangeButton.setOnClickListener {
                exchange()
            }
        }

        view.idInput.addTextChangedListener(object: TextWatcher {
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
                        val voucherResult = Get("${BuildConfig.API_URL}/voucher/filter?id=${barcode}").execute().get()!!
                        voucher = if (voucherResult.getBoolean("status") ) voucherResult.getJSONArray("result").getJSONObject(0) else JSONObject()
                        if (voucher == null) {
                            Toast.makeText(this@VoucherActivity, "상품권 정보를 불러올 수 없어요.", Toast.LENGTH_SHORT).show()
                            return
                        }
                        voucherId = barcode
                        loadVoucherDetail(
                            voucher.getJSONObject("issuer").getString("uid"),
                            voucher.getString("date"),
                            voucher.getString("expire"),
                            voucher.getInt("amount"),
                            voucher.getBoolean("exchanged")
                        )

                        p0!!.clear()
                        view.productBarcode.requestFocus()
                    }
                }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        view.productBarcode.addTextChangedListener(object: TextWatcher {
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
                        return // 오류 메시지 표시 후 리턴
                    }

                    addProductToItem(product.getInt("id"))
                    if (!view.itemsRecyclerView.isActivated) { // RecyclerView가 활성화 되지 않았으면
                        recyclerAdapter = itemsRecycler(true, this@VoucherActivity, itemJsonToArray(item)) // 리사이클러 어댑터를 생성하고
                        initRecyclerView() // init한다.
                    } else { // 활성화 되어 있으면
                        recyclerAdapter.changeItems(itemJsonToArray(item)) // 어댑터의 아이템을 모두 변경해준다.
                    }
                    p0!!.clear()
                    val totalAmount = calcTotalAmount()

                    view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(totalAmount)} 원"
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
        Thread {
            Thread.sleep(1000)
            runOnUiThread {
                view.productBarcode.requestFocus()
            }
        }.start() //editText focus in
    }
    fun findProductByBarcode(barcode: String) : JSONObject? {
       for (i in 0 until prodList.length()) {
           try {
               if (prodList.getJSONObject(i).getString("barcode") == barcode) {
                   return prodList.getJSONObject(i)
               }
           } catch (e: Exception) {

           }

       }
        return null
    }
    fun addProductToItem(productId:Int) {
        if (item.has(productId.toString())) { // 해당 제품이 존재하면
            val qty:Int = item[productId.toString()] as Int // 해당 제품의 수를 가져와
            item.put(productId.toString(), qty + 1) // 1을 더한 것을 저장한다
        } else {
            item.put(productId.toString(), 1) // 그 외의 경우에는 해당 제품의 개수를 1로 지정한다
        }
        return
    }
    fun calcTotalAmount() : Int {
        var totalAmount:Int = 0;
            item.keys().forEach { // 모든 추가된 품목들에 대해
                for (j in 0 until prodList.length()) {
                    if (prodList.getJSONObject(j).getInt("id") == Integer.parseInt(it)) { // 제품 배열에서 id가 일치하는 것을 찾아
                        totalAmount += item.getInt(it) * prodList.getJSONObject(j).getInt("price") // 제품의 가격과 해당 제품이 담긴 갯수를 곱해 totalAmount에 더한다
                    }
                }
            }
        return totalAmount
    }
    fun exchange() {
        val totalAmount = calcTotalAmount()

        if (totalAmount > voucher.getInt("amount")) { // 선택한 제품 금액 총합보다 액면가가 작으면
            // 교환이 불가하다는 메시지를 띄운다.
            Toast.makeText(this@VoucherActivity, "액면가보다 선택한 제품의 총 금액이 더 많습니다. 제거해주세요.", Toast.LENGTH_SHORT).show()
            return
        } else if (totalAmount < voucher.getInt("amount") * 0.6){ // 선택한 제품 금액 총합이 액면가의 60%보다 작으면
            // 교환이 불가하다는 메시지를 띄운다.
            Toast.makeText(this@VoucherActivity, "액면가의 60% 이상을 사용해야 합니다.", Toast.LENGTH_SHORT).show()
            return
        } else {
            val data = JSONObject()
            data.put("id", voucherId);
            data.put("item", item)
            val exchangeResult = Post("${BuildConfig.API_URL}/voucher/exchange", data).execute().get()
            val status = exchangeResult.getBoolean("status")
            if (status) {
                Toast.makeText(this, "교환 처리에 성공했습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, VoucherActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "교환 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onRecyclerDelButtonClicked() {
        item = itemArrayToJson(recyclerAdapter.getItems())
        Log.d("RECYCLER", item.toString())
        view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(calcTotalAmount())} 원"
    }
}