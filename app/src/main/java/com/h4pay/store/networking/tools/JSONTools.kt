package com.h4pay.store.networking.tools

import com.h4pay.store.recyclerAdapter.itemsRecycler
import org.json.JSONArray

object JSONTools {
    fun deleteUnusedCartItems(items: JSONArray): JSONArray {
        for (i in 0 until items.length() - 1) {
            if (items.getJSONObject(i).getInt("amount") == 0) {
                items.remove(i)
            }
        }
        return items
    }
}