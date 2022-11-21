package com.trufflear.search.influencer.repositories

import com.trufflear.search.influencer.database.tables.InfluencerTable
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.domain.InfluencerPublicProfile
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import javax.sql.DataSource

class InfluencerProfileRepository(
    private val dataSource: DataSource
) {
    private val logger = KotlinLogging.logger {}

    internal suspend fun getPublicProfile(username: String): ProfileResult =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    val influencer = InfluencerTable
                        .slice(
                            InfluencerTable.profileTitle, InfluencerTable.categoryTitle,
                            InfluencerTable.bioDescription, InfluencerTable.profileImageUrl,
                            InfluencerTable.isProfileLive
                        )
                        .select { InfluencerTable.username eq username }
                        .map {
                            InfluencerPublicProfile(
                                profilePicUrl = it[InfluencerTable.profileImageUrl],
                                profileTitle = it[InfluencerTable.profileTitle],
                                professionCategory = it[InfluencerTable.categoryTitle],
                                bioDescription = it[InfluencerTable.bioDescription],
                                isProfileLive = it[InfluencerTable.isProfileLive]
                            )
                        }.firstOrNull()

                    influencer?.let {
                        ProfileResult.Success(it)
                    } ?: ProfileResult.NotFound
                }
            } catch (e: Exception) {
                logger.error(e) { "error getting profile for $username" }
                ProfileResult.Unknown
            }
        }

    internal suspend fun insertInfluencer(influencer: Influencer): InsertResult =
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

                InsertResult.Success
            } catch (e: ExposedSQLException) {
                logger.error(e) { "error creating user for ${influencer.email}" }

                if (e.sqlState == "23505" || e.sqlState == "23000") {
                    logger.error("user already exists")
                    InsertResult.UserAlreadyExists
                } else {
                    InsertResult.Unknown
                }
            }
        }

    internal suspend fun checkIfInfluencerExists(email: String): Unit? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    val userNotCreated = InfluencerTable.select { InfluencerTable.email eq email }.empty()
                    if (userNotCreated) {
                        null
                    } else {
                        Unit
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "error checking if influencer exists for $email" }
                null
            }
        }

    internal suspend fun updateInfluencerProfile(
        influencerEmail: String,
        title: String,
        professionCategory: String,
        description: String
    ): CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.update({ InfluencerTable.email eq influencerEmail}) {
                        it[profileTitle] = title
                        it[categoryTitle] = professionCategory
                        it[bioDescription] = description
                    }
                }
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error updating influencer profile" }
                null
            }
        }

    internal suspend fun upsertInfluencerIgInfo(
        tokenResponse: IgLongLivedTokenResponse,
        igUser: IgUserInfo,
        influencerEmail: String,
        instagramUserId: String
    ): Unit? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.update({ InfluencerTable.email eq influencerEmail}) {
                        it[igUserId] = instagramUserId
                        it[igLongLivedAccessToken] = tokenResponse.accessToken
                        it[igLongLivedAccessTokenExpiresIn] = tokenResponse.expiresIn.toLongOrNull() ?: 0
                        it[igMediaCount] = igUser.mediaCount
                        it[igAccountType] = igUser.accountType
                    }

                    InfluencerTable.update({ InfluencerTable.email eq influencerEmail and (InfluencerTable.username eq "") }) {
                        it[username] = igUser.userName
                        it[profileTitle] = igUser.userName
                    }
                }
                Unit
            } catch (e: Exception) {
                logger.error(e) { "error upserting influencer Ig info" }
                null
            }
        }


}

sealed class ProfileResult {
    data class Success(
        val profile: InfluencerPublicProfile
    ): ProfileResult()

    object NotFound: ProfileResult()

    object Unknown: ProfileResult()
}

sealed class InsertResult {
    object Success: InsertResult()

    object UserAlreadyExists: InsertResult()

    object Unknown: InsertResult()
}
