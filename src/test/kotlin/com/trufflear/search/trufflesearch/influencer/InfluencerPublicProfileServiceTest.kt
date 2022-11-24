package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.influencer.GetInfluencerPublicProfileResponse
import com.trufflear.search.influencer.domain.InfluencerProfile
import com.trufflear.search.influencer.getInfluencerPublicProfileRequest
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.network.service.ImageService
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

private val profile = InfluencerProfile(
    profilePicObjectKey = "",
    profileTitle = testUsername,
    professionCategory = "",
    bioDescription = "",
    isProfileLive = true,
    username = testUsername
)

class InfluencerPublicProfileServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository>()
    private val imageService = mock<ImageService>()

    private val service = InfluencerPublicProfileService(influencerProfileRepository, imageService)

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
    fun `get influencer public profile should return profile with empty profile url when image service returns null`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.Success(profile))

            whenever(imageService.getPresignedUrl(profile.profilePicObjectKey))
                .thenReturn(null)

            val request = getInfluencerPublicProfileRequest {
                username = testUsername
            }

            // ACT
            val response = service.getInfluencerPublicProfile(request)

            // ASSERT
            assertThat(response.success.profile).isEqualTo(
                influencerProfile {
                    profilePicUrl = ""
                    profileTitle = profile.profileTitle
                    categoryTitle = profile.professionCategory
                    bioDescription = profile.bioDescription
                }
            )
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
            verify(imageService).getPresignedUrl(profile.profilePicObjectKey)
        }

    @Test
    fun `get influencer public profile should return profile`() =
        runBlocking<Unit> {
            // ARRANGE
            val presignedUrl = "this is a presigned url"
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.Success(profile))

            whenever(imageService.getPresignedUrl(profile.profilePicObjectKey))
                .thenReturn(presignedUrl)

            val request = getInfluencerPublicProfileRequest {
                username = testUsername
            }

            // ACT
            val response = service.getInfluencerPublicProfile(request)

            // ASSERT
            assertThat(response.success.profile).isEqualTo(
                influencerProfile {
                    profilePicUrl = presignedUrl
                    profileTitle = profile.profileTitle
                    categoryTitle = profile.professionCategory
                    bioDescription = profile.bioDescription
                }
            )
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
            verify(imageService).getPresignedUrl(profile.profilePicObjectKey)
        }
}