package com.h4pay.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.networking.*
import com.h4pay.store.networking.tools.JSONTools
import com.h4pay.store.recyclerAdapter.itemsRecycler
import org.apache.poi.sl.usermodel.Line
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class exchangeActivity : AppCompatActivity() {

    private val TAG = "exchangeActivity"
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var recyclerView:RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var edit:EditText
    private lateinit var uid:TextView
    private lateinit var orderid:TextView
    private lateinit var date:TextView
    private lateinit var expire:TextView
    private lateinit var amount :TextView
    private lateinit var exchanged :TextView
    private lateinit var exchangeButton:LinearLayout
    private lateinit var cameraScan:LinearLayout
    
    fun makeEmpty(){
        uid.text = ""
        orderid.text = ""
        date.text = ""
        expire.text = ""
        amount.text = ""
        exchanged.text = ""
        exchanged.background = null

        exchangeButton.isVisible = false
        try {
            recyclerView.isVisible = false
        }catch (e:UninitializedPropertyAccessException){
            e.printStackTrace()
        }
    }

    fun UiInit(){
        edit = findViewById(R.id.orderIdInput)
        uid = findViewById(R.id.lookup_uid)
        orderid = findViewById(R.id.lookup_orderid)
        date = findViewById(R.id.lookup_date)
        expire = findViewById(R.id.lookup_expire)
        amount = findViewById(R.id.lookup_amount)
        exchanged = findViewById(R.id. exchanged)
        exchangeButton = findViewById(R.id.exchange_Button)
        cameraScan = findViewById(R.id.cameraScan)
        cameraScan.setOnClickListener {
            initScan()
        }

        exchangeButton.isVisible = false
    }

    fun cancelSuccess(status:Boolean){
        if (status) {
            makeEmpty()
            Toast.makeText(
                this,
                "취소가 정상적으로 완료되었습니다.",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            Toast.makeText(
                this,
                "취소에 실패했습니다.\n없는 주문번호입니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun exchangeSuccess(status: Boolean){
        if(status){
            Toast.makeText(this, "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
            makeEmpty()
        }
        else{
            Toast.makeText(this, "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.", Toast.LENGTH_LONG).show()
        }
    }
    fun initScan() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            edit.setText( result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire(10*60*1000L /*10 minutes*/)
                }
            }
        UiInit()

        viewManager = LinearLayoutManager(this)
        val inputMethodManager:InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(edit.windowToken, 0)
        edit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // 입력되는 텍스트에 변화가 있을 때
            }

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(arg0: Editable) {
                // 입력이 끝났을 때
                edit.requestFocus();
                
                if (edit.text.length < 0 || edit.text.length > 25){
                    Toast.makeText(this@exchangeActivity, "올바른 주문번호가 아닙니다!", Toast.LENGTH_SHORT).show()
                }
                else if (edit.text.length == 25){
                    //Handling Numbers
                    val f = NumberFormat.getInstance()
                    f.isGroupingUsed = false
                    val orderID = edit.getText().toString()
                    var res:JSONObject? = JSONObject()

                    if (orderID.startsWith("1")){ // general order
                        val result = Get("${BuildConfig.API_URL}/orders/fromorderid/$orderID").execute().get()
                        if (result == null ){
                            showServerError(this@exchangeActivity)
                            return
                        } else {
                            if (result.getBoolean("status")){
                                res = result.getJSONObject("order")
                            }else{
                                Toast.makeText(this@exchangeActivity, "불러오지 못했습니다!", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }

                    }else if(orderID.startsWith("2")){ // gift
                        val result = Get("${BuildConfig.API_URL}/gift/findbyorderid/$orderID").execute().get()
                        if (result == null) {
                            showServerError(this@exchangeActivity)
                            return
                        } else {
                            if (result.getBoolean("status")){
                                res = result.getJSONObject("gift")
                            }
                            else{
                                Toast.makeText(this@exchangeActivity, "불러오지 못했습니다!", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                    }else{
                        Toast.makeText(this@exchangeActivity, "올바른 주문번호가 아닙니다! 1 혹은 2로 시작해야 합니다!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var items: JSONArray? = res.getJSONArray("item")
                    if (items != null){
                        items = JSONTools.deleteUnusedCartItems(items)
                        viewAdapter = itemsRecycler(this@exchangeActivity, items)
                    }
                    if (items == null) {
                        return
                    }

                    //recyclerView Init (Cart Items)
                    val lm = LinearLayoutManager(this@exchangeActivity, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView = findViewById<RecyclerView>(R.id.itemsRecyclerView)

                    recyclerView.apply {
                        // use this setting to improve performance if you know that changes
                        // in content do not change the layout size of the RecyclerView
                        setHasFixedSize(true)

                        // use a linear layout manager
                        layoutManager = lm


                        // specify an viewAdapter (see also next example)
                        adapter = viewAdapter

                    }

                    recyclerView.isVisible = true
                    //----------RecyclerView END-----------

                    if (res != null){

                        //UI Update

                        edit.setText("")
                        if (orderID.startsWith("2")){
                            uid.text = "발송자 ID ${res.getString("uidfrom")}\n받는이 ID ${res.getString("uidto")}"
                        }
                        else{
                            uid.text = "사용자 ID " + res.getString("uid")
                        }
                        orderid.text = "주문번호 " +  (res.getString("orderid"))
                        val format = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
                        )
                        val format2 = SimpleDateFormat(
                                "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
                        )
                        val moneyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());


                        format.timeZone = TimeZone.getTimeZone("UTC")
                        format2.timeZone = TimeZone.getTimeZone("UTC")
                        val pretifiedDate = format.parse(res.getString("date"))
                        val pretifiedExpire = format.parse(res.getString("expire"))
                        date.text = "주문 일시 ${format2.format(pretifiedDate)}"
                        expire.text = "사용 기한 ${format2.format(pretifiedExpire)}"
                        if (orderID.startsWith("1")) {
                            amount.text = "결제 금액 ${moneyFormat.format(res.getInt("amount"))}"
                        }else if(orderID.startsWith("2")){
                            amount.visibility = View.GONE
                        }

                        recyclerView.post{
                            edit.isFocusableInTouchMode = true;
                            edit.requestFocus()
                        } //RecyclerView focus release

                        Thread(Runnable{
                            Thread.sleep(1000)
                            runOnUiThread {
                                edit.requestFocus()
                            }
                        }).start() //editText focus in

                        if (res.getBoolean("exchanged")){
                            exchanged.text = "교환됨"
                            exchanged.background = ContextCompat.getDrawable(this@exchangeActivity, R.drawable.rounded_red)
                            exchangeButton.isVisible = false
                            exchangeButton.isEnabled = false
                            edit.requestFocus()
                        }
                        else{
                            exchanged.text = "교환 안 됨"
                            exchanged.background = ContextCompat.getDrawable(this@exchangeActivity, R.drawable.rounded_green)
                            exchangeButton.isEnabled = true
                            exchangeButton.isVisible = true
                            edit.requestFocus()

                            //------Cancel and Exchange Button OnClick Event----------
                            val orderid = res.getString("orderid")
                            val context = this@exchangeActivity
                            exchangeButton.setOnClickListener {
                                customDialogs.yesNoDialog( context, "확인", "정말로 교환처리 하시겠습니까?", {
                                    val req = JSONObject().accumulate("orderId", orderid)
                                    if (orderid.startsWith("2")){ //선물인 경우
                                        val exchangeRes = Post("${BuildConfig.API_URL}/gift/exchange", req).execute().get()
                                        if (exchangeRes == null) {
                                            showServerError(this@exchangeActivity)
                                            return@yesNoDialog
                                        }else {
                                            exchangeSuccess(exchangeRes.getBoolean("status"))
                                        }
                                    }else{ //선물이 아닌 경우
                                        val exchangeRes = Post("${BuildConfig.API_URL}/orders/exchange", req).execute().get()
                                        if (exchangeRes == null) {
                                            showServerError(this@exchangeActivity)
                                            return@yesNoDialog
                                        }else {
                                            exchangeSuccess(exchangeRes!!.getBoolean("status"))

                                        }
                                    }

                                }, {})

                            }
                        }


                    }
                    else{
                        Toast.makeText(this@exchangeActivity, "주문 내역이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                        edit.setText("")
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

                // 입력하기 전에
            }
        })


    }
}
