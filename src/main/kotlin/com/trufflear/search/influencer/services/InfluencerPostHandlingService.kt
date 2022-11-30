package com.trufflear.search.influencer.services

import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import mu.KotlinLogging

class InfluencerPostHandlingService(
    private val repository: InfluencerPostRepository,
    private val storageService: StorageService
) {
    private val logger = KotlinLogging.logger {}

    suspend fun handleIncomingPosts(
        influencerEmail: String,
        incomingPosts: List<Post>,
    ): Result<Unit, Unit> {
        return if (incomingPosts.isNotEmpty()) {
            val incomingPostIds = incomingPosts.map { it.id }.toHashSet()
            val incomingPostMap = incomingPosts.associateBy { it.id }

            val existingPostIgIds = repository.getAllPostIgIds(influencerEmail)
                ?: run {
                    logger.error { "unable to fetch existing Instagram post Ids" }
                    return Result.Error(Unit)
                }

            val oldPosts = getOldPosts(
                existingPostIds = existingPostIgIds,
                incomingPostIds = incomingPostIds
            )

            val newPosts = getNewPosts(
                existingPostIds = existingPostIgIds,
                incomingPostIds = incomingPostIds,
                incomingPostMap = incomingPostMap
            )

            val commonPosts = getCommonPosts(
                existingPostIds = existingPostIgIds,
                incomingPostIds = incomingPostIds,
                incomingPostMap = incomingPostMap
            )

            withContext(Dispatchers.IO) {
                val uploadImageTasks = newPosts.map {
                    async {
                        storageService.uploadImageToKey(
                            imageUrl = it.thumbnailUrl,
                            objectKey = it.thumbnailObjectKey
                        )
                    }
                }

                // Delete image when post is deleted
//                val deleteImageTasks = oldPosts.map {
//                        async {
//                            storageService.deleteObject(it)
//                        }
//                }

                val repositoryTask = async {
                    logger.debug { "repository handling incoming posts" }
                    repository.handleIncomingPosts(
                        postsToDelete = oldPosts,
                        newPosts = newPosts,
                        postsToUpdate = commonPosts,
                        influencerEmail = influencerEmail
                    )
                }

                repositoryTask.await()?.let {
                    uploadImageTasks.awaitAll()

                    Result.Success(Unit)
                } ?: Result.Error(Unit)
            }
        } else {
            Result.Success(Unit)
        }
    }

    private fun getNewPosts(
        existingPostIds: Set<String>,
        incomingPostIds: Set<String>,
        incomingPostMap: Map<String, Post>
    ): List<Post> = incomingPostIds.minus(existingPostIds).mapNotNull { incomingPostMap[it] }

    private fun getCommonPosts(
        existingPostIds: Set<String>,
        incomingPostIds: Set<String>,
        incomingPostMap: Map<String, Post>
    ): List<Post> = incomingPostIds.intersect(existingPostIds).mapNotNull { incomingPostMap[it] }

    private fun getOldPosts(
        existingPostIds: Set<String>,
        incomingPostIds: Set<String>
    ): Set<String> = existingPostIds.minus(incomingPostIds)

}