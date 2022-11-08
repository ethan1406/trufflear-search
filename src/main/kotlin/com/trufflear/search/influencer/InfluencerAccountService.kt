package com.trufflear.search.influencer

import com.trufflear.search.influencer.database.models.InfluencerDbDto
import io.grpc.Status
import io.grpc.StatusException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
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
                    it[title] = ""
                    it[profileImageUrl] = ""
                }
            }
        } catch (e: ExposedSQLException) {
            println("error creating user: ${e.message}")

            if (e.sqlState == "23505") {
                println("user already exists")
            }

            throw StatusException(Status.INVALID_ARGUMENT.withDescription("user already exists"))
        }

        return signupResponse { }
    }
}