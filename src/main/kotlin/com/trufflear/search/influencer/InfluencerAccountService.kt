package com.trufflear.search.influencer

import com.trufflear.search.influencer.database.models.InfluencerDbDto
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

internal class InfluencerAccountService(
    private val dataSource: DataSource
) : InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun signup(request: SignupRequest): SignupResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerDbDto.insert {
                        it[name] = influencer.name
                        it[email] = influencer.email
                        it[isEmailVerified] = influencer.emailVerified
                    }
                }
            } catch (e: ExposedSQLException) {
                logger.error(e) { "error creating user" }

                if (e.sqlState == "23505") {
                    logger.error("user already exists")
                    throw StatusException(Status.ALREADY_EXISTS)
                }

                throw StatusException(Status.INTERNAL)
            }
        }

        return signupResponse { }
    }

    override suspend fun updateBioDescription(request: UpdateBioDescriptionRequest): UpdateBioDescriptionResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                        it[bioDescription] = request.bioDescription
                    }
                }
            } catch (e: ExposedSQLException) {
                logger.error(e) { "error updating bio description" }
                throw StatusException(Status.INTERNAL)
            }
        }

        return updateBioDescriptionResponse { }
    }

    override suspend fun updateUsername(request: UpdateUsernameRequest): UpdateUsernameResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                        it[username] = request.username
                    }
                }
            } catch (e: ExposedSQLException) {
                println("error updating bio description: ${e.message}")

                throw StatusException(Status.INTERNAL)
            }
        }

        return updateUsernameResponse { }
    }
}