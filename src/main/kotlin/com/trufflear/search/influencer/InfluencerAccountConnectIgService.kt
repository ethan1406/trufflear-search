package com.trufflear.search.influencer

import com.trufflear.search.config.*
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.clientId
import com.trufflear.search.config.redirectUri
import com.trufflear.search.influencer.database.models.InfluencerDbDto
import com.trufflear.search.influencer.database.models.PostDbDto
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.batchUpsert
import com.trufflear.search.influencer.util.igDateFormat
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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

    override suspend fun getIgConnectUrl(request: GetIgConnectUrlRequest): GetIgConnectUrlResponse =
        getIgConnectUrlResponse {
            url = "$igApiSubdomainBaseUrl$authPath?${IgApiParams.clientId}=$clientId&" +
                    "${IgApiParams.redirectUri}=$redirectUri&${IgApiParams.responseType}=${IgResponseTypeFields.code}&" +
                    "${IgApiParams.scope}=${IgAuthScopeFields.userProfile},${IgAuthScopeFields.userMedia}"
        }

    override suspend fun connectIgUserMedia(request: ConnectIgUserMediaRequest): ConnectIgUserMediaResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            println("checking if user exists")
            checkIfUserExist(dataSource, influencer)
            try {
                val shortLivedTokenResponse = igAuthService.getShortLivedToken(
                    clientId = clientId,
                    clientSecret = appSecret,
                    grantType = IgCodeGrantType.authCodeGrantType,
                    redirectUri = redirectUri,
                    code = request.instagramAuthCode
                )

                val longLivedTokenResponse = igGraphService.getLongLivedAccessToken(
                    clientSecret = appSecret,
                    grantType = IgCodeGrantType.exchangeTokenType,
                    accessToken = shortLivedTokenResponse.accessToken
                )

                val igUserMedia = igGraphService.getUserMedia(
                    userId = shortLivedTokenResponse.userId,
                    fields = getUserMediaFieldsString(),
                    accessToken = shortLivedTokenResponse.accessToken
                )

                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                        it[igUserId] = shortLivedTokenResponse.userId
                        it[igLongLivedAccessToken] = longLivedTokenResponse.accessToken
                        it[igLongLivedAccessTokenExpiresIn] = longLivedTokenResponse.expiresIn.toLongOrNull() ?: 0
                    }

                    PostDbDto.batchUpsert(igUserMedia.data) { table, post ->
                        table[igId] = post.id
                        table[influencerEmail] = ""
                        table[username] = post.username
                        table[caption] = post.caption
                        table[permalink] = post.permalink
                        table[caption] = post.caption
                        table[hashtags] = captionParser.getHashTags(post.caption)
                        table[mentions] = captionParser.getMentions(post.caption)
                        table[mediaType] = post.mediaType
                        table[mediaUrl] = post.mediaUrl
                        table[thumbnailUrl] = getCorrectThumbnailUrl(
                            thumbnailUrl = post.thumbnailUrl,
                            mediaUrl = post.mediaUrl,
                            mediaType = post.mediaType
                        )
                        table[createdAtDateTime] = convertIgTimeToInstant(post.timestamp)
                    }
                }
            } catch (e: ExposedSQLException) {
                println("error with database transactions: $e")
                throw StatusException(Status.INTERNAL)
            } catch (e: Exception) {
                println("error with API calls to Instagram: $e")
                throw StatusException(Status.INTERNAL)
            }
        }

        return connectIgUserMediaResponse { }
    }


    private fun checkIfUserExist(dataSource: DataSource, influencer: Influencer) {
        transaction(Database.connect(dataSource)) {
            val userNotCreated = InfluencerDbDto.select { InfluencerDbDto.email eq influencer.email }.empty()
            if (userNotCreated) {
                println("user doesn't exist")
                throw StatusException(Status.UNAUTHENTICATED)
            }
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
}