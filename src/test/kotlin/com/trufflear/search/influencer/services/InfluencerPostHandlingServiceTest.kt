package com.trufflear.search.influencer.services

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Instant

private const val email = "bobo@gmail.com"
private const val username = "cooking_bobo"

private val post = Post(
    caption = "testing #test @temp",
    hashTags = "#test",
    mentions = "@temp",
    mediaType = IgMediaType.IMAGE.name,
    thumbnailUrl = "",
    thumbnailObjectKey = "",
    permalink = "link",
    username = username,
    id = "1",
    timestamp = Instant.now()
)

class InfluencerPostHandlingServiceTest {

    private val repository = mock<InfluencerPostRepository>()

    private val service = InfluencerPostHandlingService(repository)

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

            whenever(
                repository.handleIncomingPosts(
                    postsToDelete = anySet(),
                    postsToUpdate = anyList(),
                    newPosts = anyList(),
                    influencerEmail = anyString()
                )
            ).thenReturn(CallSuccess)

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isNotNull
            verify(repository).getAllPostIgIds(email)

            val emailCaptor = argumentCaptor<String>()
            val postToDelete = argumentCaptor<Set<String>>()
            val postToUpdateCaptor = argumentCaptor<List<Post>>()
            val newPostCaptor = argumentCaptor<List<Post>>()
            verify(repository).handleIncomingPosts(
                postsToDelete = postToDelete.capture(),
                postsToUpdate = postToUpdateCaptor.capture(),
                newPosts = newPostCaptor.capture(),
                influencerEmail = emailCaptor.capture()
            )
            assertThat(emailCaptor.firstValue).isEqualTo(email)
            assertThat(postToDelete.firstValue).isEqualTo(setOf("6", "7"))
            assertThat(newPostCaptor.firstValue).isEqualTo(emptyList<Post>())
            assertThat(postToUpdateCaptor.firstValue.map { it.id }).isEqualTo(
                listOf("1", "2", "3", "4", "5")
            )
            verifyNoMoreInteractions(repository)
        }

    @Test
    fun `handleIncomingPosts should insert, update and delete posts when mismatch between incoming and old posts`() =
        runBlocking<Unit> {
            // ARRANGE
            whenever(repository.getAllPostIgIds(email)).thenReturn(
                setOf("2", "3", "4", "10")
            )

            whenever(
                repository.handleIncomingPosts(
                    postsToDelete = anySet(),
                    postsToUpdate = anyList(),
                    newPosts = anyList(),
                    influencerEmail = anyString()
                )
            ).thenReturn(CallSuccess)
            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isNotNull
            verify(repository).getAllPostIgIds(email)

            val emailCaptor = argumentCaptor<String>()
            val postToDelete = argumentCaptor<Set<String>>()
            val postToUpdateCaptor = argumentCaptor<List<Post>>()
            val newPostCaptor = argumentCaptor<List<Post>>()
            verify(repository).handleIncomingPosts(
                postsToDelete = postToDelete.capture(),
                postsToUpdate = postToUpdateCaptor.capture(),
                newPosts = newPostCaptor.capture(),
                influencerEmail = emailCaptor.capture()
            )
            assertThat(emailCaptor.firstValue).isEqualTo(email)
            assertThat(postToDelete.firstValue).isEqualTo(setOf("10"))

            assertThat(newPostCaptor.firstValue.map { it.id }).isEqualTo(
                listOf("1", "5")
            )
            assertThat(postToUpdateCaptor.firstValue.map { it.id }).isEqualTo(
                listOf("2", "3", "4")
            )

            verifyNoMoreInteractions(repository)
        }


    private fun getIgPosts(): List<Post> =
        listOf(
            post,
            post.copy(id = "2"),
            post.copy(id = "3"),
            post.copy(id = "4"),
            post.copy(id = "5"),
        )
}