package com.trufflear.search.trufflesearch.influencer

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.InfluencerPostService
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import com.trufflear.search.influencer.util.CaptionParser
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

private const val email = "bobo@gmail.com"
private const val username = "cooking_bobo"

private val igPost = IgPost(
    caption = null,
    mediaType = IgMediaType.IMAGE.name,
    mediaUrl = "url1",
    thumbnailUrl = null,
    permalink = "link",
    username = username,
    id = "1",
    timestamp = "malformed timestamp"
)

class InfluencerPostServiceTest {

    private val repository = mock<InfluencerPostRepository>()
    private val captionParser = mock<CaptionParser>{
        on { getHashTags(any()) } doReturn ""
        on { getMentions(any()) } doReturn ""
    }

    private val service = InfluencerPostService(repository, captionParser)

    @Test
    fun `handleIncomingPosts should not call anything if incoming posts are empty`() =
        runBlocking<Unit> {
            // ACT
            val response = service.handleIncomingPosts(email, emptyList())

            // ASSERT
            assertThat(response).isEqualTo(CallSuccess)
            verifyNoInteractions(repository)
        }

    @Test
    fun `handleIncomingPosts should return null when repository call returns null`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(repository.getAllPostIgIds(email)).thenReturn(null)

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isNull()
            verify(repository).getAllPostIgIds(email)
        }

    @Test
    fun `handleIncomingPosts should delete and update posts when mismatch between incoming and old posts`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(repository.getAllPostIgIds(email)).thenReturn(
                setOf("1", "2", "3", "4", "5", "6", "7")
            )

            whenever(repository.deletePosts(setOf("6", "7"))).thenReturn(CallSuccess)
            whenever(repository.updatePosts(anyList())).thenReturn(CallSuccess)

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isNotNull
            verify(repository).getAllPostIgIds(email)
            verify(repository).deletePosts(setOf("6", "7"))
            argumentCaptor<List<Post>> {
                verify(repository).updatePosts(capture())

                assertThat(firstValue.map { it.id }).isEqualTo(
                    listOf("1", "2", "3", "4", "5")
                )
            }
            verifyNoMoreInteractions(repository)
        }

    @Test
    fun `handleIncomingPosts should insert, update and delete posts when mismatch between incoming and old posts`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(repository.getAllPostIgIds(email)).thenReturn(
                setOf("2", "3", "4", "10")
            )
            whenever(repository.deletePosts(setOf("10"))).thenReturn(CallSuccess)
            whenever(repository.insertNewPosts(anyList(), eq(email))).thenReturn(CallSuccess)
            whenever(repository.updatePosts(anyList())).thenReturn(CallSuccess)

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isNotNull
            verify(repository).getAllPostIgIds(email)
            verify(repository).deletePosts(setOf("10"))

            argumentCaptor<List<Post>> {
                verify(repository).insertNewPosts(capture(), eq(email))
                assertThat(firstValue.map { it.id }).isEqualTo(
                    listOf("1", "5")
                )

                verify(repository).updatePosts(capture())
                assertThat(secondValue.map { it.id }).isEqualTo(
                    listOf("2", "3", "4")
                )
            }
            verifyNoMoreInteractions(repository)
        }


    private fun getIgPosts(): List<IgPost> =
        listOf(
            igPost,
            igPost.copy(id = "2"),
            igPost.copy(id = "3"),
            igPost.copy(id = "4"),
            igPost.copy(id = "5"),
        )
}