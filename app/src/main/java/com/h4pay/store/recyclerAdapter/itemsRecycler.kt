package com.h4pay.store.recyclerAdapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.h4pay.store.R
import com.h4pay.store.VoucherActivity
import com.h4pay.store.prodList
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.text.NumberFormat


class itemsRecycler(
    private val isMutable: Boolean,
    private val context: Context,
    private var items: JsonArray
) : RecyclerView.Adapter<itemsRecycler.Holder>() {

    private val TAG = "[DEBUG]"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.items_recyclerview, parent, false)
        return Holder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeItems(newItems: JsonArray) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItems(): JsonArray {
        return items;
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, ${items.size()} items")
        return items.size()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.d(TAG, "current position: $position")
        holder.bind(position, items[position].asJsonObject, context)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pName = itemView.findViewById<TextView>(R.id.pName)
        val pImage = itemView.findViewById<ImageView>(R.id.pImage)
        val amount = itemView.findViewById<TextView>(R.id.rec_amount)
        val delButton = itemView.findViewById<FloatingActionButton>(R.id.delButton)
        val addButton = itemView.findViewById<FloatingActionButton>(R.id.addButton)
        val delProductButton = itemView.findViewById<FloatingActionButton>(R.id.delProductButton)

        @SuppressLint("SetTextI18n")
        fun bind(position: Int, item: JsonObject, context: Context) {
            Log.d("TAG", item.toString())
            if (!isMutable) {
                delButton.visibility = View.GONE
                delProductButton.visibility = View.GONE
                addButton.visibility = View.GONE
            }

            delButton.setOnClickListener {
                for (i in 0 until items.size()) {
                    if (items[i].asJsonObject["id"].asInt == item["id"].asInt) {
                        if (item["qty"].asInt > 1) {
                            items.remove(position)
                            val currentQty = item["qty"].asInt
                            item.remove("qty")
                            item.addProperty("qty", currentQty - 1)
                            items.add(item)
                            notifyItemChanged(position)
                            (itemView.context as VoucherActivity)
                                .onRecyclerDataChanged()
                        } else if (item["qty"].asInt == 1) {
                            items.remove(position)
                            notifyItemRemoved(position);
                            (itemView.context as VoucherActivity)
                                .onRecyclerDataChanged()
                        }
                        return@setOnClickListener
                    }
                }
            }
            addButton.setOnClickListener {
                if (item["qty"].asInt >= 100) {
                    Toast.makeText(context, "한 제품 당 최대 갯수는 100개 입니다!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                for (i in 0 until items.size()) {
                    if (items[i].asJsonObject["id"].asInt == item["id"].asInt) {
                        items.remove(position)
                        val currentQty = item["qty"].asInt
                        item.remove("qty")
                        item.addProperty("qty", currentQty + 1)
                        items.add(item)
                        notifyItemChanged(position)
                        (itemView.context as VoucherActivity)
                            .onRecyclerDataChanged()
                    }
                }
            }
            delProductButton.setOnClickListener {
                for (i in 0 until items.size()) {
                    if (items[i].asJsonObject["id"].asInt == item["id"].asInt) {
                        items.remove(position);
                        notifyItemRemoved(position);
                        (itemView.context as VoucherActivity)
                            .onRecyclerDataChanged()
                    }
                }
            }
            val matchingProduct = prodList[item["id"].asInt]
            val gotName = matchingProduct.productName
            val gotAmount = " " + (item["qty"].asString) + " 개"
            val gotImage = matchingProduct.img
            //텍스트 설정
            if (item["qty"].asInt != 0) {
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

        fun add(position: Int, item: JsonObject) {
            items.add(item)
            notifyItemInserted(position)
        }
    }


}