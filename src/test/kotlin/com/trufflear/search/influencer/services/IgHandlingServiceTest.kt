package com.trufflear.search.influencer.services

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Influencer
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.model.Cursors
import com.trufflear.search.influencer.network.model.IgLongLivedTokenResponse
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.network.model.IgUserInfo
import com.trufflear.search.influencer.network.model.IgUserMedia
import com.trufflear.search.influencer.network.model.Paging
import com.trufflear.search.influencer.network.service.IgServiceResult
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.util.CaptionParser
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

private val influencer = Influencer(
    emailVerified = false,
    name = "Bobo Chang",
    email = "bobo@gmail.com"
)

private const val shortAccessToken = "token_test"
private const val igUserId = "ig_user_id"

private const val longAccessToken = "long_token_test"
private const val expiresIn = "51231"
private const val thumbnailKey = "thumbnail/key"

private val igUserInfo = IgUserInfo(
    userId = igUserId,
    userName = "username",
    accountType = "PERSONAL",
    mediaCount = 10
)

val igPost = IgPost(
    caption = "caption",
    mediaType = IgMediaType.IMAGE.name,
    mediaUrl = "url1",
    thumbnailUrl = "thumbnailUrl",
    permalink = "link",
    username = igUserInfo.userName,
    id = "1",
    timestamp = "2013-09-29T18:46:19-0700"
)

private val igPostList1 = listOf(igPost, igPost.copy(id = "2", mediaUrl = "url2"), igPost.copy(id = "3", mediaUrl = "url3"))
private val igPostList2 = listOf(igPost.copy(id = "4", mediaUrl = "url4"), igPost.copy(id = "5", mediaUrl = "url5"))

class IgHandlingServiceTest {

