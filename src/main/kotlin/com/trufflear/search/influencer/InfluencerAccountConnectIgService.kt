package com.trufflear.search.influencer

import com.trufflear.search.config.*
import com.trufflear.search.config.IgCodeGrantType
import com.trufflear.search.config.IgMediaFields
import com.trufflear.search.config.appSecret
import com.trufflear.search.config.clientId
import com.trufflear.search.config.redirectUri
import com.trufflear.search.influencer.database.models.InfluencerDbDto
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.lang.Exception
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

class InfluencerAccountConnectIgService (
    private val dataSource: DataSource,
    private val igAuthService: IgAuthService,
    private val igGraphService: IgGraphService
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

                println(igUserMedia)

                transaction(Database.connect(dataSource)) {
                    println("successfully connected to database for connecting instagram user media")

                    InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                        it[igUserId] = shortLivedTokenResponse.userId
                    }
                }

            } catch (e: Exception) {
                println(e)
            }
        }

        return connectIgUserMediaResponse { }
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
}