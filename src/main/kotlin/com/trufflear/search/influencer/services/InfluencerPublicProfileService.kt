package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.GetInfluencerPublicProfileRequest
import com.trufflear.search.influencer.GetInfluencerPublicProfileResponse
import com.trufflear.search.influencer.InfluencerPublicProfileServiceGrpcKt
import com.trufflear.search.influencer.getInfluencerPublicProfileResponse
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.ProfileResult
import mu.KotlinLogging

class InfluencerPublicProfileService(
    private val repository: InfluencerProfileRepository
): InfluencerPublicProfileServiceGrpcKt.InfluencerPublicProfileServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun getInfluencerPublicProfile(request: GetInfluencerPublicProfileRequest): GetInfluencerPublicProfileResponse {
        logger.debug { "getting public profile for ${request.username}" }

        return when (val result = repository.getPublicProfile(request.username)) {
            is ProfileResult.Unknown ->
                getInfluencerPublicProfileResponse {
                    error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                        .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.UNKNOWN)
                        .build()
                }
            is ProfileResult.NotFound ->
                getInfluencerPublicProfileResponse {
                    error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                        .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_DOES_NOT_EXIST)
                        .build()
                }
            is ProfileResult.Success ->
                if (result.profile.isProfileLive.not()) {
                    getInfluencerPublicProfileResponse {
                        error = GetInfluencerPublicProfileResponse.Error.newBuilder()
                            .setErrorType(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_NOT_LIVE)
                            .build()
                    }
                } else {
                    getInfluencerPublicProfileResponse {
                        success = GetInfluencerPublicProfileResponse.Success.newBuilder()
                            .setProfile(
                                influencerProfile {
                                    profilePicUrl = result.profile.profilePicUrl
                                    profileTitle = result.profile.profileTitle
                                    categoryTitle = result.profile.professionCategory
                                    bioDescription = result.profile.bioDescription
                                }
                            ).build()
                    }
                }
        }
    }

}