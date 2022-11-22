package com.trufflear.search.influencer.services

import com.trufflear.search.config.*
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.clientId
import com.trufflear.search.config.redirectUri
import com.trufflear.search.influencer.ConnectIgUserMediaRequest
import com.trufflear.search.influencer.ConnectIgUserMediaResponse
import com.trufflear.search.influencer.GetIgAuthorizationWindowUrlRequest
import com.trufflear.search.influencer.InfluencerAccountConnectIgServiceGrpcKt
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.InfluencerPostService
import com.trufflear.search.influencer.connectIgUserMediaResponse
import com.trufflear.search.influencer.getIgAuthorizationWindowUrlResponse
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.model.IgResponse
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext

class InfluencerAccountConnectIgService (
    private val influencerProfileRepository: InfluencerProfileRepository,
    private val influencerPostService: InfluencerPostService,
    private val igService: InstagramService
) : InfluencerAccountConnectIgServiceGrpcKt.InfluencerAccountConnectIgServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getIgAuthorizationWindowUrl(request: GetIgAuthorizationWindowUrlRequest) =
        getIgAuthorizationWindowUrlResponse {
            url = "$igApiSubdomainBaseUrl$authPath?${IgApiParams.clientId}=$clientId&" +
                    "${IgApiParams.redirectUri}=$redirectUri&${IgApiParams.responseType}=${IgResponseTypeFields.code}&" +
                    "${IgApiParams.scope}=${IgAuthScopeFields.userProfile},${IgAuthScopeFields.userMedia}"
        }

    override suspend fun connectIgUserMedia(request: ConnectIgUserMediaRequest): ConnectIgUserMediaResponse {
        logger.info ("connecting instagram for user")

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)


        logger.info { "checking if user exists" }
        if (influencerProfileRepository.userExists(influencer.email).not()) {
            throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))
        }

        val result = igService.getShortLivedToken(
            clientId = clientId,
            clientSecret = appSecret,
            grantType = IgCodeGrantType.authCodeGrantType,
            redirectUri = redirectUri,
            code = request.instagramAuthCode
        )

        when (result) {
            is IgServiceResult.PermissionError, IgServiceResult.ExpiredError,
            IgServiceResult.Unknown -> handleIgErrorResult(result)
            is IgServiceResult.Success -> {
                println("asdhfasd: ${result.response.accessToken}")
                withContext(Dispatchers.IO) {
                    listOf(
                        launch {
                            fetchAndStoreUserInfoAndToken(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                        },
                        launch {
                            fetchAndStoreUserPosts(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                        }
                    ).joinAll()
                }
            }
        }

        return connectIgUserMediaResponse { }
    }

    private fun handleIgErrorResult(result: IgServiceResult<IgResponse>) {
        when (result) {
            is IgServiceResult.PermissionError -> throw StatusException(Status.PERMISSION_DENIED)
            is IgServiceResult.ExpiredError -> throw StatusException(Status.INVALID_ARGUMENT)
            is IgServiceResult.Unknown -> throw StatusException(Status.UNKNOWN)
            is IgServiceResult.Success -> Unit
        }
    }

    private suspend fun fetchAndStoreUserInfoAndToken(
        accessToken: String,
        instagramUserId: String,
        influencerEmail: String
    ) {
        logger.info ("getting long lived access token for user: $influencerEmail")
        val tokenResult = igService.getLongLivedAccessToken(
            clientSecret = appSecret,
            grantType = IgCodeGrantType.exchangeTokenType,
            accessToken = accessToken
        )

        when (tokenResult) {
            is IgServiceResult.Success -> {
                logger.info ("getting user info for $influencerEmail")
                val userResult = igService.getUser(
                    userId = instagramUserId,
                    fields = getUserInfoFieldsString(),
                    accessToken = accessToken
                )

                when (userResult) {
                    is IgServiceResult.Success -> {
                        influencerProfileRepository.upsertInfluencerIgInfo(
                            tokenResponse = tokenResult.response,
                            igUser = userResult.response,
                            influencerEmail = influencerEmail,
                            instagramUserId = instagramUserId
                        ) ?: throw StatusException(Status.UNKNOWN)
                    }
                    else -> handleIgErrorResult(userResult)
                }
            }
            else -> handleIgErrorResult(tokenResult)
        }
    }

    private suspend fun fetchAndStoreUserPosts(
        accessToken: String,
        instagramUserId: String,
        influencerEmail: String
    ) {
        logger.info ("getting user media for $influencerEmail")

        val igPosts = getAllUserPosts(
            accessToken = accessToken,
            instagramUserId = instagramUserId
        )

        influencerPostService.handleIncomingPosts(
            influencerEmail = influencerEmail,
            igPosts = igPosts
        ) ?: throw StatusException(Status.UNKNOWN)

    }

    private suspend fun getAllUserPosts(
        instagramUserId: String,
        accessToken: String,
    ): List<IgPost> {
        val igPosts = mutableListOf<IgPost>()

        var afterToken: String? = null
        var nextLink: String? = null
        do {
            val mediaResult = igService.getUserMedia(
                userId = instagramUserId,
                limit = fetchingLimit,
                fields = getUserMediaFieldsString(),
                accessToken = accessToken,
                after = afterToken
            )

            when (mediaResult) {
                is IgServiceResult.Success -> {
                    igPosts.addAll(mediaResult.response.data)
                    nextLink = mediaResult.response.paging?.next
                    afterToken = mediaResult.response.paging?.cursors?.after
                }
                else -> {
                    handleIgErrorResult(mediaResult)
                }
            }
        } while (nextLink != null && afterToken != null)

        return igPosts
    }

    private fun getUserMediaFieldsString() = listOf(
        IgMediaFields.caption,
        IgMediaFields.mediaUrl,
        IgMediaFields.thumbnailUrl,
        IgMediaFields.mediaType,
        IgMediaFields.id,
        IgMediaFields.username,
        IgMediaFields.permalink,
        IgMediaFields.timestamp
    ).toString()
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")

    private fun getUserInfoFieldsString() = listOf(
        IgUserFields.id,
        IgUserFields.username,
        IgUserFields.accountType,
        IgUserFields.mediaCount
    ).toString()
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
}