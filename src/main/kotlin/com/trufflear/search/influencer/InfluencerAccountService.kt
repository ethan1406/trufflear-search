package com.trufflear.search.influencer

import com.trufflear.search.configs.*
import com.trufflear.search.configs.InstagramApiParams
import com.trufflear.search.configs.apiInstagramSubdomain
import com.trufflear.search.configs.authPath
import com.trufflear.search.configs.clientId
import com.trufflear.search.configs.https_host
import com.trufflear.search.influencer.database.models.InfluencerDbDto
import io.grpc.Status
import io.grpc.StatusException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

internal class InfluencerAccountService(
    private val dataSource: DataSource
) : InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineImplBase() {

    override suspend fun signup(request: SignupRequest): SignupResponse {

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        try {
            transaction(Database.connect(dataSource)) {
                println("successfully connected to database for signup")

                InfluencerDbDto.insert {
                    it[name] = influencer.name
                    it[email] = influencer.email
                    it[isEmailVerified] = influencer.emailVerified
                    it[bioDescription] = ""
                    it[username] = ""
                    it[profileImageUrl] = ""
                }
            }
        } catch (e: ExposedSQLException) {
            println("error creating user: ${e.message}")

            if (e.sqlState == "23505") {
                println("user already exists")
                throw StatusException(Status.INVALID_ARGUMENT.withDescription("user already exists"))
            }

            throw StatusException(Status.INTERNAL)
        }

        return signupResponse { }
    }

    override suspend fun updateBioDescription(request: UpdateBioDescriptionRequest): UpdateBioDescriptionResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        try {
            transaction(Database.connect(dataSource)) {
                println("successfully connected to database for signup")

                InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                    it[bioDescription] = request.bioDescription
                }
            }
        } catch (e: ExposedSQLException) {
            println("error updating bio description: ${e.message}")
            throw StatusException(Status.INTERNAL)
        }

        return updateBioDescriptionResponse { }
    }

    override suspend fun updateUsername(request: UpdateUsernameRequest): UpdateUsernameResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)
        try {
            transaction(Database.connect(dataSource)) {
                println("successfully connected to database for signup")

                InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                    it[username] = request.username
                }
            }
        } catch (e: ExposedSQLException) {
            println("error updating bio description: ${e.message}")

            throw StatusException(Status.INTERNAL)
        }

        return updateUsernameResponse { }
    }

    override suspend fun getInstagramConnectUrl(request: GetInstagramConnectUrlRequest): GetInstagramConnectUrlResponse {
        return getInstagramConnectUrlResponse {
            url = "$https_host://$apiInstagramSubdomain/$authPath?${InstagramApiParams.clientId}=$clientId&" +
                    "${InstagramApiParams.redirectUri}=$redirectUrl&${InstagramApiParams.responseType}=${InstagramResponseTypeFields.code}&" +
                    "${InstagramApiParams.scope}=${InstagramAuthScopeFields.userProfile},${InstagramAuthScopeFields.userMedia}"
        }
    }

    override suspend fun fetchInstagramUserMedia(request: FetchInstagramUserMediaRequest): FetchInstagramUserMediaResponse {
        return fetchInstagramUserMediaResponse {

        }
    }
}