    private val influencerProfileRepository = mock<InfluencerProfileRepository> {
        onBlocking {
            upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )
        } doReturn CallSuccess
    }

    private val instagramService = mock<InstagramService> {
        onBlocking { getLongLivedAccessToken(any(), any(), eq(shortAccessToken)) } doReturn
                IgServiceResult.Success(IgLongLivedTokenResponse(longAccessToken, expiresIn))

        onBlocking { getUser(eq(igUserId), any(), eq(shortAccessToken)) } doReturn
                IgServiceResult.Success(igUserInfo)

        onBlocking { getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()) } doReturn
                IgServiceResult.Success(
                    IgUserMedia(emptyList(), null)
                )
    }

    private val postHandlingService = mock<InfluencerPostHandlingService> {
        onBlocking {
            handleIncomingPosts(eq(influencer.email), anyList())
        } doReturn CallSuccess
    }

    private val captionParser = mock<CaptionParser>{
        on { getHashTags(any()) } doReturn ""
        on { getMentions(any()) } doReturn ""
    }

    private val storageService = mock<StorageService> {
        on { getThumbnailObjectKey(any(), any()) } doReturn thumbnailKey
    }


    private val service = IgHandlingService(
        igService = instagramService,
        influencerPostHandlingService = postHandlingService,
        influencerProfileRepository = influencerProfileRepository,
        captionParser = captionParser,
        storageService = storageService
    )



    @Test
    fun `fetch user info and token should return error result when ig service returns errors during long token fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getLongLivedAccessToken(any(), any(), eq(shortAccessToken)))
                .thenReturn(IgServiceResult.Unknown)

            // ACT
            val result = service.fetchAndStoreUserInfoAndToken(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Unknown))

            verify(instagramService).getLongLivedAccessToken(any(), any(), eq(shortAccessToken))
            verifyNoMoreInteractions(instagramService)
            verifyNoInteractions(influencerProfileRepository)
        }

    @Test
    fun `fetch user info and token should return error result when ig service returns errors during user info fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUser(eq(igUserId), any(), eq(shortAccessToken)))
                .thenReturn(IgServiceResult.Unknown)

            // ACT
            val result = service.fetchAndStoreUserInfoAndToken(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Unknown))

            verify(instagramService).getLongLivedAccessToken(any(), any(), eq(shortAccessToken))
            verify(instagramService).getUser(eq(igUserId), any(), eq(shortAccessToken))
            verifyNoMoreInteractions(instagramService)
            verifyNoInteractions(influencerProfileRepository)
        }

    @Test
    fun `fetch user info and token should return error result when repository returns insert errors`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(influencerProfileRepository.upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )).thenReturn(null)

            // ACT
            val result = service.fetchAndStoreUserInfoAndToken(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Unknown))

            verify(instagramService).getLongLivedAccessToken(any(), any(), eq(shortAccessToken))
            verify(instagramService).getUser(eq(igUserId), any(), eq(shortAccessToken))
            verifyNoMoreInteractions(instagramService)
            verify(influencerProfileRepository).upsertInfluencerIgInfo(
                IgLongLivedTokenResponse(longAccessToken, expiresIn),
                igUserInfo,
                influencer.email,
                igUserId
            )
        }

    @Test
    fun `fetch user posts should return error result when ig service returns permission error during media fetching`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(IgServiceResult.PermissionError)

            // ACT
            val result = service.fetchAndStoreUserPosts(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email,
                shouldDownloadImages = false
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Instagram.PermissionError))

            verify(instagramService).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verifyNoMoreInteractions(instagramService)
            verifyNoInteractions(postHandlingService)
        }

    @Test
    fun `fetch user posts should return error result when ig service returns permission error during media fetching second time`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            igPostList1,
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


            // ACT
            val result = service.fetchAndStoreUserPosts(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email,
                shouldDownloadImages = false
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Unknown))

            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verifyNoMoreInteractions(instagramService)
            verifyNoInteractions(postHandlingService)
        }

    @Test
    fun `etch user posts should return error result when post service returns errors while handling incoming posts`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            igPostList1,
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
                        IgUserMedia(igPostList2, null)
                    )
                )

            whenever(postHandlingService.handleIncomingPosts(eq(influencer.email), anyList())).thenReturn(null)

            // ACT
            val result = service.fetchAndStoreUserPosts(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email,
                shouldDownloadImages = false
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Error(IgHandlingService.Error.Unknown))

            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verifyNoMoreInteractions(instagramService)

            val emailCaptor = argumentCaptor<String>()
            val postListCaptor = argumentCaptor<List<Post>>()
            verify(postHandlingService).handleIncomingPosts(emailCaptor.capture(), postListCaptor.capture())
            assertThat(emailCaptor.firstValue).isEqualTo(influencer.email)
            assertThat(postListCaptor.firstValue.map { it.id }).isEqualTo(listOf("1", "2", "3", "4", "5"))
        }

    @Test
    fun `connect ig media should return success result when not downloading images`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            igPostList1,
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
                        IgUserMedia(igPostList2, null)
                    )
                )

            // ACT
            val result = service.fetchAndStoreUserPosts(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email,
                shouldDownloadImages = false
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Success(Unit))

            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verifyNoMoreInteractions(instagramService)

            val emailCaptor = argumentCaptor<String>()
            val postListCaptor = argumentCaptor<List<Post>>()
            verify(postHandlingService).handleIncomingPosts(emailCaptor.capture(), postListCaptor.capture())
            assertThat(emailCaptor.firstValue).isEqualTo(influencer.email)
            assertThat(postListCaptor.firstValue.map { it.id }).isEqualTo(listOf("1", "2", "3", "4", "5"))
        }

    @Test
    fun `connect ig media should return success result when downloading images even if downloading tasks failed`() =
        runBlocking<Unit>(InfluencerCoroutineElement(influencer)) {
            // ARRANGE
            whenever(instagramService.getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull()))
                .thenReturn(
                    IgServiceResult.Success(
                        IgUserMedia(
                            igPostList1,
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
                        IgUserMedia(igPostList2, null)
                    )
                )

            // ACT
            val result = service.fetchAndStoreUserPosts(
                accessToken = shortAccessToken,
                instagramUserId = igUserId,
                influencerEmail = influencer.email,
                shouldDownloadImages = true
            )

            // ASSERT
            assertThat(result).isEqualTo(Result.Success(Unit))

            verify(instagramService, times(2)).getUserMedia(eq(igUserId), any(), any(), eq(shortAccessToken), anyOrNull())
            verifyNoMoreInteractions(instagramService)

            val emailCaptor = argumentCaptor<String>()
            val postListCaptor = argumentCaptor<List<Post>>()
            verify(postHandlingService).handleIncomingPosts(emailCaptor.capture(), postListCaptor.capture())
            assertThat(emailCaptor.firstValue).isEqualTo(influencer.email)
            assertThat(postListCaptor.firstValue.map { it.id }).isEqualTo(listOf("1", "2", "3", "4", "5"))

            (igPostList1 + igPostList2).forEach {
                verify(storageService).uploadImageToKey(it.mediaUrl, thumbnailKey)
            }
        }
}