package com.khanabook.lite.pos.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data class matching the server's PushSyncResponse shape.
 * Used to handle sync responses without crashing.
 */
data class PushSyncResponse(
    @SerializedName("successfulLocalIds")
    val successfulLocalIds: List<Int>,
    @SerializedName("failedLocalIds")
    val failedLocalIds: List<Int>
)
