package com.h4pay.store.util

import org.json.JSONArray
import org.json.JSONObject

fun itemJsonToArray(items:JSONObject) : JSONArray {
    var i = 0
    var itemArray = JSONArray()
    items.keys().forEach { id ->
        val qty = items.getInt(id);
        var item = JSONObject()
        item.put("id", id.toInt())
        item.put("qty", qty)
        itemArray.put(i, item)
        i++
    }
    return itemArray
}