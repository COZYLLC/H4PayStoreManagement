package com.h4pay.store.model.dto

import com.google.gson.JsonObject
import com.h4pay.store.model.dto.ReqBody

data class ExchangeVoucherDto(val id: String, val item: JsonObject) : ReqBody()
