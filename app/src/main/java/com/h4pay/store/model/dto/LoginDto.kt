package com.h4pay.store.model.dto

data class LoginDto(val id: String, val password: String, val sessionId: String) : ReqBody()