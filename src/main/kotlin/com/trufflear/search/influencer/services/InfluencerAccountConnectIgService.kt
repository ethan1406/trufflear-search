package com.trufflear.search.influencer.services

import com.trufflear.search.config.IgApiParams
import com.trufflear.search.config.IgAuthScopeFields
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgResponseTypeFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.authPath
import com.trufflear.search.config.clientId
import com.trufflear.search.config.igApiSubdomainBaseUrl
import com.trufflear.search.config.redirectUri
import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.ConnectIgUserMediaRequest
import com.trufflear.search.influencer.ConnectIgUserMediaResponse
import com.trufflear.search.influencer.GetIgAuthorizationWindowUrlRequest
import com.trufflear.search.influencer.InfluencerAccountConnectIgServiceGrpcKt
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.RefreshIgUserMediaRequest
import com.trufflear.search.influencer.RefreshIgUserMediaResponse
import com.trufflear.search.influencer.connectIgUserMediaResponse
import com.trufflear.search.influencer.getIgAuthorizationWindowUrlResponse
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.refreshIgUserMediaResponse
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext

class InfluencerAccountConnectIgService (
    private val influencerProfileRepository: InfluencerProfileRepository,
    private val igService: InstagramService,
    private val igHandlingService: IgHandlingService

) : InfluencerAccountConnectIgServiceGrpcKt.InfluencerAccountConnectIgServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getIgAuthorizationWindowUrl(request: GetIgAuthorizationWindowUrlRequest) =
        getIgAuthorizationWindowUrlResponse { url = getIgAuthWindowUrl()}

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
            IgServiceResult.Unknown -> handleError(result.toError(logger))
            is IgServiceResult.Success -> {
                println("asdhfasd: ${result.response.accessToken}")
                coroutineScope {
                    val fetchTasks = listOf(
                        async {
                            igHandlingService.fetchAndStoreUserInfoAndToken(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                        },
                        async {
                            igHandlingService.fetchAndStoreUserPosts(
                                accessToken = result.response.accessToken,
                                instagramUserId = result.response.userId,
                                influencerEmail = influencer.email
                            )
                        }
                    ).awaitAll()

                    fetchTasks.forEach {
                        if (it is Result.Error) {
                            handleError(it.error)
                        }
                    }
                }
            }
        }

        return connectIgUserMediaResponse { }
    }

    override suspend fun refreshIgUserMedia(request: RefreshIgUserMediaRequest): RefreshIgUserMediaResponse {
        logger.info ("Refreshing instagram connections for user")

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        val igAuth = influencerProfileRepository.getIgAuth(influencer.email) ?: throw StatusException(Status.UNKNOWN)

        if (igAuth.accessToken.isBlank() || igAuth.instagramId.isBlank()) {
            logger.debug("Ig auth access token or instagram id is missing. AccessToken: ${igAuth.accessToken} Id: ${igAuth.instagramId}")
            return refreshIgUserMediaResponse {
                authorizationRequired = RefreshIgUserMediaResponse.AuthorizationRequired.newBuilder()
                    .setAuthWindowUrl(getIgAuthWindowUrl())
                    .build()
            }
        }

        val result = igHandlingService.fetchAndStoreUserPosts(
            accessToken = igAuth.accessToken,
            instagramUserId = igAuth.instagramId,
            influencerEmail = influencer.email
        )

        return when (result) {
            is Result.Error -> refreshIgUserMediaResponse {
                authorizationRequired = RefreshIgUserMediaResponse.AuthorizationRequired.newBuilder()
                    .setAuthWindowUrl(getIgAuthWindowUrl())
                    .build()
            }
            is Result.Success -> refreshIgUserMediaResponse {
                success = RefreshIgUserMediaResponse.Success.getDefaultInstance()
            }
        }
    }

    private fun handleError(error: IgHandlingService.Error) {
        when (error) {
            is IgHandlingService.Error.Unknown -> throw StatusException(Status.UNKNOWN)
            is IgHandlingService.Error.Instagram -> {
                when (error) {
                    is IgHandlingService.Error.Instagram.ExpiredError -> throw StatusException(Status.INVALID_ARGUMENT)
                    is IgHandlingService.Error.Instagram.PermissionError -> throw StatusException(Status.PERMISSION_DENIED)
                }
            }
        }
    }

    private fun getIgAuthWindowUrl() = "$igApiSubdomainBaseUrl$authPath?${IgApiParams.clientId}=$clientId&" +
            "${IgApiParams.redirectUri}=$redirectUri&${IgApiParams.responseType}=${IgResponseTypeFields.code}&" +
            "${IgApiParams.scope}=${IgAuthScopeFields.userProfile},${IgAuthScopeFields.userMedia}"

}