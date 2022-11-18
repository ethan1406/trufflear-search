package com.trufflear.search.influencer.services

import com.trufflear.search.config.*
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.clientId
import com.trufflear.search.config.redirectUri
import com.trufflear.search.influencer.*
import com.trufflear.search.influencer.database.models.InfluencerDbDto
import com.trufflear.search.influencer.database.models.PostDbDto
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import com.trufflear.search.influencer.services.util.checkIfUserExists
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
import retrofit2.HttpException
import java.lang.Exception
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

class InfluencerAccountConnectIgService (
    private val dataSource: DataSource,
    private val igAuthService: IgAuthService,
    private val igGraphService: IgGraphService,
    private val captionParser: CaptionParser
) : InfluencerAccountConnectIgServiceGrpcKt.InfluencerAccountConnectIgServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getIgConnectUrl(request: GetIgConnectUrlRequest): GetIgConnectUrlResponse =
        getIgConnectUrlResponse {
            url = "$igApiSubdomainBaseUrl$authPath?${IgApiParams.clientId}=$clientId&" +
                    "${IgApiParams.redirectUri}=$redirectUri&${IgApiParams.responseType}=${IgResponseTypeFields.code}&" +
                    "${IgApiParams.scope}=${IgAuthScopeFields.userProfile},${IgAuthScopeFields.userMedia}"
        }

    override suspend fun connectIgUserMedia(request: ConnectIgUserMediaRequest): ConnectIgUserMediaResponse {
        logger.info ("connecting instagram for user")

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            logger.info { "checking if user exists" }
            checkIfUserExists(dataSource, influencer)

            try {
                val shortLivedTokenResponse = igAuthService.getShortLivedToken(
                    clientId = clientId,
                    clientSecret = appSecret,
                    grantType = IgCodeGrantType.authCodeGrantType,
                    redirectUri = redirectUri,
                    code = request.instagramAuthCode
                )

                println("asdhfasd: ${shortLivedTokenResponse.accessToken}")

                listOf(
                    launch {
                        fetchAndStoreUserInfoAndToken(
                            accessToken = shortLivedTokenResponse.accessToken,
                            instagramUserId = shortLivedTokenResponse.userId,
                            influencerEmail = influencer.email
                        )
                    },
                    launch {
                        fetchAndStoreUserPosts(
                            accessToken = shortLivedTokenResponse.accessToken,
                            instagramUserId = shortLivedTokenResponse.userId,
                            influencerEmail = influencer.email
                        )
                    }
                ).joinAll()

            } catch (e: ExposedSQLException) {
                logger.error(e) { "error with database transaction" }
                throw StatusException(Status.UNKNOWN)
            } catch (e: HttpException) {
                logger.error(e) { "error with API calls to Instagram" }
                if (e.code() == 403) {
                    throw StatusException(Status.PERMISSION_DENIED)
                } else {
                    throw StatusException(Status.INVALID_ARGUMENT)
                }
            } catch (e: Exception) {
                logger.error(e) { "unknown error while connecting instagram" }
                throw StatusException(Status.UNKNOWN)
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

            InfluencerDbDto.update({ InfluencerDbDto.email eq influencerEmail}) {
                it[igUserId] = instagramUserId
                it[igLongLivedAccessToken] = longLivedTokenResponse.accessToken
                it[igLongLivedAccessTokenExpiresIn] = longLivedTokenResponse.expiresIn.toLongOrNull() ?: 0
                it[igMediaCount] = igUserInfo.mediaCount
                it[igAccountType] = igUserInfo.accountType
            }

            InfluencerDbDto.update({ InfluencerDbDto.email eq influencerEmail and (InfluencerDbDto.username eq "") }) {
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

            val existingPostIgIdSet = PostDbDto.slice(PostDbDto.igId)
                .select { PostDbDto.influencerEmail eq influencerEmail }
                .map { it[PostDbDto.igId] }
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
        PostDbDto.deleteWhere { igId inList oldPostIgIds }
    }

    private fun updatePosts(postsToUpdate: List<IgPost>) {
        postsToUpdate.forEach { post ->
            PostDbDto.update({ PostDbDto.igId eq post.id }) {
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
        PostDbDto.batchInsert(newPosts) { post ->
            this[PostDbDto.igId] = post.id
            this[PostDbDto.influencerEmail] = influencerEmail
            this[PostDbDto.username] = post.username
            post.caption?.let {
                this[PostDbDto.caption] = it
                this[PostDbDto.hashtags] = captionParser.getHashTags(it)
                this[PostDbDto.mentions] = captionParser.getMentions(it)
            }
            this[PostDbDto.permalink] = post.permalink
            this[PostDbDto.mediaType] = post.mediaType
            this[PostDbDto.mediaUrl] = post.mediaUrl
            this[PostDbDto.thumbnailUrl] = getCorrectThumbnailUrl(
                thumbnailUrl = post.thumbnailUrl,
                mediaUrl = post.mediaUrl,
                mediaType = post.mediaType
            )
            this[PostDbDto.createdAtTimestamp] = convertIgTimeToInstant(post.timestamp)
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