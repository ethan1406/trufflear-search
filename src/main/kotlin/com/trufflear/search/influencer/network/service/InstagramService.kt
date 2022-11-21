package com.trufflear.search.influencer.network.service

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgShortLivedTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import retrofit2.HttpException
import retrofit2.http.Query

class InstagramService(
    private val authService: IgAuthService,
    private val graphService: IgGraphService
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getShortLivedToken(
        clientId: String,
        clientSecret: String,
        grantType: String,
        redirectUri: String,
        code: String,
    ): IgServiceResult<IgShortLivedTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = authService.getShortLivedToken(
                    clientId,
                    clientSecret,
                    grantType,
                    redirectUri,
                    code
                )
                IgServiceResult.Success(response)
            } catch (e: HttpException) {
                logger.error(e) { "error with API calls to Instagram" }
                if (e.code() == 403) {
                    IgServiceResult.PermissionError
                } else {
                    IgServiceResult.ExpiredError
                }
            } catch (e: Exception) {
                logger.error(e) { "unknown error while connecting instagram" }
                IgServiceResult.Unknown
            }
        }

    suspend fun getLongLivedAccessToken(
        grantType: String,
        clientSecret: String,
        accessToken: String,
    ): IgServiceResult<IgLongLivedTokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = graphService.getLongLivedAccessToken(
                    grantType, clientSecret, accessToken
                )
                IgServiceResult.Success(response)
            } catch (e: HttpException) {
                logger.error(e) { "error with API calls to Instagram" }
                IgServiceResult.Unknown
            } catch (e: Exception) {
                logger.error(e) { "unknown error while connecting instagram" }
                IgServiceResult.Unknown
            }
        }
}

sealed class IgServiceResult<out T> {

    data class Success<T>(
        val response: T
    ): IgServiceResult<T>()

    object PermissionError: IgServiceResult<Nothing>()

    object ExpiredError: IgServiceResult<Nothing>()

    object Unknown: IgServiceResult<Nothing>()
}