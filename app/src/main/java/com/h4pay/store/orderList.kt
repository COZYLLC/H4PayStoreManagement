package com.h4pay.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h4pay.store.networking.cancelOrder
import com.h4pay.store.networking.exchangeOrder
import com.h4pay.store.networking.getOrderList
import com.h4pay.store.recyclerAdapter.RecyclerItemClickListener
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.recyclerAdapter.orderRecycler
import org.json.JSONArray
import java.lang.Integer.reverse
import java.lang.Long.reverse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Collections.reverse


class orderList : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerView2: RecyclerView
    private lateinit var viewAdapter2: RecyclerView.Adapter<*>
    private lateinit var viewManager2: RecyclerView.LayoutManager
    private var TAG = "orderList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orderlist)



        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire()
                }
            }



        //------UI Init-------
        var exchangeSuccess = findViewById<LinearLayout>(R.id.exchangeSuccess)
        var cancel = findViewById<LinearLayout>(R.id.cancel)
        recyclerView2 = findViewById<RecyclerView>(R.id.productRecyclerView)

        exchangeSuccess.apply{
            isEnabled = false
            isVisible = false
        }
        cancel.apply{
            isEnabled = false
            isVisible = false
        }
        //--------------------

        //----Data Load and RecyclerView Init----
        viewManager = LinearLayoutManager(this)

        val orderList: JSONArray? = getOrderList().execute().get()
        var newJsonArray = JSONArray()
        if (orderList == null){
            Log.e(TAG, "orderList null")
            viewAdapter = orderRecycler(this, JSONArray("[]"))
            Toast.makeText(this, "주문내역이 존재하지 않습니다.", Toast.LENGTH_LONG).show()
            return
        }
        else{

            for (i in orderList.length() - 1 downTo 0)
            {
                // Perform your regular JSON Parsing here
                newJsonArray.put(orderList.get(i))
            }
            Log.e(TAG, orderList.toString())
            viewAdapter = orderRecycler(this, newJsonArray)
        }



        recyclerView = findViewById<RecyclerView>(R.id.orderListView).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            adapter = viewAdapter
            layoutManager = viewManager

            addOnItemTouchListener(
                RecyclerItemClickListener(applicationContext, this,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        @SuppressLint("SetTextI18n")
                        override fun onItemClick(view: View, position: Int) {
                            loadOrder(newJsonArray, position)
                        }
                    }
                )
            )

            // specify an viewAdapter (see also next example)

        }
        val notiOrderID = intent.getDoubleExtra("orderID", 0.0)

        if (notiOrderID != 0.0){
            for (i in 0..newJsonArray.length()-1){
                if (newJsonArray.getJSONObject(i).get("orderid") == notiOrderID) {
                    Log.e(TAG,
                        "same item clicked, " + newJsonArray.getJSONObject(i)
                            .get("orderid") + ", position: " + i
                    )
                    loadOrder(newJsonArray, i)
                }
            }
        }

        //--------------------------------------



    }

    fun loadOrder(orderList:JSONArray, position:Int){

        var uid = findViewById<TextView>(R.id.orderlist_uid)
        var orderID = findViewById<TextView>(R.id.orderlist_orderID)
        var date = findViewById<TextView>(R.id.orderlist_date)
        var expire = findViewById<TextView>(R.id.orderlist_expire)
        var amount = findViewById<TextView>(R.id.orderlist_amount)
        var exchangeSuccess = findViewById<LinearLayout>(R.id.exchangeSuccess)
        var exchanged = findViewById<TextView>(R.id.orderlist_exchanged)
        var cancel = findViewById<LinearLayout>(R.id.cancel)
        recyclerView2 = findViewById<RecyclerView>(R.id.productRecyclerView)
        // event code

        val order = orderList.getJSONObject(position)
        val f = NumberFormat.getInstance()
        f.isGroupingUsed = false

        val format = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
        )

        val format2 = SimpleDateFormat(
                "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
        )
        format.timeZone = TimeZone.getTimeZone("UTC")
        format2.timeZone = TimeZone.getTimeZone("UTC")

        val moneyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        val pretifiedDate = format.parse(order.getString("date"))
        val pretifiedExpire = format.parse(order.getString("expire"))


        uid.text = "사용자 ID " + order.getString("uid")
        orderID.text = "주문번호 " +  f.format(order.get("orderid"))
        date.text = "주문 일시 ${format2.format(pretifiedDate)}"
        expire.text = "사용 기한 ${format2.format(pretifiedExpire)}"
        amount.text = "결제 금액 ${moneyFormat.format(order.getInt("amount"))}"
        exchangeSuccess.apply{
            isEnabled = true
            isVisible = true
        }
        cancel.apply{
            isEnabled = true
            isVisible = true
        }

        //----------RecyclerView Init-----------
        var items = order.getJSONArray("item")
        for (i in 0 until items.length()-1){
            if (items.getJSONObject(i).getInt("amount") == 0){
                items.remove(i)
            }
        }

        viewAdapter2 = itemsRecycler(this@orderList, items)
        val lm2 = LinearLayoutManager(this@orderList, LinearLayoutManager.HORIZONTAL, false)

        recyclerView2.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = lm2


            // specify an viewAdapter (see also next example)
            adapter = viewAdapter2

        }
        recyclerView2.isVisible = true
        if (order.getBoolean("exchanged")){
            exchanged.text = "교환됨"
            exchanged.background = ContextCompat.getDrawable(this@orderList, R.drawable.rounded_red)
            exchangeSuccess.isEnabled = false
        }
        else{
            exchanged.text = "교환 안 됨"
            exchanged.background = ContextCompat.getDrawable(this@orderList, R.drawable.rounded_green)
            exchangeSuccess.isEnabled = true


            //------Cancel and Exchange Button OnClick Event----------

            exchangeSuccess.setOnClickListener {


                val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {

                            if (exchangeOrder(f.format(order.get("orderid"))).execute().get() == true) {
                                Toast.makeText(this@orderList, "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
                                uid.text = ""
                                orderID.text = ""
                                date.text = ""
                                expire.text = ""
                                amount.text = ""
                                exchanged.text = ""
                                exchanged.background = null
                                recyclerView2.isVisible = false
                                finish();
                                startActivity(getIntent());
                            }
                            else{
                                Toast.makeText(this@orderList, "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                }


                AlertDialog.Builder(this@orderList, R.style.AlertDialogTheme)
                        .setTitle("확인")
                        .setMessage("정말로 교환처리 하시겠습니까?")
                        .setPositiveButton("예", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener)
                        .show()


            }


            cancel.setOnClickListener {
                val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            Log.e(TAG, "yes")
                            if (cancelOrder(f.format(order.get("orderid"))).execute().get() == true) {
                                Toast.makeText(this@orderList, "취소가 정상적으로 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                uid.text = ""
                                orderID.text = ""
                                date.text = ""
                                expire.text = ""
                                amount.text = ""
                                exchanged.text = ""
                                exchanged.background = null
                                recyclerView2.isVisible = false
                                finish();
                                startActivity(getIntent());
                            }
                            else{
                                Toast.makeText(this@orderList, "취소에 실패했습니다.\n없는 주문번호입니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            Log.e(TAG, "no")
                        }
                    }
                }

                AlertDialog.Builder(this@orderList, R.style.AlertDialogTheme)
                        .setTitle("확인")
                        .setMessage("정말로 취소처리 하시겠습니까?")
                        .setPositiveButton("예", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener)
                        .show()
            }

        }


    }
}