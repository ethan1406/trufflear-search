package com.trufflear.search.influencer.network.model

import com.google.gson.annotations.SerializedName

data class IgPost(
    @SerializedName("caption") val caption: String?,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("media_url") val mediaUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("permalink") val permalink: String,
    @SerializedName("username") val username: String,
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: String,
)

data class Paging(
    @SerializedName("cursors") val cursors: Cursors?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("next") val next: String?
)

data class Cursors(
    @SerializedName("before") val before: String?,
    @SerializedName("after") val after: String?
)

data class IgUserMedia(
    @SerializedName("data") val data: List<IgPost>,
    @SerializedName("paging") val paging: Paging?
)

data class IgUserInfo(
    @SerializedName("id") val userId: String,
    @SerializedName("username") val userName: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("media_count") val mediaCount: Int
)