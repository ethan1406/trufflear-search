package com.trufflear.search.influencer.network.service

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.config.IgApiPaths
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

    @GET("/{${IgApiPaths.userId}}/media")
    suspend fun getUserMedia(
        @Path(IgApiPaths.userId) userId: String,
        @Query(IgApiParams.limit) limit: Int,
        @Query(IgApiParams.fields, encoded = true) fields: String,
        @Query(IgApiParams.accessToken) accessToken: String,
        @Query(IgApiParams.after) after: String?,
    ): IgUserMedia

    @GET("/{${IgApiPaths.userId}}")
    suspend fun getUser(
        @Path(IgApiPaths.userId) userId: String,
        @Query(IgApiParams.fields, encoded = true) fields: String,
        @Query(IgApiParams.accessToken) accessToken: String,
    ): IgUserInfo
}