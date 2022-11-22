package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.influencer.GetInfluencerPublicProfileResponse
import com.trufflear.search.influencer.domain.InfluencerPublicProfile
import com.trufflear.search.influencer.getInfluencerPublicProfileRequest
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.repositories.ProfileRequest
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.ProfileResult
import com.trufflear.search.influencer.services.InfluencerPublicProfileService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private val testUsername = "cooking_bobo"

private val getProfileRequest = ProfileRequest.WithUsername(username = testUsername)

private val profile = InfluencerPublicProfile(
    profilePicUrl = "",
    profileTitle = testUsername,
    professionCategory = "",
    bioDescription = "",
    isProfileLive = true
)

class InfluencerPublicProfileServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository>()

    private val service = InfluencerPublicProfileService(influencerProfileRepository)

    @Test
    fun `get influencer public profile should return profile does not exist error when username not found`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.NotFound)

            val request = getInfluencerPublicProfileRequest {
                username = testUsername
            }

            // ACT
            val response = service.getInfluencerPublicProfile(request)

            // ASSERT
            assertThat(response.error.errorType).isEqualTo(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_DOES_NOT_EXIST)
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
        }

    @Test
    fun `get influencer public profile should return profile not live error when repository returns a profile not live`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.Success(profile.copy(isProfileLive = false)))

            val request = getInfluencerPublicProfileRequest {
                username = testUsername
            }

            // ACT
            val response = service.getInfluencerPublicProfile(request)

            // ASSERT
            assertThat(response.error.errorType).isEqualTo(GetInfluencerPublicProfileResponse.Error.ErrorType.PROFILE_NOT_LIVE)
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
        }

    @Test
    fun `get influencer public profile should return profile`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.Success(profile))

            val request = getInfluencerPublicProfileRequest {
                username = testUsername
            }

            // ACT
            val response = service.getInfluencerPublicProfile(request)

            // ASSERT
            assertThat(response.success.profile).isEqualTo(
                influencerProfile {
                    profilePicUrl = profile.profilePicUrl
                    profileTitle = profile.profileTitle
                    categoryTitle = profile.professionCategory
                    bioDescription = profile.bioDescription
                }
            )
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
        }

}