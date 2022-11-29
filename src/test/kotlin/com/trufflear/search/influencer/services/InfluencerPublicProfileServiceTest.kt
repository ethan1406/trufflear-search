package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.GetInfluencerPublicProfileResponse
import com.trufflear.search.influencer.domain.InfluencerProfile
import com.trufflear.search.influencer.getInfluencerPublicProfileRequest
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.repositories.ProfileRequest
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.ProfileResult
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
    username = testUsername,
    email = "cookingBobo@gmail.com"
)

class InfluencerPublicProfileServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository>()
    private val storageService = mock<StorageService>()

    private val service = InfluencerPublicProfileService(influencerProfileRepository, storageService)

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

            whenever(storageService.getUrl(profile.profilePicObjectKey))
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
                    email = profile.email
                }
            )
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
            verify(storageService).getUrl(profile.profilePicObjectKey)
        }

    @Test
    fun `get influencer public profile should return profile`() =
        runBlocking<Unit> {
            // ARRANGE
            val presignedUrl = "this is a presigned url"
            whenever(influencerProfileRepository.getPublicProfile(getProfileRequest))
                .thenReturn(ProfileResult.Success(profile))

            whenever(storageService.getUrl(profile.profilePicObjectKey))
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
                    email = profile.email
                }
            )
            verify(influencerProfileRepository).getPublicProfile(getProfileRequest)
            verify(storageService).getUrl(profile.profilePicObjectKey)
        }
}