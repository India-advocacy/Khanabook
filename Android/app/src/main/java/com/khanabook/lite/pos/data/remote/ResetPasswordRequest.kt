package com.khanabook.lite.pos.data.remote

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("newPassword")
    val newPassword: String
)
