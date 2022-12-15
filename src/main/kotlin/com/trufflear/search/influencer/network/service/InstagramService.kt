package com.trufflear.search.influencer.network.service

import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgShortLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import com.trufflear.search.influencer.network.model.IgUserMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import retrofit2.HttpException

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
                logger.debug { "Getting instagram short lived token with clientId: $clientId, " +
                        "grant type: $grantType, redirectUri: $redirectUri, code: $code" }

                val response = authService.getShortLivedToken(
                    clientId,
                    clientSecret,
                    grantType,
                    redirectUri,
                    code
                )
                IgServiceResult.Success(response)
            } catch (e: HttpException) {
                logger.error(e) { "error getting short lived token from instagram" }
                if (e.code() == 403) {
                    IgServiceResult.PermissionError
                } else {
                    IgServiceResult.ExpiredError
                }
            } catch (e: Exception) {
                logger.error(e) { "exception getting short lived token from instagram" }
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
                logger.error(e) { "error getting long lived token from instagram" }
                IgServiceResult.Unknown
            } catch (e: Exception) {
                logger.error(e) { "error getting long lived token from instagram" }
                IgServiceResult.Unknown
            }
        }

    suspend fun getUserMedia(
        userId: String,
        limit: Int,
        fields: String,
        accessToken: String,
        after: String?,
    ): IgServiceResult<IgUserMedia> =
        withContext(Dispatchers.IO) {
            try {
                val response = graphService.getUserMedia(
                    userId, limit, fields, accessToken, after
                )
                IgServiceResult.Success(response)
            } catch (e: HttpException) {
                logger.error(e) { "error getting user media from instagram" }
                if (e.code() == 403) {
                    IgServiceResult.PermissionError
                } else {
                    IgServiceResult.Unknown
                }
            } catch (e: Exception) {
                logger.error(e) { "error getting user media from instagram" }
                IgServiceResult.Unknown
            }
        }

    suspend fun getUser(
        userId: String,
        fields: String,
        accessToken: String,
    ): IgServiceResult<IgUserInfo> =
        withContext(Dispatchers.IO) {
            try {
                val response = graphService.getUser(
                    userId, fields, accessToken
                )
                IgServiceResult.Success(response)
            } catch (e: HttpException) {
                logger.error(e) { "error getting user info from Instagram" }
                IgServiceResult.Unknown
            } catch (e: Exception) {
                logger.error(e) { "error getting user info from Instagram" }
                IgServiceResult.Unknown
            }
        }
}

sealed class IgServiceResult<out IgResult> {

    data class Success<IgResult>(
        val response: IgResult
    ): IgServiceResult<IgResult>()

    object PermissionError: IgServiceResult<Nothing>()

    object ExpiredError: IgServiceResult<Nothing>()

    object Unknown: IgServiceResult<Nothing>()
}