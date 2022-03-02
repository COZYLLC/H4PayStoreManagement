package com.h4pay.store.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject

fun itemJsonToArray(items:JsonObject) : JsonArray {
    var i = 0
    var itemArray = JsonArray()
    items.keySet().forEach { id ->
        val qty:Int = items[id].asInt
        var item = JsonObject()
        item.addProperty("id", id.toInt())
        item.addProperty("qty", qty)
        itemArray.add(item)
        i++
    }
    return itemArray
}

fun itemArrayToJson(items: JsonArray) : JsonObject {
    val newItemObject = JsonObject()
    for (i in 0 until items.size()) {
        val itemObjectInArray = items[i].asJsonObject
        newItemObject.addProperty(itemObjectInArray["id"].asInt.toString(), itemObjectInArray["qty"].asInt)
    }
    return newItemObject
}