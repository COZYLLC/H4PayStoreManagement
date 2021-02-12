package com.h4pay.store.recyclerAdapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h4pay.store.R
import com.h4pay.store.prodList
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat


class productRecycler(val context: Context, val items: JSONArray) : RecyclerView.Adapter<productRecycler.Holder>() {

    private val TAG = "[DEBUG]"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.productrecycler, parent, false)
        return Holder(view)
    }


    interface OnItemClickListner {
        fun onItemClick(v:View, positon:Int){

        }
    }

    private var mListner:OnItemClickListner? = null;

    fun setOnItemClickListner(listner:OnItemClickListner){
        this.mListner = listner
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, ${items.length()} items")
        return items.length()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.d(TAG, "current position: $position")
        holder.bind(items.getJSONObject(position), context)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pName = itemView.findViewById<TextView>(R.id.productName)
        val soldout = itemView.findViewById<TextView>(R.id.soldout)
        val amount = itemView.findViewById<TextView>(R.id.amount)

        //val pName = itemView.findViewById<TextView>(R.id.pName)
        @SuppressLint("SetTextI18n")
        fun bind(item: JSONObject, context: Context) {
            pName.text = item.getString("productName")
            soldout.text = if (item.getBoolean("soldout")) {"품절"} else {"재고 있음"}
            amount.text = item.getString("price")
        }
    }


}