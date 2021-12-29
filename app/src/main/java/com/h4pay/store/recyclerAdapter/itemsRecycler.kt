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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.h4pay.store.R
import com.h4pay.store.VoucherActivity
import com.h4pay.store.prodList
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.text.NumberFormat


class itemsRecycler( private val isRemovable:Boolean, private val context: Context, private var items: JSONArray) : RecyclerView.Adapter<itemsRecycler.Holder>() {

    private val TAG = "[DEBUG]"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.items_recyclerview, parent, false)
        return Holder(view)
    }

    interface OnItemClickListner {
        fun onItemClick(v:View, positon:Int){

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeItems(newItems: JSONArray) {
        items = newItems
        notifyDataSetChanged()
    }

    private var mListner:OnItemClickListner? = null;

    fun setOnItemClickListner(listner:OnItemClickListner){
        this.mListner = listner
    }
   fun getItems() : JSONArray {
        return items;
   }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, ${items.length()} items")
        return items.length()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.d(TAG, "current position: $position")
        holder.bind(position, items.getJSONObject(position), context)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pName = itemView.findViewById<TextView>(R.id.pName)
        val pImage = itemView.findViewById<ImageView>(R.id.pImage)
        val amount = itemView.findViewById<TextView>(R.id.rec_amount)
        val removeButton = itemView.findViewById<FloatingActionButton>(R.id.removeButton)
        @SuppressLint("SetTextI18n")
        fun bind(position: Int, item:JSONObject, context: Context) {
            Log.d("TAG", item.toString())
            if (!isRemovable){
                removeButton.visibility = View.GONE
            }

            removeButton.setOnClickListener {
                    for (i in 0 until items.length()) {
                        if (items.getJSONObject(i).getInt("id") == item.getInt("id")) {
                            if (item.getInt("qty") > 1) {
                                items.remove(position)
                                val currentQty = item.getInt("qty")
                                item.remove("qty")
                                item.put("qty", currentQty - 1)
                                items.put(item)
                                notifyItemChanged(position)
                                (itemView.context as VoucherActivity)
                                    .onRecyclerDelButtonClicked()
                            } else if (item.getInt("qty") == 1) {
                                items.remove(position)
                                notifyItemRemoved(position);
                                (itemView.context as VoucherActivity)
                                    .onRecyclerDelButtonClicked()
                            }
                            return@setOnClickListener
                        }
                    }
            }
            val gotName = prodList.getJSONObject(item.getInt("id")).getString("productName")
            val gotAmount = " " + (item.getInt("qty")).toString() + " 개"
            val gotImage = prodList.getJSONObject(item.getInt("id")).getString("img")
            //텍스트 설정
            if (item.getInt("qty") != 0) {
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
        fun add(position:Int, item:JSONObject) {
            items.put(item)
            notifyItemInserted(position)
        }
    }


}