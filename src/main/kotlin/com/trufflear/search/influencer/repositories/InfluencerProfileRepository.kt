package com.trufflear.search.influencer.repositories

import com.trufflear.search.influencer.database.tables.InfluencerTable
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.domain.InfluencerProfile
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
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

    internal suspend fun getPublicProfile(request: ProfileRequest): ProfileResult =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    val influencer = InfluencerTable
                        .slice(
                            InfluencerTable.profileTitle, InfluencerTable.categoryTitle,
                            InfluencerTable.bioDescription, InfluencerTable.profileImageObjectKey,
                            InfluencerTable.isProfileLive, InfluencerTable.username
                        )
                        .select {
                            when (request) {
                                is ProfileRequest.WithEmail ->
                                    InfluencerTable.email eq request.email
                                is ProfileRequest.WithUsername ->
                                    InfluencerTable.username eq request.username
                            }
                        }
                        .map {
                            InfluencerProfile(
                                username = it[InfluencerTable.username],
                                profilePicObjectKey = it[InfluencerTable.profileImageObjectKey],
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
                logger.error(e) { "error getting profile for ${request.id}" }
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

    internal suspend fun saveProfileImageKey(username: String, imageKey: String): CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.update({ InfluencerTable.username eq username}) {
                        it[profileImageObjectKey] = imageKey
                    }

                    CallSuccess
                }
            } catch (e: Exception) {
                logger.error(e) { "error saving profile image object key for $username" }
                null
            }
        }

    internal suspend fun userExists(email: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    val userNotCreated = InfluencerTable.select { InfluencerTable.email eq email }.empty()
                    userNotCreated.not()
                }
            } catch (e: Exception) {
                logger.error(e) { "error checking if influencer exists for $email" }
                false
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
    ): CallSuccess? =
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
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error upserting influencer Ig info" }
                null
            }
        }


    internal suspend fun setProfileLive(
        setLive: Boolean,
        influencerEmail: String
    ): CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable.update({ InfluencerTable.email eq influencerEmail}) {
                        it[isProfileLive] = setLive
                    }
                }
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error setting live to $setLive for influencer profile $influencerEmail" }
                null
            }
        }
}

sealed class ProfileRequest(open val id: String) {
    data class WithEmail(
        val email: String
    ): ProfileRequest(email)

    data class WithUsername(
        val username: String
    ): ProfileRequest(username)
}

sealed class ProfileResult {
    data class Success(
        val profile: InfluencerProfile
    ): ProfileResult()

    object NotFound: ProfileResult()

    object Unknown: ProfileResult()
}

sealed class InsertResult {
    object Success: InsertResult()

    object UserAlreadyExists: InsertResult()

    object Unknown: InsertResult()
}
