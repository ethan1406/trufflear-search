package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.domain.InfluencerPublicProfile
import com.trufflear.search.influencer.getProfileRequest
import com.trufflear.search.influencer.network.service.CollectionCreation
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.InsertResult
import com.trufflear.search.influencer.repositories.ProfileRequest
import com.trufflear.search.influencer.repositories.ProfileResult
import com.trufflear.search.influencer.repositories.SearchIndexRepository
import com.trufflear.search.influencer.services.InfluencerAccountService
import com.trufflear.search.influencer.signupRequest
import com.trufflear.search.influencer.updateProfileRequest
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class InfluencerAccountServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository>()
    private val searchIndexRepository = mock<SearchIndexRepository>()

    private val service = InfluencerAccountService(
        influencerProfileRepository, searchIndexRepository
    )

    private val influencer = Influencer(
        emailVerified = false,
        name = "Bobo Chang",
        email = "bobo@gmail.com"
    )

    @Test
    fun `signup should throw unauthenticated status exception when influencer cannot be found in context`() = runBlocking<Unit> {
        // ARRANGE
        val request = signupRequest { }

        // ACT
        val exception = assertThrows<StatusException> { service.signup(request) }

        // ASSERT
        assertThat(exception.status).isEqualTo(Status.UNAUTHENTICATED)
    }

    @Test
    fun `signup should throw already-exists exception when profile repository returns already exist error`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.insertInfluencer(influencer))
                .thenReturn(InsertResult.UserAlreadyExists)

            val request = signupRequest { }

            // ACT
            val exception = assertThrows<StatusException> { service.signup(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.ALREADY_EXISTS)
            verify(influencerProfileRepository).insertInfluencer(influencer)
        }


    @Test
    fun `signup should throw unknown exception when search repository returns error`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.insertInfluencer(influencer))
                .thenReturn(InsertResult.Success)

            whenever(searchIndexRepository.createSearchCollectionFor(influencer.email))
                .thenReturn(CollectionCreation.Failure)

            val request = signupRequest { }

            // ACT
            val exception = assertThrows<StatusException> { service.signup(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(influencerProfileRepository).insertInfluencer(influencer)
            verify(searchIndexRepository).createSearchCollectionFor(influencer.email)
        }

    @Test
    fun `signup should return response`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.insertInfluencer(influencer))
                .thenReturn(InsertResult.Success)

            whenever(searchIndexRepository.createSearchCollectionFor(influencer.email))
                .thenReturn(CollectionCreation.Success)

            val request = signupRequest { }

            // ACT
            val response = service.signup(request)

            // ASSERT
            assertThat(response).isNotNull
            verify(influencerProfileRepository).insertInfluencer(influencer)
            verify(searchIndexRepository).createSearchCollectionFor(influencer.email)
        }

    @Test
    fun `updateProfile should throw permission denied exception when user has not signed up yet`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.userExists(influencer.email))
                .thenReturn(false)

            val request = updateProfileRequest { }

            // ACT
            val exception = assertThrows<StatusException> { service.updateProfile(request) }

            // ASSERT
            assertThat(exception.status.code).isEqualTo(Status.Code.PERMISSION_DENIED)
            verify(influencerProfileRepository).userExists(influencer.email)
        }

    @Test
    fun `updateProfile should throw unknown exception when there is an insertion error`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.userExists(influencer.email))
                .thenReturn(true)

            val title = "title"

            whenever(
                influencerProfileRepository.updateInfluencerProfile(
                    influencer.email, title, "", ""
                )
            ).thenReturn(null)

            val request = updateProfileRequest {
                profileTitle = "title"
            }

            // ACT
            val exception = assertThrows<StatusException> { service.updateProfile(request) }

            // ASSERT
            assertThat(exception.status.code).isEqualTo(Status.Code.UNKNOWN)
            verify(influencerProfileRepository).userExists(influencer.email)
            verify(influencerProfileRepository).updateInfluencerProfile(
                influencer.email, title, "", ""
            )
        }

    @Test
    fun `updateProfile should return response`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.userExists(influencer.email))
                .thenReturn(true)

            val title = "title"
            val category = "category"

            whenever(
                influencerProfileRepository.updateInfluencerProfile(
                    influencer.email, title, category, ""
                )
            ).thenReturn(CallSuccess)

            val request = updateProfileRequest {
                profileTitle = "title"
                professionCategory = category
            }

            // ACT
            val response = service.updateProfile(request)

            // ASSERT
            assertThat(response).isNotNull
            verify(influencerProfileRepository).userExists(influencer.email)
            verify(influencerProfileRepository).updateInfluencerProfile(
                influencer.email, title, category, ""
            )
        }

    @Test
    fun `get profile should return throw exception when not found`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(ProfileResult.NotFound)

            val request = getProfileRequest {}

            // ACT
            val exception = assertThrows<StatusException> { service.getProfile(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
        }

    @Test
    fun `get profile should return response when found`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val bioDescription = "hi, how are you?"

            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(
                    ProfileResult.Success(
                        InfluencerPublicProfile(
                            profilePicUrl = "",
                            profileTitle = "",
                            professionCategory = "",
                            bioDescription = bioDescription,
                            isProfileLive = true
                        )
                    )
                )

            val request = getProfileRequest {}

            // ACT
            val response = service.getProfile(request)

            // ASSERT
            assertThat(response.isProfileLive).isTrue
            assertThat(response.influencerProfile.bioDescription).isEqualTo(bioDescription)
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
        }
}