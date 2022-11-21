package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.*
import com.trufflear.search.influencer.database.tables.InfluencerTable
import com.trufflear.search.influencer.network.service.SearchIndexService
import com.trufflear.search.influencer.services.util.checkIfUserExists
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
    private val dataSource: DataSource,
    private val searchIndexService: SearchIndexService
) : InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun signup(request: SignupRequest): SignupResponse {
        logger.debug { "user signing up" }
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.insert {
                        it[name] = influencer.name
                        it[email] = influencer.email
                        it[isEmailVerified] = influencer.emailVerified
                    }
                }

                searchIndexService.createSearchCollectionForInfluencer(influencer.email)
            } catch (e: ExposedSQLException) {
                logger.error(e) { "error creating user for ${influencer.email}" }

                if (e.sqlState == "23505" || e.sqlState == "23000") {
                    logger.error("user already exists")
                    throw StatusException(Status.ALREADY_EXISTS)
                }

                throw StatusException(Status.UNKNOWN)
            } catch (e: Exception) {
                logger.error(e) { "error creating for ${influencer.email}" }
                throw StatusException(Status.UNKNOWN)
            }
        }

        return signupResponse { }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): UpdateProfileRequestResponse {
        logger.debug { "user updating profile" }

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            logger.info { "checking if user exists" }
            checkIfUserExists(dataSource, influencer)
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.update({ InfluencerTable.email eq influencer.email}) {
                        it[profileTitle] = request.profileTitle
                        it[categoryTitle] = request.professionCategory
                        it[bioDescription] = request.bioDescription
                    }
                }
            } catch (e: ExposedSQLException) {
                logger.error(e) { "error updating bio description" }
                throw StatusException(Status.INTERNAL)
            }
        }

        return updateProfileRequestResponse { }
    }
}