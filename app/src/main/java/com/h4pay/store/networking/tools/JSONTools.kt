package com.h4pay.store.networking.tools

import android.util.Log
import com.h4pay.store.recyclerAdapter.itemsRecycler
import org.json.JSONArray

object JSONTools {
    fun deleteUnusedCartItems(items: JSONArray): JSONArray {
        Log.d("JSONTOOLS", "${items.length()} 개 존재")
        Log.d("JSONTOOLS", items.toString())
        var newItems = JSONArray()

        for (i in 0 until items.length()) {
            if (items.getJSONObject(i).getInt("qty") != 0) {
                Log.d("JSONTOOLS", "not empty. add.")
                newItems.put(items.getJSONObject(i))
            }
        }
        Log.d("JSONTOOLS", items.toString())
        return newItems
    }
}