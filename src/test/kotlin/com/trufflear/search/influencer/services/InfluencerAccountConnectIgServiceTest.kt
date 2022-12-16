package com.trufflear.search.influencer.services

import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.authorizationRequiredOrNull
import com.trufflear.search.influencer.connectIgUserMediaRequest
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.IgAuth
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgShortLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.refreshIgUserMediaRequest
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.successOrNull
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private val influencer = Influencer(
    emailVerified = false,
    name = "Bobo Chang",
    email = "bobo@gmail.com"
)

private const val authCode = "auth_code"

private const val shortAccessToken = "token_test"
private const val igUserId = "ig_user_id"

private const val longAccessToken = "long_token_test"
private const val expiresIn = "51231"

private val igUserInfo = IgUserInfo(
    userId = igUserId,
    userName = "username",
    accountType = "PERSONAL",
    mediaCount = 10
)

private val igAuth = IgAuth(
    accessToken = longAccessToken,
    instagramId = igUserId
)

class InfluencerAccountConnectIgServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository> {
        onBlocking { userExists(influencer.email) } doReturn true

        onBlocking {
            upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )
        } doReturn CallSuccess

        onBlocking { getIgAuth(influencer.email) } doReturn igAuth
    }

    private val igHandlingService = mock<IgHandlingService> {
        onBlocking {
            fetchAndStoreUserInfoAndToken(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
        } doReturn Result.Success(Unit)
    }

    private val instagramService = mock<InstagramService> {
        onBlocking { getShortLivedToken(any(), any(), any(), any(), eq(authCode)) } doReturn
                IgServiceResult.Success(IgShortLivedTokenResponse(shortAccessToken, igUserId))
    }

    private val service = InfluencerAccountConnectIgService(
        influencerProfileRepository, instagramService, igHandlingService
    )

    @Test
    fun `connect ig media should throw permission denied exception when user has not signed up yet`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.userExists(influencer.email))
                .thenReturn(false)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }

            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status.code).isEqualTo(Status.Code.PERMISSION_DENIED)
            verify(influencerProfileRepository).userExists(influencer.email)
        }

    @Test
    fun `connect ig media should throw invalid argument exception when ig service returns expired during short token fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getShortLivedToken(any(), any(), any(), any(), eq(authCode)))
                .thenReturn(IgServiceResult.PermissionError)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.PERMISSION_DENIED)
            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
        }

    @Test
    fun `connect ig media should throw unknown exception when ig handling service returns error while fetching user token`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(
                igHandlingService.fetchAndStoreUserInfoAndToken(
                    influencerEmail = influencer.email,
                    instagramUserId = igUserId,
                    accessToken = shortAccessToken
                )
            ).doReturn(Result.Error(IgHandlingService.Error.Unknown))

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
            verify(igHandlingService).fetchAndStoreUserInfoAndToken(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
            verify(igHandlingService).fetchAndStoreUserPosts(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
        }

    @Test
    fun `connect ig media should throw unknown exception when ig handling service returns error while fetching user media`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(
                igHandlingService.fetchAndStoreUserPosts(
                    influencerEmail = influencer.email,
                    instagramUserId = igUserId,
                    accessToken = shortAccessToken
                )
            ).doReturn(Result.Error(IgHandlingService.Error.Unknown))

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
            verify(igHandlingService).fetchAndStoreUserInfoAndToken(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
            verify(igHandlingService).fetchAndStoreUserPosts(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
        }

    @Test
    fun `connect ig media should return response`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val response = service.connectIgUserMedia(request)

            // ASSERT
            assertThat(response).isNotNull

            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
            verify(igHandlingService).fetchAndStoreUserInfoAndToken(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
            verify(igHandlingService).fetchAndStoreUserPosts(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = shortAccessToken
            )
        }

    @Test
    fun `refresh ig media should throw unknown exception when repository returns errors`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.getIgAuth(influencer.email))
                .thenReturn(null)

            val request = refreshIgUserMediaRequest { }
            // ACT
            val exception = assertThrows<StatusException> { service.refreshIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(influencerProfileRepository).getIgAuth(influencer.email)
        }

    @Test
    fun `refresh ig media should return auth window url when auth token is blank`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.getIgAuth(influencer.email))
                .thenReturn(
                    IgAuth(
                        instagramId = igUserId,
                        accessToken = ""
                    )
                )

            val request = refreshIgUserMediaRequest { }
            // ACT
            val response = service.refreshIgUserMedia(request)

            // ASSERT
            assertThat(response.authorizationRequired.authWindowUrl).isNotBlank
            assertThat(response.successOrNull).isNull()
            verify(influencerProfileRepository).getIgAuth(influencer.email)
        }

    @Test
    fun `refresh ig media should return auth window url when ig services returns error`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(
                igHandlingService.fetchAndStoreUserPosts(
                    influencerEmail = influencer.email,
                    instagramUserId = igUserId,
                    accessToken = longAccessToken
                )
            ).doReturn(Result.Error(IgHandlingService.Error.Unknown))

            val request = refreshIgUserMediaRequest { }
            // ACT
            val response = service.refreshIgUserMedia(request)

            // ASSERT
            assertThat(response.authorizationRequired.authWindowUrl).isNotBlank
            assertThat(response.successOrNull).isNull()

            verify(influencerProfileRepository).getIgAuth(influencer.email)
            verify(igHandlingService).fetchAndStoreUserPosts(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = longAccessToken
            )
        }

    @Test
    fun `refresh ig media should return success`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(
                igHandlingService.fetchAndStoreUserPosts(
                    influencerEmail = influencer.email,
                    instagramUserId = igUserId,
                    accessToken = longAccessToken
                )
            ).thenReturn(Result.Success(Unit))

            val request = refreshIgUserMediaRequest { }

            // ACT
            val response = service.refreshIgUserMedia(request)

            // ASSERT
            assertThat(response.successOrNull).isNotNull
            assertThat(response.authorizationRequiredOrNull).isNull()

            verify(influencerProfileRepository).getIgAuth(influencer.email)
            verify(igHandlingService).fetchAndStoreUserPosts(
                influencerEmail = influencer.email,
                instagramUserId = igUserId,
                accessToken = longAccessToken
            )
        }
}