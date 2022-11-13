package com.trufflear.search.influencer.network.service

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import com.trufflear.search.influencer.network.model.IgUserMedia
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IgGraphService {

    @GET("/access_token")
    suspend fun getLongLivedAccessToken(
        @Query(IgApiParams.grantType) grantType: String,
        @Query(IgApiParams.clientSecret) clientSecret: String,
        @Query(IgApiParams.accessToken) accessToken: String,
    ): IgLongLivedTokenResponse

    @GET("/{user-id}/media")
    suspend fun getUserMedia(
        @Path("user-id") userId: String,
        @Query(IgApiParams.fields, encoded = true) fields: String,
        @Query(IgApiParams.accessToken) accessToken: String,
    ): IgUserMedia

    @GET("/{user-id}")
    suspend fun getUser(
        @Path("user-id") userId: String,
        @Query(IgApiParams.fields, encoded = true) fields: String,
        @Query(IgApiParams.accessToken) accessToken: String,
    ): IgUserInfo
}