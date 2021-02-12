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


class itemsRecycler(val context: Context, val items: JSONArray) : RecyclerView.Adapter<itemsRecycler.Holder>() {

    private val TAG = "[DEBUG]"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.items_recyclerview, parent, false)
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
        val pName = itemView.findViewById<TextView>(R.id.pName)
        val pImage = itemView.findViewById<ImageView>(R.id.pImage)
        val amount = itemView.findViewById<TextView>(R.id.rec_amount)
        //val pName = itemView.findViewById<TextView>(R.id.pName)
        @SuppressLint("SetTextI18n")
        fun bind(item: JSONObject, context: Context) {
            val gotName = prodList.getJSONObject(item.getInt("id")).getString("productName")
            val gotAmount = " " + (item.getInt("amount")).toString() + " 개"
            val gotImage = prodList.getJSONObject(item.getInt("id")).getString("img")
            //텍스트 설정
            if (item.getInt("amount") != 0) {
                val f = NumberFormat.getInstance()
                f.isGroupingUsed = false
                pName.text = gotName
                amount.text = gotAmount
                val TAG = "prodList IMG"
                Log.e(TAG, gotImage)
                if (gotImage != "") {
                    Glide.with(context)
                        .load(gotImage)
                        .into(pImage)
                }
            }

        }
    }


}