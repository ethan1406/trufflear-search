package com.trufflear.search.influencer.services

import com.trufflear.search.frameworks.Result
import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.service.StorageService
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
private const val thumbnailUrl = "imageUrl"

private val post = Post(
    caption = "testing #test @temp",
    hashTags = "#test",
    mentions = "@temp",
    mediaType = IgMediaType.IMAGE.name,
    thumbnailUrl = thumbnailUrl,
    thumbnailObjectKey = "key1",
    permalink = "link",
    username = username,
    id = "1",
    timestamp = Instant.now()
)

class InfluencerPostHandlingServiceTest {

    private val repository = mock<InfluencerPostRepository>()

    private val storageService = mock<StorageService>()

    private val service = InfluencerPostHandlingService(repository, storageService)

    @Test
    fun `handleIncomingPosts should not call anything if incoming posts are empty`() =
        runBlocking<Unit> {
            // ACT
            val response = service.handleIncomingPosts(email, emptyList())

            // ASSERT
            assertThat(response).isEqualTo(Result.Success(Unit))
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
            assertThat(response).isEqualTo(Result.Error(Unit))
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

            val expectedDeleteSet = setOf("6", "7")

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isEqualTo(Result.Success(Unit))

            // storage verification
            verifyNoInteractions(storageService)

            // repository verification
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
            assertThat(postToDelete.firstValue).isEqualTo(expectedDeleteSet)
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

            val deleteSet = setOf("10")
            val expectedNewPostList = listOf("1", "5")

            // ACT
            val response = service.handleIncomingPosts(email, getIgPosts())

            // ASSERT
            assertThat(response).isEqualTo(Result.Success(Unit))

            // storage verification
            expectedNewPostList.forEach {
                verify(storageService).uploadImageToKey(thumbnailUrl, "key$it")
            }
            verifyNoMoreInteractions(storageService)

            // repository verification
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
            assertThat(postToDelete.firstValue).isEqualTo(deleteSet)
            assertThat(newPostCaptor.firstValue.map { it.id }).isEqualTo(expectedNewPostList)
            assertThat(postToUpdateCaptor.firstValue.map { it.id }).isEqualTo(
                listOf("2", "3", "4")
            )

            verifyNoMoreInteractions(repository)
        }


    private fun getIgPosts(): List<Post> =
        listOf(
            post,
            post.copy(id = "2", thumbnailObjectKey = "key2"),
            post.copy(id = "3", thumbnailObjectKey = "key3"),
            post.copy(id = "4", thumbnailObjectKey = "key4"),
            post.copy(id = "5", thumbnailObjectKey = "key5"),
        )
}