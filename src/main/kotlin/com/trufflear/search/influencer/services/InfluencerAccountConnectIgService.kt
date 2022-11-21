package com.trufflear.search.influencer.services

import com.trufflear.search.config.*
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.clientId
import com.trufflear.search.config.redirectUri
import com.trufflear.search.influencer.*
import com.trufflear.search.influencer.database.tables.InfluencerTable
import com.trufflear.search.influencer.database.tables.PostTable
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.igDateFormat
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

class InfluencerAccountConnectIgService (
    private val dataSource: DataSource,
    private val igAuthService: IgAuthService,
    private val igGraphService: IgGraphService,
    private val captionParser: CaptionParser,
    private val influencerProfileRepository: InfluencerProfileRepository,
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
        influencerProfileRepository.checkIfInfluencerExists(influencer.email)
            ?: throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))


        val result = igService.getShortLivedToken(
            clientId = clientId,
            clientSecret = appSecret,
            grantType = IgCodeGrantType.authCodeGrantType,
            redirectUri = redirectUri,
            code = request.instagramAuthCode
        )

        when (result) {
            is IgServiceResult.PermissionError -> throw StatusException(Status.PERMISSION_DENIED)
            is IgServiceResult.ExpiredError -> throw StatusException(Status.INVALID_ARGUMENT)
            is IgServiceResult.Unknown -> throw StatusException(Status.UNKNOWN)
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

    private suspend fun fetchAndStoreUserInfoAndToken(
        accessToken: String,
        instagramUserId: String,
        influencerEmail: String
    ) {
        logger.info ("getting long lived access token for user: $influencerEmail")
        val longLivedTokenResponse = igGraphService.getLongLivedAccessToken(
            clientSecret = appSecret,
            grantType = IgCodeGrantType.exchangeTokenType,
            accessToken = accessToken
        )

        logger.info ("getting user info for $influencerEmail")
        val igUserInfo = igGraphService.getUser(
            userId = instagramUserId,
            fields = getUserInfoFieldsString(),
            accessToken = accessToken
        )

        transaction(Database.connect(dataSource)) {
            addLogger(StdOutSqlLogger)

            InfluencerTable.update({ InfluencerTable.email eq influencerEmail}) {
                it[igUserId] = instagramUserId
                it[igLongLivedAccessToken] = longLivedTokenResponse.accessToken
                it[igLongLivedAccessTokenExpiresIn] = longLivedTokenResponse.expiresIn.toLongOrNull() ?: 0
                it[igMediaCount] = igUserInfo.mediaCount
                it[igAccountType] = igUserInfo.accountType
            }

            InfluencerTable.update({ InfluencerTable.email eq influencerEmail and (InfluencerTable.username eq "") }) {
                it[username] = igUserInfo.userName
                it[profileTitle] = igUserInfo.userName
            }
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

        transaction(Database.connect(dataSource)) {
            addLogger(StdOutSqlLogger)

            val existingPostIgIdSet = PostTable.slice(PostTable.igId)
                .select { PostTable.influencerEmail eq influencerEmail }
                .map { it[PostTable.igId] }
                .toHashSet()

            val incomingPostsRemovingNewLine = igPosts.map {
                it.copy(
                    caption = it.caption?.replace("\n", " ")
                )
            }

            deleteOldPosts(existingPostIgIdSet.minus(incomingPostsRemovingNewLine.map { it.id }.toHashSet()))
            insertNewPosts(
                newPosts = incomingPostsRemovingNewLine
                    .filter { existingPostIgIdSet.contains(it.id).not() },
                influencerEmail = influencerEmail
            )

            updatePosts(incomingPostsRemovingNewLine.filter { existingPostIgIdSet.contains(it.id) })
        }
    }

    private suspend fun getAllUserPosts(
        instagramUserId: String,
        accessToken: String,
    ): List<IgPost> {
        val igPosts = mutableListOf<IgPost>()

        var afterToken: String? = null
        var nextLink: String?
        do {
            val igUserMedia = igGraphService.getUserMedia(
                userId = instagramUserId,
                limit = fetchingLimit,
                fields = getUserMediaFieldsString(),
                accessToken = accessToken,
                after = afterToken
            )
            igPosts.addAll(igUserMedia.data)

            nextLink = igUserMedia.paging?.next
            afterToken = igUserMedia.paging?.cursors?.after

        } while (nextLink != null && afterToken != null)

        return igPosts
    }

    private fun deleteOldPosts(oldPostIgIds: Set<String>) {
        PostTable.deleteWhere { igId inList oldPostIgIds }
    }

    private fun updatePosts(postsToUpdate: List<IgPost>) {
        postsToUpdate.forEach { post ->
            PostTable.update({ PostTable.igId eq post.id }) {
                post.caption?.let { cap ->
                    it[caption] = cap
                    it[hashtags] = captionParser.getHashTags(cap)
                    it[mentions] = captionParser.getMentions(cap)
                }
                it[permalink] = post.permalink
            }
        }
    }

    private fun insertNewPosts(
        newPosts: List<IgPost>,
        influencerEmail: String
    ){
        PostTable.batchInsert(newPosts) { post ->
            this[PostTable.igId] = post.id
            this[PostTable.influencerEmail] = influencerEmail
            this[PostTable.username] = post.username
            post.caption?.let {
                this[PostTable.caption] = it
                this[PostTable.hashtags] = captionParser.getHashTags(it)
                this[PostTable.mentions] = captionParser.getMentions(it)
            }
            this[PostTable.permalink] = post.permalink
            this[PostTable.mediaType] = post.mediaType
            this[PostTable.mediaUrl] = post.mediaUrl
            this[PostTable.thumbnailUrl] = getCorrectThumbnailUrl(
                thumbnailUrl = post.thumbnailUrl,
                mediaUrl = post.mediaUrl,
                mediaType = post.mediaType
            )
            this[PostTable.createdAtTimestamp] = convertIgTimeToInstant(post.timestamp)
        }
    }

    private fun convertIgTimeToInstant(dateTimeString: String): Instant {
        val createdAtTime = igDateFormat.parse(dateTimeString).time
        return Timestamp(createdAtTime).toInstant()
    }

    private fun getCorrectThumbnailUrl(
        thumbnailUrl: String?,
        mediaUrl: String,
        mediaType: String
    ): String =
        if (mediaType == IgMediaType.IMAGE.name || mediaType == IgMediaType.CAROUSEL_ALBUM.name) {
            mediaUrl
        } else if (mediaType == IgMediaType.VIDEO.name && thumbnailUrl != null) {
            thumbnailUrl
        } else { "" }


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