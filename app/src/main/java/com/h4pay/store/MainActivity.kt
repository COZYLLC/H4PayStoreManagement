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
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.networking.Get
import com.h4pay.store.networking.Post
import com.h4pay.store.networking.tools.permissionManager
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.itemJsonToArray
import com.jtv7.rippleswitchlib.RippleSwitch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

lateinit var prodList: JSONArray


fun showServerError(context: Activity) {
    AlertDialog.Builder(context, R.style.AlertDialogTheme)
        .setTitle("서버 오류")
        .setMessage("서버 오류로 인해 사용할 수 없습니다. 개발자에게 문의 바랍니다.")
        .setPositiveButton("확인") { dialadogInterface: DialogInterface, i: Int ->
            context.finish()
        }.show()
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var time:TextView

    private lateinit var edit: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>

    private lateinit var orderUidTextView: TextView
    private lateinit var orderDateTextView: TextView
    private lateinit var orderExpireTextView: TextView
    private lateinit var orderAmountTextView: TextView
    private lateinit var orderExchangedTextView: TextView
    private lateinit var exchangeButton: LinearLayout

    private lateinit var cameraScan: LinearLayout
    private lateinit var cameraScanCircle: ImageButton

    private lateinit var callDeveloperButton:LinearLayout
    private lateinit var showInfoButton: LinearLayout

    private lateinit var openStatusSwitch: RippleSwitch
    private lateinit var openStatusText: TextView

    private lateinit var openWarningLayout: ConstraintLayout
    private lateinit var usableLayout: ConstraintLayout

    private lateinit var voucherButton: LinearLayout

    val ISODateFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
    )
    val KoreanDateFormat = SimpleDateFormat(
        "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
    )

    fun checkConnectivity(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val status = networkInfo != null && networkInfo.isConnected
        return status
    }

    fun fetchStoreStatus() {
        val status: JSONObject? = Get("${BuildConfig.API_URL}/store").execute().get()
        if (status == null) {
            showServerError(this)
            return
        } else {
            openStatusSwitch.isChecked = status.getBoolean("isOpened")
            when (status.getBoolean("isOpened")) {
                true -> {
                    openStatusText.text = "OPEN"
                    usableLayout.isVisible = true
                    openWarningLayout.isVisible = false
                }
                false -> {
                    openStatusText.text = "CLOSED"
                    usableLayout.isVisible = false
                    openWarningLayout.isVisible = true
                }
            }
        }
    }

    fun setExchangeButtonListener(orderId:String) {

        //------Cancel and Exchange Button OnClick Event----------
        val context = this@MainActivity
        exchangeButton.setOnClickListener {
            customDialogs.yesNoDialog(context, "확인", "정말로 교환처리 하시겠습니까?", {
                val req = JSONObject().accumulate("orderId", JSONArray().put(0, orderId))
                if (isGift(orderId) == true) { //선물인 경우
                    val exchangeRes =
                        Post("${BuildConfig.API_URL}/gift/exchange", req).execute()
                            .get()
                    if (exchangeRes == null) {
                        showServerError(this@MainActivity)
                        return@yesNoDialog
                    } else {
                        exchangeSuccess(exchangeRes.getBoolean("status"))
                    }
                } else { //선물이 아닌 경우
                    val exchangeRes = Post(
                        "${BuildConfig.API_URL}/orders/exchange",
                        req
                    ).execute().get()
                    if (exchangeRes == null) {
                        showServerError(this@MainActivity)
                        return@yesNoDialog
                    } else {
                        exchangeSuccess(exchangeRes.getBoolean("status"))
                    }
                }
            }, {})
        }
    }

    fun UiInit() {
        time = findViewById(R.id.time)
        edit = findViewById(R.id.orderIdInput)
        orderUidTextView = findViewById(R.id.order_uid)
        orderDateTextView = findViewById(R.id.order_date)
        orderExpireTextView = findViewById(R.id.order_expire)
        orderAmountTextView = findViewById(R.id.order_amount)
        orderExchangedTextView = findViewById(R.id.order_exchanged)
        exchangeButton = findViewById(R.id.exchangeButton)
        cameraScan = findViewById(R.id.cameraScan)
        cameraScanCircle = findViewById(R.id.cameraScanCircle)
        callDeveloperButton = findViewById(R.id.callDeveloper)
        showInfoButton = findViewById(R.id.showInfo)
        openStatusSwitch = findViewById(R.id.openStatus)
        openStatusText = findViewById(R.id.openStatusText)
        openWarningLayout = findViewById(R.id.openWarning)
        usableLayout = findViewById(R.id.usable)
        voucherButton = findViewById(R.id.switchToVoucher)

        cameraScan.setOnClickListener {
            initScan()
        }
        cameraScanCircle.setOnClickListener {
            initScan()
        }
        callDeveloperButton.setOnClickListener {
            val intent = Intent(this, CallDeveloper::class.java)
            startActivity(intent)
        }
        showInfoButton.setOnClickListener {
            val intent = Intent(this, H4PayInfo::class.java)
            startActivity(intent)
        }

        voucherButton.setOnClickListener {
            val intent = Intent(this, VoucherActivity::class.java)
            startActivity(intent)
        }



        openStatusSwitch.setOnCheckedChangeListener {
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
                            openStatusText.text = "OPEN"
                            usableLayout.isVisible = true
                            openWarningLayout.isVisible = false
                        }
                        false -> {
                            openStatusText.text = "CLOSED"
                            usableLayout.isVisible = false
                            openWarningLayout.isVisible = true
                        }
                    }
                }
            }
        }

        exchangeButton.isVisible = false
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(edit.windowToken, 0)
        edit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                // 입력이 끝났을 때
                val inputtedOrderId = editable.toString()
                var uid: String

                edit.requestFocus();
                if (inputtedOrderId.length < 0 || inputtedOrderId.length > 25) {
                    Toast.makeText(this@MainActivity, "올바른 주문번호가 아닙니다!", Toast.LENGTH_SHORT).show()
                } else if (inputtedOrderId.length == 25) {
                    if (inputtedOrderId.startsWith("3")) {
                        val voucherIntent = Intent(this@MainActivity, VoucherActivity::class.java);
                        voucherIntent.putExtra("voucherId", inputtedOrderId)
                        startActivity(voucherIntent)
                        finish()
                        return
                    }
                    //Handling Numbers
                    val f = NumberFormat.getInstance()
                    f.isGroupingUsed = false
                    var res: JSONObject? = JSONObject()

                    if (isGift(inputtedOrderId) == false) { // general order
                        val result =
                            Get("${BuildConfig.API_URL}/orders/fromorderId/$inputtedOrderId").execute()
                                .get()
                        if (result == null) {
                            showServerError(this@MainActivity)
                            return
                        } else {
                            if (result.getBoolean("status")) {
                                res = result.getJSONObject("order")
                                uid = res.getString("uid")
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "불러오지 못했습니다!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                        }
                    } else if (isGift(inputtedOrderId) == true) { // gift
                        val result =
                            Get("${BuildConfig.API_URL}/gift/findbyorderId/$inputtedOrderId").execute()
                                .get()
                        if (result == null) {
                            showServerError(this@MainActivity)
                            return
                        } else {
                            if (result.getBoolean("status")) {
                                res = result.getJSONObject("gift")
                                uid = res.getString("uidto")
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "불러오지 못했습니다!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "올바른 주문번호가 아닙니다! 1 혹은 2로 시작해야 합니다!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    if (res != null) {
                        //UI Update

                        var items: JSONObject? = res.getJSONObject("item") // stash item array
                        if (items != null) {
                            val itemArray = itemJsonToArray(items)
                            Log.d("TAG", itemArray.toString())

                            viewAdapter =
                                itemsRecycler(false, this@MainActivity, itemArray) // use itme array
                        } else {
                            return
                        }

                        recyclerViewInit()
                        editable.clear()
                        val moneyFormat: NumberFormat = DecimalFormat("#,###")
                        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
                        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
                        val prettifiedDate = ISODateFormat.parse(res.getString("date"))
                        val prettifiedExpire = ISODateFormat.parse(res.getString("expire"))
                        loadOrderDetail(
                            uid,
                            KoreanDateFormat.format(prettifiedDate),
                            KoreanDateFormat.format(prettifiedExpire),
                            moneyFormat.format(res.getInt("amount")) + " 원",
                            res.getBoolean("exchanged")
                        )
                        setExchangeButtonListener(inputtedOrderId)

                    } else {
                        Toast.makeText(this@MainActivity, "주문 내역이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                            .show()
                        editable.clear()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        })
        val mHandler = Handler()
        val thread = Thread(Runnable {
            run {
                while (true) {
                    runOnUiThread {
                        val cal = Calendar.getInstance()
                        var min = cal.get(Calendar.MINUTE).toString()
                        var hour = cal.get(Calendar.HOUR).toString()
                        min = (if (min.toInt() < 10) "0" else "") + min
                        hour = (if (hour.toInt() < 10) "0" else "") + hour

                        time.setText("$hour : $min")
                    }
                    Thread.sleep(10000)
                }
            }
        })
        thread.start()
    }

    fun isGift(input: String): Boolean? {
        return if (input.startsWith("1") || input.startsWith("2")) input.startsWith("2") else null
    }

    fun setButton(exchanged: Boolean) {
        exchangeButton.isVisible = !exchanged
        exchangeButton.isEnabled = !exchanged
    }

    fun fetchProduct() {
        val prodListResult = Get("${BuildConfig.API_URL}/product").execute().get()
        if (prodListResult == null) {
            showServerError(this)
            return
        } else {
            prodList = prodListResult.getJSONArray("list")
        }
    }

    fun loadOrderDetail(
        uid: String?,
        date: String?,
        expire: String?,
        amount: String?,
        exchanged: Boolean?
    ) {
        orderUidTextView.text = uid ?: orderUidTextView.text
        orderDateTextView.text = date ?: orderDateTextView.text
        orderExpireTextView.text = expire ?: orderExpireTextView.text
        orderAmountTextView.text = amount ?: orderAmountTextView.text
        if (exchanged == true) {
            orderExchangedTextView.text = "교환 됨"
            orderExchangedTextView.background =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_red)
            edit.requestFocus()
            setButton(exchanged)
        } else if (exchanged == false) {
            orderExchangedTextView.text = "교환 안됨"
            orderExchangedTextView.background =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_green)
            edit.requestFocus()
            setButton(exchanged)
        }
    }

    fun recyclerViewInit() {
        val lm = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
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

        recyclerView.post {
            edit.isFocusableInTouchMode = true;
            edit.requestFocus()
        } //RecyclerView focus release

        Thread(Runnable {
            Thread.sleep(1000)
            runOnUiThread {
                edit.requestFocus()
            }
        }).start() //editText focus in
    }

    fun exchangeSuccess(status: Boolean) {
        if (status) {
            Toast.makeText(this, "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
            makeEmpty()
        } else {
            Toast.makeText(this, "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.", Toast.LENGTH_LONG).show()
        }
    }

    fun initScan() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.initiateScan()
    }


    fun makeEmpty() {
        orderExchangedTextView.text = ""
        orderAmountTextView.text = ""
        orderExpireTextView.text = ""
        orderDateTextView.text = ""
        orderUidTextView.text = ""
        orderExchangedTextView.setBackgroundColor(Color.TRANSPARENT)

        exchangeButton.isVisible = false
        try {
            recyclerView.isVisible = false
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            edit.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_exchangemain)
        if (!checkConnectivity()) {
            Log.d(TAG, "인터넷 연결 X")
            customDialogs.yesOnlyDialog(
                this, "인터넷에 연결되어있지 않습니다. 확인 후 다시 이용 바랍니다.",
                { Process.killProcess(Process.myPid()) }, "", null
            )
        } else {
            Log.d(TAG, "권한 취득 후 업데이트체크")
            if (!permissionManager.hasPermissions(this, permissionList)) {
                ActivityCompat.requestPermissions(this, permissionList, permissionALL)
            } else {
                update()
            }
        }
        fetchProduct()
        UiInit()
        fetchStoreStatus()
        val passedOrderId = intent.getStringExtra("orderId") ?: return
        if (passedOrderId.length != 25) return
        var purchase = JSONObject()
        if (isGift(passedOrderId) == true) {
            val result =
                Get("${BuildConfig.API_URL}/gift/findbyorderId/$passedOrderId").execute()
                    .get()
            if (result == null) {
                showServerError(this@MainActivity)
                return
            } else {
                if (result.getBoolean("status")) {
                    purchase = result.getJSONObject("gift")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "불러오지 못했습니다!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }

            loadOrderDetail(purchase.getString("uidto"), purchase.getString("date"), purchase.getString("expire"), purchase.getString("amount"), purchase.getBoolean("exchanged") )
        } else {
            val result =
                Get("${BuildConfig.API_URL}/orders/fromorderId/$passedOrderId").execute()
                    .get()
            if (result == null) {
                showServerError(this@MainActivity)
                return
            } else {
                if (result.getBoolean("status")) {
                    purchase = result.getJSONObject("order")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "불러오지 못했습니다!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
            loadOrderDetail(purchase.getString("uid"), purchase.getString("date"), purchase.getString("expire"), purchase.getString("amount"), purchase.getBoolean("exchanged") )

        }
        var items: JSONObject? = purchase.getJSONObject("item") // stash item array
        if (items != null) {
            val itemArray = itemJsonToArray(items)
            Log.d("TAG", itemArray.toString())

            viewAdapter =
                itemsRecycler(false, this@MainActivity, itemArray) // use itme array
        } else {
            return
        }
        recyclerViewInit()
        setExchangeButtonListener(passedOrderId)

    }
}