package com.trufflear.search.influencer.network.model

import com.google.gson.annotations.SerializedName

data class IgShortLivedTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("user_id") val userId: String
)

data class IgLongLivedTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: String
)