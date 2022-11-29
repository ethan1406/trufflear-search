package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import mu.KotlinLogging

class InfluencerPostHandlingService(
    private val repository: InfluencerPostRepository,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun handleIncomingPosts(
        influencerEmail: String,
        incomingPosts: List<Post>,
    ): CallSuccess? {
        return if (incomingPosts.isNotEmpty()) {
            val incomingPostIds = incomingPosts.map { it.id }.toHashSet()
            val incomingPostMap = incomingPosts.associateBy { it.id }

            val existingPostIgIds = repository.getAllPostIgIds(influencerEmail)
                ?: return null

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

            logger.debug { "repository handling incoming posts" }
            repository.handleIncomingPosts(
                postsToDelete = oldPosts,
                newPosts = newPosts,
                postsToUpdate = commonPosts,
                influencerEmail = influencerEmail
            )
        } else {
            CallSuccess
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