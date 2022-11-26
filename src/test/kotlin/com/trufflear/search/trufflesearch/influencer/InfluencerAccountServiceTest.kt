package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.domain.InfluencerProfile
import com.trufflear.search.influencer.getProfileImageUploadUrlRequest
import com.trufflear.search.influencer.getProfileRequest
import com.trufflear.search.influencer.imageUploadSuccessRequest
import com.trufflear.search.influencer.network.service.CollectionCreation
import com.trufflear.search.influencer.network.service.ImageService
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

private val influencer = Influencer(
    emailVerified = false,
    name = "Bobo Chang",
    email = "bobo@gmail.com"
)

private val influencerProfile = InfluencerProfile(
    profilePicObjectKey = "",
    profileTitle = "",
    professionCategory = "",
    bioDescription = "hi, how are you?",
    isProfileLive = true,
    username = "cooking_bobo",
    email = "cookingBobo@gmail.com"
)

class InfluencerAccountServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository>()
    private val searchIndexRepository = mock<SearchIndexRepository>()
    private val imageService = mock<ImageService>()

    private val service = InfluencerAccountService(
        influencerProfileRepository, searchIndexRepository, imageService
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
            val presignedUrl = "presigned url"

            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(ProfileResult.Success(influencerProfile))

            whenever(imageService.getPresignedUrl(influencerProfile.profilePicObjectKey))
                .thenReturn(presignedUrl)

            val request = getProfileRequest {}

            // ACT
            val response = service.getProfile(request)

            // ASSERT
            assertThat(response.isProfileLive).isTrue
            assertThat(response.influencerProfile.bioDescription).isEqualTo(influencerProfile.bioDescription)
            assertThat(response.influencerProfile.email).isEqualTo(influencerProfile.email)
            assertThat(response.influencerProfile.profilePicUrl).isEqualTo(presignedUrl)

            verify(influencerProfileRepository).getPublicProfile(profileRequest)
            verify(imageService).getPresignedUrl(influencerProfile.profilePicObjectKey)
        }

    @Test
    fun `get profile image upload url should throw exception if repository fails to fetch profile`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(ProfileResult.NotFound)

            val request = getProfileImageUploadUrlRequest { }

            // ACT
            val exception = assertThrows<StatusException> { service.getProfileImageUploadUrl(request) }

            // ASSERT
            assertThat(exception.status.code).isEqualTo(Status.Code.UNKNOWN)
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
        }

    @Test
    fun `get profile image upload url should return presigned url`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val uploadUrl = "s3upload presigned url"
            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(ProfileResult.Success(influencerProfile))

            whenever(imageService.getProfileImageUploadUrl(influencerProfile.username)).thenReturn(uploadUrl)

            val request = getProfileImageUploadUrlRequest { }

            // ACT
            val response = service.getProfileImageUploadUrl(request)

            // ASSERT
            assertThat(response.url).isEqualTo(uploadUrl)
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
            verify(imageService).getProfileImageUploadUrl(influencerProfile.username)
        }

    @Test
    fun `succeed in image upload request throws exceptions when username is empty`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(
                    ProfileResult.Success(
                        influencerProfile.copy(username = "")
                    )
                )

            val request = imageUploadSuccessRequest { }

            // ACT
            val exception = assertThrows<StatusException> { service.succeedInImageUpload(request) }

            // ASSERT
            assertThat(exception.status.code).isEqualTo(Status.Code.PERMISSION_DENIED)
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
        }

    @Test
    fun `succeed in image upload returns success`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val profileRequest = ProfileRequest.WithEmail(influencer.email)
            whenever(influencerProfileRepository.getPublicProfile(profileRequest))
                .thenReturn(ProfileResult.Success(influencerProfile))

            whenever(imageService.saveProfileImageKey(influencerProfile.username))
                .thenReturn(CallSuccess)

            val request = imageUploadSuccessRequest { }

            // ACT
            val response = service.succeedInImageUpload(request)

            // ASSERT
            assertThat(response).isNotNull
            verify(influencerProfileRepository).getPublicProfile(profileRequest)
            verify(imageService).saveProfileImageKey(influencerProfile.username)
        }
}