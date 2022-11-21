package com.trufflear.search.influencer.network.service

import com.trufflear.search.config.TypesenseFields
import com.trufflear.search.config.TypesenseLocale
import mu.KotlinLogging
import org.typesense.api.Client
import org.typesense.api.Configuration
import org.typesense.api.FieldTypes
import org.typesense.model.CollectionSchema
import org.typesense.model.Field
import org.typesense.resources.Node
import java.time.Duration
import kotlin.math.log

class SearchIndexService {
    private val logger = KotlinLogging.logger {}

    private fun getTypeSenseClient(): Client {
        val nodes = listOf(
            Node(
                System.getenv("TYPESENSE_PROTOCOL") ?: "https",  // For Typesense Cloud use https
                System.getenv("TYPESENSE_HOST")?: "search.trufflear.com",  // For Typesense Cloud use xxx.a1.typesense.net
                System.getenv("TYPESENSE_PORT") ?:"8108" // For Typesense Cloud use 443
            )
        )

        val configuration = Configuration(nodes, Duration.ofSeconds(2), System.getenv("TYPESENSE_ADMIN_API_KEY") ?: "")
        return Client(configuration)
    }

    fun createSearchCollectionForInfluencer(influencerEmail: String): CollectionCreation =
        try {
            val client = getTypeSenseClient()
            val collectionSchema = CollectionSchema()
            collectionSchema.name(influencerEmail).fields(
                listOf(
                    Field().name(TypesenseFields.postId).type(FieldTypes.STRING),
                    Field().name(TypesenseFields.caption).type(FieldTypes.STRING).locale(TypesenseLocale.chinese),
                    Field().name(TypesenseFields.thumbnailUrl).type(FieldTypes.STRING),
                    Field().name(TypesenseFields.mentions).type(FieldTypes.STRING),
                    Field().name(TypesenseFields.hashtags).type(FieldTypes.STRING).locale(TypesenseLocale.chinese),
                    Field().name(TypesenseFields.permalink).type(FieldTypes.STRING),
                    Field().name(TypesenseFields.createdAtTimeMillis).type(FieldTypes.INT64).sort(true)
                )
            )
            client.collections().create(collectionSchema)

            CollectionCreation.Success
        } catch (e: Exception) {
            logger.error(e) { "error creating typesense search index for $influencerEmail" }

            CollectionCreation.Failure
        }
}

sealed class CollectionCreation {
    object Success: CollectionCreation()

    object Failure: CollectionCreation()

}