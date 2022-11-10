package com.trufflear.search.influencer.network.service

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.influencer.network.model.IgShortLivedTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IgAuthService {

    @FormUrlEncoded
    @POST("/oauth/access_token")
    suspend fun getShortLivedToken(
        @Field(IgApiParams.clientId) clientId: String,
        @Field(IgApiParams.clientSecret) clientSecret: String,
        @Field(IgApiParams.grantType) grantType: String,
        @Field(IgApiParams.redirectUri) redirectUri: String,
        @Field(IgApiParams.code) code: String,
    ): IgShortLivedTokenResponse

}