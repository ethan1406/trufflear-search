package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.*
import com.trufflear.search.influencer.database.models.InfluencerDbDto
import com.trufflear.search.influencer.domain.InfluencerPublicProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class InfluencerPublicProfileService(
    private val dataSource: DataSource
): InfluencerPublicProfileServiceGrpcKt.InfluencerPublicProfileServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getInfluencerPublicProfile(request: GetInfluencerPublicProfileRequest): GetInfluencerPublicProfileResponse {
        logger.debug { "getting public profile for ${request.username}" }

        val publicProfile = withContext(Dispatchers.IO) {
            runCatching {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerDbDto
                        .slice(
                            InfluencerDbDto.profileTitle, InfluencerDbDto.categoryTitle,
                            InfluencerDbDto.bioDescription, InfluencerDbDto.profileImageUrl,
                            InfluencerDbDto.isProfileLive
                        )
                        .select { InfluencerDbDto.username eq request.username }
                        .map {
                            InfluencerPublicProfile(
                                profilePicUrl = it[InfluencerDbDto.profileImageUrl],
                                profileTitle = it[InfluencerDbDto.profileTitle],
                                professionCategory = it[InfluencerDbDto.categoryTitle],
                                bioDescription = it[InfluencerDbDto.bioDescription],
                                isProfileLive = it[InfluencerDbDto.isProfileLive]
                            )
                        }.firstOrNull()
                }
            }
        }

        val profile = publicProfile.getOrElse {
            logger.error(it) { "error getting influencer profile info" }
            return getInfluencerPublicProfileResponse {
                error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                    .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.UNKNOWN)
                    .build()
            }
        } ?: return getInfluencerPublicProfileResponse {
            logger.error { "influencer profile does not exist" }
            error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_DOES_NOT_EXIST)
                .build()
        }

        return if (profile.isProfileLive) {
            getInfluencerPublicProfileResponse {
                success = GetInfluencerPublicProfileResponse.Success.newBuilder()
                    .setProfile(
                        influencerProfile {
                            profilePicUrl = profile.profilePicUrl
                            profileTitle = profile.profileTitle
                            categoryTitle = profile.professionCategory
                            bioDescription = profile.bioDescription
                        }
                    ).build()
            }
        } else {
            logger.debug { "influencer profile not live" }
            getInfluencerPublicProfileResponse {
                error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                    .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_NOT_LIVE)
                    .build()
            }
        }
    }

}