package com.h4pay.store

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h4pay.store.networking.Get
import com.h4pay.store.networking.Post
import com.h4pay.store.networking.tools.JSONTools
import com.h4pay.store.recyclerAdapter.itemsRecycler
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class handleOrder {
    private val TAG = "handleOrder"
    fun exchangeOrder(context: Context, intent: Intent, activity: Activity, order: JSONObject, uid: TextView, orderID: TextView, date: TextView, expire: TextView, amount: TextView, exchanged: TextView, itemRecycler: RecyclerView){
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val req = JSONObject().accumulate("orderId", order.getString("orderid"))
                    val exchangeRes = Post("${BuildConfig.API_URL}/orders/exchange", req).execute().get()
                    if (exchangeRes == null) {
                        showServerError(activity)
                        return@OnClickListener
                    }else {
                        if (exchangeRes.getBoolean("status")) {
                            Toast.makeText(context, "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
                            uid.text = ""
                            orderID.text = ""
                            date.text = ""
                            expire.text = ""
                            amount.text = ""
                            exchanged.text = ""
                            exchanged.background = null
                            itemRecycler.isVisible = false
                            activity.finish()
                            activity.startActivity(intent)
                        }
                        else{
                            Toast.makeText(context, "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }


        AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setTitle("확인")
            .setMessage("정말로 교환처리 하시겠습니까?")
            .setPositiveButton("예", dialogClickListener)
            .setNegativeButton("아니오", dialogClickListener)
            .show()


    }

    fun cancelOrder(context: Context, intent: Intent, activity: Activity, order: JSONObject, uid: TextView, orderID: TextView, date: TextView, expire: TextView, amount: TextView, exchanged: TextView, itemRecycler: RecyclerView){
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val cancelRes = Get("${BuildConfig.API_URL}/orders/cancel/${order.getString("orderid")}").execute().get()
                    if (cancelRes == null) {
                        showServerError(activity)
                        return@OnClickListener
                    }else {
                        if (cancelRes.getBoolean("status")) {
                            Toast.makeText(context, "취소가 정상적으로 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            uid.text = ""
                            orderID.text = ""
                            date.text = ""
                            expire.text = ""
                            amount.text = ""
                            exchanged.text = ""
                            exchanged.background = null
                            itemRecycler.isVisible = false
                            activity.finish();
                            activity.startActivity(intent);
                        }
                        else{
                            Toast.makeText(context, "취소에 실패했습니다.\n없는 주문번호입니다.", Toast.LENGTH_LONG).show()
                        }
                    }

                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Log.e(TAG, "no")
                }
            }
        }

        AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setTitle("확인")
            .setMessage("정말로 취소처리 하시겠습니까?")
            .setPositiveButton("예", dialogClickListener)
            .setNegativeButton("아니오", dialogClickListener)
            .show()
    }

    fun loadOrder(
        context: Context,
        intent:Intent,
        activity:Activity,
        orderList: JSONArray,
        position: Int,
        uid: TextView,
        orderID: TextView,
        date: TextView,
        expire: TextView,
        amount: TextView,
        exchanged: TextView,
        itemRecyclerView: RecyclerView,
        exchangeSuccess: LinearLayout,
        cancel: LinearLayout
    ){

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
        orderID.text = "주문번호 " +  order.getString("orderid")
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
        val items = JSONTools.deleteUnusedCartItems(order.getJSONArray("item"))
        val itemViewAdapter = itemsRecycler(context, items)
        val itemViewManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        itemRecyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = itemViewManager


            // specify an viewAdapter (see also next example)
            adapter = itemViewAdapter

        }
        itemRecyclerView.isVisible = true
        if (order.getBoolean("exchanged")){
            exchanged.text = "교환됨"
            exchanged.background = ContextCompat.getDrawable(context, R.drawable.rounded_red)
            exchangeSuccess.isVisible = false
            exchangeSuccess.isEnabled = false
            cancel.isVisible = false
            cancel.isEnabled = false
        }
        else{
            exchanged.text = "교환 안 됨"
            exchanged.background = ContextCompat.getDrawable(context, R.drawable.rounded_green)
            exchangeSuccess.isVisible = true
            exchangeSuccess.isEnabled = true
            cancel.isVisible = true
            cancel.isEnabled = true


            //------Cancel and Exchange Button OnClick Event----------

            exchangeSuccess.setOnClickListener {
                exchangeOrder(context, intent, activity, order, uid, orderID, date, expire, amount, exchanged, itemRecyclerView)
            }

            cancel.setOnClickListener {
                cancelOrder(context, intent, activity, order, uid, orderID, date, expire, amount, exchanged, itemRecyclerView)
            }

        }


    }
}