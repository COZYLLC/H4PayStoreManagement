package com.h4pay.store

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent.getActivity
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
import com.google.android.material.internal.ContextUtils.getActivity
import com.h4pay.store.networking.*
import com.h4pay.store.networking.tools.JSONTools
import com.h4pay.store.recyclerAdapter.RecyclerItemClickListener
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.recyclerAdapter.orderRecycler
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class orderList : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerView2: RecyclerView
    private lateinit var viewAdapter2: RecyclerView.Adapter<*>
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
        recyclerView2 = findViewById(R.id.itemRecyclerView)

        exchangeSuccess.apply {
            isEnabled = false
            isVisible = false
        }
        cancel.apply {
            isEnabled = false
            isVisible = false
        }
        //--------------------

        //----Data Load and RecyclerView Init----
        viewManager = LinearLayoutManager(this)
        var newJsonArray = JSONArray()
        val orderListRes = Get("${BuildConfig.API_URL}/orders/retrieveall").execute().get()
        if (orderListRes == null) {
            showServerError(this)
            return
        } else {
            if (!orderListRes.getBoolean("status") || orderListRes.getJSONArray("order") == null) {
                Log.e(TAG, "orderList null")
                viewAdapter = orderRecycler(this, JSONArray("[]"))
                Toast.makeText(this, "주문내역이 존재하지 않습니다.", Toast.LENGTH_LONG).show()
                return
            } else {
                val _orderList = orderListRes.getJSONArray("order")
                for (i in _orderList.length() - 1 downTo 0) {
                    // Perform your regular JSON Parsing here
                    newJsonArray.put(_orderList.get(i))
                }
                Log.e(TAG, _orderList.toString())
                viewAdapter = orderRecycler(this, newJsonArray)
            }
        }

        var uid = findViewById<TextView>(R.id.orderlist_uid)
        var orderID = findViewById<TextView>(R.id.orderlist_orderID)
        var date = findViewById<TextView>(R.id.orderlist_date)
        var expire = findViewById<TextView>(R.id.orderlist_expire)
        var amount = findViewById<TextView>(R.id.orderlist_amount)
        var exchanged = findViewById<TextView>(R.id.orderlist_exchanged)
        recyclerView2 = findViewById(R.id.itemRecyclerView)

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
                            handleOrder().loadOrder(
                                this@orderList,
                                intent,
                                this@orderList as Activity,
                                newJsonArray,
                                position,
                                uid,
                                orderID,
                                date,
                                expire,
                                amount,
                                exchanged,
                                recyclerView2,
                                exchangeSuccess,
                                cancel
                            )
                        }
                    }
                )
            )

            // specify an viewAdapter (see also next example)

        }
        val notiOrderID = intent.getStringExtra("orderID")

        if (notiOrderID != null) {
            for (i in 0..newJsonArray.length() - 1) {
                if (newJsonArray.getJSONObject(i).get("orderid") == notiOrderID) {
                    Log.e(
                        TAG,
                        "same item clicked, " + newJsonArray.getJSONObject(i)
                            .get("orderid") + ", position: " + i
                    )
                    handleOrder().loadOrder(
                        this,
                        intent,
                        this@orderList as Activity,
                        newJsonArray,
                        i,
                        uid,
                        orderID,
                        date,
                        expire,
                        amount,
                        exchanged,
                        recyclerView2,
                        exchangeSuccess,
                        cancel
                    )
                }
            }
        }

        //--------------------------------------


    }


}