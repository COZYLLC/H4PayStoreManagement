package com.h4pay.store.recyclerAdapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.h4pay.store.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat


class orderRecycler(val context: Context, val orders: JSONArray?) : RecyclerView.Adapter<orderRecycler.Holder>() {
    private val TAG = "[DEBUG]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.my_text_view, parent, false)

        return Holder(view)
    }

    override fun getItemCount(): Int {
        if (orders != null){
            Log.d(TAG, "getItemCount called, ${orders.length()} items")
            return orders.length()
        }else{
            return 0
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.d(TAG, "current position: $position")
        if (orders != null) {
            holder.bind(orders[position] as JSONObject, context)
        }

        holder.itemView.setOnClickListener {
            holder.itemView
        }
    }




    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderId = itemView.findViewById<TextView>(R.id.orderId)
        val date = itemView.findViewById<TextView>(R.id.date)
        val amount = itemView.findViewById<TextView>(R.id.amount)
        val exchange = itemView.findViewById<TextView>(R.id.itsExchanged)
        //val pName = itemView.findViewById<TextView>(R.id.pName)
        @SuppressLint("SetTextI18n")
        fun bind(orders: JSONObject, context: Context) {
            //텍스트 설정
            val f = NumberFormat.getInstance()
            f.isGroupingUsed = false
            orderId.text = "No. " + f.format(orders.get("orderid"))
            date.text = orders.get("date").toString()
            amount.text = orders.get("amount").toString() + " ₩"
            val exchanged = orders.get("exchanged")
            if (exchanged == true){
                exchange.text = "교환 됨"
                exchange.setTextColor(Color.parseColor("#DC3545"))
            } else{
                exchange.text = "교환 안됨"
                exchange.setTextColor(Color.parseColor("#28A745"))
            }
            //리스너 객체 호출
        }


    }


}