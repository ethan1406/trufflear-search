package com.trufflear.search.influencer.repositories

import com.trufflear.search.influencer.network.service.CollectionCreation
import com.trufflear.search.influencer.network.service.SearchIndexService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchIndexRepository(
    private val searchIndexService: SearchIndexService
) {
    internal suspend fun createSearchCollectionFor(infuencerEmail: String): CollectionCreation =
        withContext(Dispatchers.IO) {
            searchIndexService.createSearchCollectionForInfluencer(infuencerEmail)
        }
}