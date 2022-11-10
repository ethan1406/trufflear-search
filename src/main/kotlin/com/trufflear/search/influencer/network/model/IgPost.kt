package com.trufflear.search.influencer.network.model

import com.google.gson.annotations.SerializedName

data class IgPost(
    @SerializedName("caption") val caption: String,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("media_url") val mediaUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("username") val username: String,
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: String,
)

data class IgUserMedia(
    @SerializedName("data") val data: List<IgPost>
)