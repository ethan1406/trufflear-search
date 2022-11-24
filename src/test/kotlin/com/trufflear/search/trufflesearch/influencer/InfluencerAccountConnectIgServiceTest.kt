package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.InfluencerPostService
import com.trufflear.search.influencer.RefreshIgUserMediaResponse.AuthorizationRequired
import com.trufflear.search.influencer.authorizationRequiredOrNull
import com.trufflear.search.influencer.connectIgUserMediaRequest
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.IgAuth
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.network.model.Cursors
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.model.IgShortLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgUserInfo
import com.trufflear.search.influencer.network.model.IgUserMedia
import com.trufflear.search.influencer.network.model.Paging
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.refreshIgUserMediaRequest
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.services.InfluencerAccountConnectIgService
import com.trufflear.search.influencer.successOrNull
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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

private val postList1 = listOf<IgPost>(mock(), mock(), mock())
private val postList2 = listOf<IgPost>(mock(), mock())

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
    private val influencerPostService = mock<InfluencerPostService>{
        onBlocking { handleIncomingPosts(eq(influencer.email), anyList()) } doReturn CallSuccess
    }

    private val instagramService = mock<InstagramService> {
        onBlocking { getShortLivedToken(any(), any(), any(), any(), eq(authCode)) } doReturn
                IgServiceResult.Success(IgShortLivedTokenResponse(shortAccessToken, igUserId))

        onBlocking { getLongLivedAccessToken(any(), any(), eq(shortAccessToken)) } doReturn
                IgServiceResult.Success(IgLongLivedTokenResponse(longAccessToken, expiresIn))

        onBlocking { getUser(eq(igUserId), any(), eq(shortAccessToken)) } doReturn
                IgServiceResult.Success(igUserInfo)

        onBlocking { getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()) } doReturn
                IgServiceResult.Success(
                    IgUserMedia(emptyList(), null)
                )

    }

    private val service = InfluencerAccountConnectIgService(
        influencerProfileRepository, influencerPostService, instagramService
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
                .thenReturn(IgServiceResult.ExpiredError)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.INVALID_ARGUMENT)
            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
        }

    @Test
    fun `connect ig media should throw unknown exception when ig service returns errors during long token fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getLongLivedAccessToken(any(), any(), eq(shortAccessToken)))
                .thenReturn(IgServiceResult.Unknown)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService).getLongLivedAccessToken(any(), any(), eq(shortAccessToken))
        }

    @Test
    fun `connect ig media should throw unknown exception when ig service returns errors during user info fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUser(eq(igUserId), any(), eq(shortAccessToken)))
                .thenReturn(IgServiceResult.Unknown)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService).getUser(eq(igUserId), any(), eq(shortAccessToken))
        }

    @Test
    fun `connect ig media should throw unknown exception when repository returns insert errors`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )).thenReturn(null)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService).getUser(eq(igUserId), any(), eq(shortAccessToken))
            verify(influencerProfileRepository).upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )
        }

    @Test
    fun `connect ig media should throw permission denied exception when ig service returns permission error during media fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(IgServiceResult.PermissionError)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.PERMISSION_DENIED)
            verify(instagramService).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
        }

    @Test
    fun `connect ig media should throw unknown exception when ig service returns permission error during media fetching second time`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            postList1,
                            Paging(
                                cursors = Cursors(
                                    before = "before",
                                    after = "after"
                                ),
                                previous = null,
                                next = "next"
                            )
                        )
                    ),
                    IgServiceResult.Unknown
                )

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
        }

    @Test
    fun `connect ig media should throw unknown exception when post service returns errors while handling incoming posts`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            postList1,
                            Paging(
                                cursors = Cursors(
                                    before = "before",
                                    after = "after"
                                ),
                                previous = null,
                                next = "next"
                            )
                        )
                    ),
                    IgServiceResult.Success(
                        IgUserMedia(postList2, null)
                    )
                )

            whenever(influencerPostService.handleIncomingPosts(influencer.email, postList1 + postList2)).thenReturn(null)

            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val exception = assertThrows<StatusException> { service.connectIgUserMedia(request) }

            // ASSERT
            assertThat(exception.status).isEqualTo(Status.UNKNOWN)
            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verify(influencerPostService).handleIncomingPosts(influencer.email, postList1 + postList2)
        }

    @Test
    fun `connect ig media should response`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            postList1,
                            Paging(
                                cursors = Cursors(
                                    before = "before",
                                    after = "after"
                                ),
                                previous = null,
                                next = "next"
                            )
                        )
                    ),
                    IgServiceResult.Success(
                        IgUserMedia(postList2, null)
                    )
                )


            val request = connectIgUserMediaRequest {
                instagramAuthCode = authCode
            }
            // ACT
            val response = service.connectIgUserMedia(request)

            // ASSERT
            assertThat(response).isNotNull

            verify(instagramService).getShortLivedToken(any(), any(), any(), any(), eq(authCode))
            verify(instagramService).getLongLivedAccessToken(any(), any(), eq(shortAccessToken))
            verify(instagramService).getUser(eq(igUserId), any(), eq(shortAccessToken))
            verify(influencerProfileRepository).upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )

            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verify(influencerPostService).handleIncomingPosts(influencer.email, postList1 + postList2)
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
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(longAccessToken), anyOrNull()))
                .thenReturn(IgServiceResult.Unknown)

            val request = refreshIgUserMediaRequest { }
            // ACT
            val response = service.refreshIgUserMedia(request)

            // ASSERT
            assertThat(response.authorizationRequired.authWindowUrl).isNotBlank
            assertThat(response.successOrNull).isNull()

            verify(influencerProfileRepository).getIgAuth(influencer.email)
            verify(instagramService).getUserMedia(eq(igUserId), any(), any(), eq(longAccessToken), anyOrNull())
        }

    @Test
    fun `refresh ig media should return success`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(longAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(postList1, null)
                    )
                )

            val request = refreshIgUserMediaRequest { }
            // ACT
            val response = service.refreshIgUserMedia(request)

            // ASSERT
            assertThat(response.successOrNull).isNotNull
            assertThat(response.authorizationRequiredOrNull).isNull()

            verify(influencerProfileRepository).getIgAuth(influencer.email)
            verify(instagramService).getUserMedia(eq(igUserId), any(), any(), eq(longAccessToken), anyOrNull())
            verify(influencerPostService).handleIncomingPosts(influencer.email, postList1)
        }
}