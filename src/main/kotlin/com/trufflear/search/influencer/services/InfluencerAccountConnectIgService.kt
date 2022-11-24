package com.trufflear.search.influencer.services

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.config.IgAuthScopeFields
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.IgResponseTypeFields
import com.trufflear.search.config.IgUserFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.authPath
import com.trufflear.search.config.clientId
import com.trufflear.search.config.fetchingLimit
import com.trufflear.search.config.igApiSubdomainBaseUrl
import com.trufflear.search.config.redirectUri
import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.ConnectIgUserMediaRequest
import com.trufflear.search.influencer.ConnectIgUserMediaResponse
import com.trufflear.search.influencer.GetIgAuthorizationWindowUrlRequest
import com.trufflear.search.influencer.InfluencerAccountConnectIgServiceGrpcKt
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.InfluencerPostService
import com.trufflear.search.influencer.RefreshIgUserMediaRequest
import com.trufflear.search.influencer.RefreshIgUserMediaResponse
import com.trufflear.search.influencer.connectIgUserMediaResponse
import com.trufflear.search.influencer.getIgAuthorizationWindowUrlResponse
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.model.IgResponse
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            IgServiceResult.Unknown -> handleError(result.toError())
            is IgServiceResult.Success -> {
                println("asdhfasd: ${result.response.accessToken}")
                withContext(Dispatchers.IO) {
                    listOf(
                        launch {
                            val fetchResult = fetchAndStoreUserInfoAndToken(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                            if (fetchResult is Result.Error) {
                                handleError(fetchResult.error)
                            }
                        },
                        launch {
                            val fetchResult = fetchAndStoreUserPosts(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                            if (fetchResult is Result.Error) {
                                handleError(fetchResult.error)
                            }
                        }
                    ).joinAll()
                }
            }
        }

        return connectIgUserMediaResponse { }
    }

//    override suspend fun refreshIgUserMedia(request: RefreshIgUserMediaRequest): RefreshIgUserMediaResponse {
//        logger.info ("Refreshing instagram connections for user")
//
//        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
//            ?: throw StatusException(Status.UNAUTHENTICATED)
//
//        val igAuth = influencerProfileRepository.getIgAuth(influencer.email) ?: throw StatusException(Status.UNKNOWN)
//
//        fetchAndStoreUserPosts(
//            accessToken = igAuth.accessToken,
//            instagramUserId = igAuth.instagramId,
//            influencerEmail = influencer.email
//        )
//
//
//    }

    private fun handleError(error: Error) {
        when (error) {
            is Error.Unknown -> throw StatusException(Status.UNKNOWN)
            is Error.IgError -> {
                when (error) {
                    is Error.IgError.ExpiredError -> throw StatusException(Status.INVALID_ARGUMENT)
                    is Error.IgError.IgPermissionError -> throw StatusException(Status.PERMISSION_DENIED)
                }
            }
        }
    }

    private suspend fun fetchAndStoreUserInfoAndToken(
        accessToken: String,
        instagramUserId: String,
        influencerEmail: String
    ): Result<Unit, Error> {
        logger.info ("getting long lived access token for user: $influencerEmail")
        val tokenResult = igService.getLongLivedAccessToken(
            clientSecret = appSecret,
            grantType = IgCodeGrantType.exchangeTokenType,
            accessToken = accessToken
        )

        return when (tokenResult) {
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
                        )?.let {
                            Result.Success(Unit)
                        } ?: Result.Error(Error.Unknown)
                    }
                    else -> Result.Error(userResult.toError())
                }
            }
            else -> Result.Error(tokenResult.toError())
        }
    }

    private suspend fun fetchAndStoreUserPosts(
        accessToken: String,
        instagramUserId: String,
        influencerEmail: String
    ): Result<Unit, Error> {
        logger.info ("getting user media for $influencerEmail")

        val igPostResult = getAllUserPosts(
            accessToken = accessToken,
            instagramUserId = instagramUserId
        )

        return when (igPostResult) {
            is Result.Error -> igPostResult
            is Result.Success -> {
                influencerPostService.handleIncomingPosts(
                    influencerEmail = influencerEmail,
                    igPosts = igPostResult.value
                )?.let {
                    Result.Success(Unit)
                } ?: Result.Error(Error.Unknown)
            }
        }
    }

    private suspend fun getAllUserPosts(
        instagramUserId: String,
        accessToken: String,
    ): Result<List<IgPost>, Error> {
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
                    return Result.Error(mediaResult.toError())
                }
            }
        } while (nextLink != null && afterToken != null)

        return Result.Success(igPosts)
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

    internal sealed class Error {
        sealed class IgError: Error() {
            object IgPermissionError: IgError()
            object ExpiredError: IgError()
        }

        object Unknown: Error()
    }

    private fun IgServiceResult<Any>.toError(): Error =
        when (this) {
            is IgServiceResult.Unknown -> Error.Unknown
            is IgServiceResult.ExpiredError -> Error.IgError.ExpiredError
            is IgServiceResult.PermissionError -> Error.IgError.IgPermissionError
            is IgServiceResult.Success -> {
                logger.error { "IgService Result should not be converted to error in Connect Ig Service" }
                Error.Unknown
            }
        }
}