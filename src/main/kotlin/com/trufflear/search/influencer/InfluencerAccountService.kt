package com.trufflear.search.influencer

import com.trufflear.search.influencer.database.models.InfluencerDbDto
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        withContext(Dispatchers.IO) {
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
        }

        return signupResponse { }
    }

    override suspend fun updateBioDescription(request: UpdateBioDescriptionRequest): UpdateBioDescriptionResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    println("successfully connected to database to update bio description")

                    InfluencerDbDto.update({ InfluencerDbDto.email eq influencer.email}) {
                        it[bioDescription] = request.bioDescription
                    }
                }
            } catch (e: ExposedSQLException) {
                println("error updating bio description: ${e.message}")
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
                    println("successfully connected to database to update username")

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