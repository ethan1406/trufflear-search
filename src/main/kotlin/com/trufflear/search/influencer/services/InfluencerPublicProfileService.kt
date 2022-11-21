package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.GetInfluencerPublicProfileRequest
import com.trufflear.search.influencer.GetInfluencerPublicProfileResponse
import com.trufflear.search.influencer.InfluencerPublicProfileServiceGrpcKt
import com.trufflear.search.influencer.getInfluencerPublicProfileResponse
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import mu.KotlinLogging

class InfluencerPublicProfileService(
    private val repository: InfluencerProfileRepository
): InfluencerPublicProfileServiceGrpcKt.InfluencerPublicProfileServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getInfluencerPublicProfile(request: GetInfluencerPublicProfileRequest): GetInfluencerPublicProfileResponse {
        logger.debug { "getting public profile for ${request.username}" }

        val publicProfile = repository.getPublicProfile(request.username)

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