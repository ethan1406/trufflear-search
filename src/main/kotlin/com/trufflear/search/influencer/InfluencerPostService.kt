package com.trufflear.search.influencer

import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.mappers.toPostDomain
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import com.trufflear.search.influencer.util.CaptionParser

class InfluencerPostService(
    private val repository: InfluencerPostRepository,
    private val captionParser: CaptionParser,
) {

    suspend fun handleIncomingPosts(
        influencerEmail: String,
        igPosts: List<IgPost>,
    ): CallSuccess? {
        val incomingPosts = getPostsWithoutNewLine(igPosts)
        val incomingPostIds = incomingPosts.map { it.id }.toHashSet()
        val incomingPostMap = incomingPosts.associateBy { it.id }

        val existingPostIgIds = repository.getAllPostIgIds(influencerEmail)
            ?: return null

        deleteOldPosts(
            existingPostIgIds = existingPostIgIds,
            incomingPostIgIds = incomingPostIds
        ) ?: return null

        insertNewPosts(
            influencerEmail = influencerEmail,
            existingPostIgIds = existingPostIgIds,
            incomingPostIgIds = incomingPostIds,
            incomingPostMap = incomingPostMap
        ) ?: return null

        updatePosts(
            influencerEmail = influencerEmail,
            existingPostIgIds = existingPostIgIds,
            incomingPostIgIds = incomingPostIds,
            incomingPostMap = incomingPostMap
        ) ?: return null

        return CallSuccess
    }


    private suspend fun insertNewPosts(
        influencerEmail: String,
        existingPostIgIds: Set<String>,
        incomingPostIgIds: Set<String>,
        incomingPostMap: Map<String, IgPost>
    ): CallSuccess? {

        val newPosts = incomingPostIgIds.minus(existingPostIgIds).mapNotNull { igId ->
            incomingPostMap[igId]?.toPostDomain(captionParser)
        }

        return repository.insertNewPosts(
            newPosts = newPosts,
            influencerEmail = influencerEmail
        )
    }

    private suspend fun updatePosts(
        influencerEmail: String,
        existingPostIgIds: Set<String>,
        incomingPostIgIds: Set<String>,
        incomingPostMap: Map<String, IgPost>
    ): CallSuccess? {
        val postsToUpdate = incomingPostIgIds.union(existingPostIgIds).mapNotNull { igId ->
            incomingPostMap[igId]?.toPostDomain(captionParser)
        }

        return repository.updatePosts(postsToUpdate)
    }

    private suspend fun deleteOldPosts(
        existingPostIgIds: Set<String>,
        incomingPostIgIds: Set<String>
    ): CallSuccess? =
        repository.deletePosts(
            existingPostIgIds.minus(incomingPostIgIds)
        )

    private fun getPostsWithoutNewLine(igPosts: List<IgPost>) =
        igPosts.map {
            it.copy(caption = it.caption?.replace("\n", " "))
        }
}