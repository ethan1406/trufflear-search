package com.trufflear.search.influencer.services

import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.IgUserFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.fetchingLimit
import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.mappers.toPostDomain
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.util.CaptionParser
import mu.KLogger
import mu.KotlinLogging

class IgHandlingService(
    private val influencerPostHandlingService: InfluencerPostHandlingService,
    private val igService: InstagramService,
    private val influencerProfileRepository: InfluencerProfileRepository,
    private val captionParser: CaptionParser,
    private val storageService: StorageService
) {

    private val logger = KotlinLogging.logger {}

    internal suspend fun fetchAndStoreUserInfoAndToken(
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
                    else -> Result.Error(userResult.toError(logger))
                }
            }
            else -> Result.Error(tokenResult.toError(logger))
        }
    }

    internal suspend fun fetchAndStoreUserPosts(
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
                val posts = igPostResult.value.map { it.toPostDomain(captionParser, storageService, logger) }
                val handleResult = influencerPostHandlingService.handleIncomingPosts(
                    influencerEmail = influencerEmail,
                    incomingPosts = posts
                )

                when (handleResult) {
                    is Result.Error -> Result.Error(Error.Unknown)
                    is Result.Success -> Result.Success(Unit)
                }
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
                    return Result.Error(mediaResult.toError(logger))
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
        sealed class Instagram: Error() {
            object PermissionError: Instagram()
        }

        object Unknown: Error()
    }
}

internal fun IgServiceResult<Any>.toError(logger: KLogger): IgHandlingService.Error =
    when (this) {
        is IgServiceResult.Unknown -> IgHandlingService.Error.Unknown
        is IgServiceResult.PermissionError -> IgHandlingService.Error.Instagram.PermissionError
        is IgServiceResult.Success -> {
            logger.error { "IgService Result should not be converted to error in Connect Ig Service" }
            IgHandlingService.Error.Unknown
        }
    }