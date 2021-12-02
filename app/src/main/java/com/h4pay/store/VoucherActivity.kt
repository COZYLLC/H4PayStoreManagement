package com.h4pay.store

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h4pay.store.databinding.ActivityVoucherBinding
import com.h4pay.store.networking.Get
import com.h4pay.store.networking.Post
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.itemJsonToArray
import org.json.JSONArray
import org.json.JSONObject
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

class VoucherActivity : AppCompatActivity() {
    private lateinit var view:ActivityVoucherBinding
    private lateinit var recyclerAdapter: itemsRecycler
    private lateinit var products: JSONArray
    private lateinit var voucherId:String
    private var item = JSONObject()

    fun loadVoucherDetail(
        uid: String?,
        date: String?,
        expire: String?,
        amount: String?,
        exchanged: Boolean?
    ) {
        val moneyFormat: NumberFormat = DecimalFormat("#,###")
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val prettifiedDate = ISODateFormat.parse(date)
        val prettifiedExpire = ISODateFormat.parse(expire)

        view.orderUid.text = uid
        view.orderDate.text = KoreanDateFormat.format(prettifiedDate)
        view.orderExpire.text = KoreanDateFormat.format(prettifiedExpire)
        view.orderAmount.text = moneyFormat.format(amount) + " 원"

        if (exchanged == true) {
            view.orderExchanged.text = "교환 됨"
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@VoucherActivity, R.drawable.rounded_red)
        } else if (exchanged == false) {
            view.orderExchanged.text = "교환 안됨"
            view.orderExchanged.background =
                ContextCompat.getDrawable(this@VoucherActivity, R.drawable.rounded_green)
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = DataBindingUtil.setContentView(this, R.layout.activity_voucher)
        fetchStoreStatus()

        view.exchangeButton.setOnClickListener {
            val data = JSONObject()
            data.put("id", voucherId)
            data.put("item", item)
            val exchangeResult = Post("${BuildConfig.API_URL}/voucher/exchange", data).execute().get()!!
            if (exchangeResult.getBoolean("status")) {
                Toast.makeText(this, "정상적으로 처리되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        view.idInput.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
                val voucher = Get("${BuildConfig.API_URL}/voucher/filter?id=${barcode}").execute().get()!!
                voucherId = barcode
                loadVoucherDetail(
                    voucher.getJSONObject("issuer").getString("uid"),
                    voucher.getString("date"),
                    voucher.getString("expire"),
                    voucher.getInt("amount").toString(),
                    voucher.getBoolean("exchanged")
                )
                view.productBarcode.requestFocus()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        view.productBarcode.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
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
                    recyclerAdapter = itemsRecycler(this@VoucherActivity, itemJsonToArray(item)) // 리사이클러 어댑터를 생성하고
                    initRecyclerView() // init한다.
                } else { // 활성화 되어 있으면
                    recyclerAdapter.changeItems(itemJsonToArray(item)) // 어댑터의 아이템을 모두 변경해준다.
                }
                p0!!.clear()
                view.totalAmount.text = "현재 사용 금액: ${calcTotalAmount()}"
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }
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
       for (i in 0..products.length()) {
           if (products.getJSONObject(i).getString("barcode") == barcode) {
               return products.getJSONObject(i)
           }
       }
        return null
    }
    fun addProductToItem(productId:Int) {
        if (item.has(productId.toString())) { // 해당 제품이 존재하면
            val qty:Int = Integer.parseInt(item[productId.toString()] as String) // 해당 제품의 수를 가져와
            item.put(productId.toString(), qty + 1) // 1을 더한 것을 저장한다
        } else {
            item.put(productId.toString(), 1) // 그 외의 경우에는 해당 제품의 개수를 1로 지정한다
        }
        return
    }
    fun calcTotalAmount() : Int {
        var totalAmount:Int = 0;
            item.keys().forEach { // 모든 추가된 품목들에 대해
                for (j in 0..products.length()) {
                    if (products.getJSONObject(j).getInt("id") == Integer.parseInt(it)) { // 제품 배열에서 id가 일치하는 것을 찾아
                        totalAmount += item.getInt(it) * products.getJSONObject(j).getInt("price") // 제품의 가격과 해당 제품이 담긴 갯수를 곱해 totalAmount에 더한다
                    }
                }
            }
        return totalAmount
    }
}