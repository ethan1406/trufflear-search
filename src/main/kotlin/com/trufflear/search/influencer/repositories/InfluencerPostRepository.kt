package com.trufflear.search.influencer.repositories

import com.trufflear.search.influencer.database.tables.PostTable
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.model.IgPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import javax.sql.DataSource

class InfluencerPostRepository(
    private val dataSource: DataSource
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getAllPostIgIds(influencerEmail: String): Set<String>? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)
                    PostTable.slice(PostTable.igId)
                        .select { PostTable.influencerEmail eq influencerEmail }
                        .map { it[PostTable.igId] }
                        .toHashSet()
                }
            } catch (e: Exception) {
                logger.error(e) { "error getting influencer post Ig ids for $influencerEmail" }
                null
            }
        }

    suspend fun insertNewPosts(
        newPosts: List<Post>,
        influencerEmail: String
    ): CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    PostTable.batchInsert(newPosts) { post ->
                        this[PostTable.igId] = post.id
                        this[PostTable.influencerEmail] = influencerEmail
                        this[PostTable.username] = post.username
                        this[PostTable.caption] = post.caption
                        this[PostTable.hashtags] = post.hashTags
                        this[PostTable.mentions] = post.mentions
                        this[PostTable.permalink] = post.permalink
                        this[PostTable.mediaType] = post.mediaType
                        this[PostTable.mediaUrl] = post.mediaUrl
                        this[PostTable.thumbnailUrl] = post.thumbnailUrl
                        this[PostTable.createdAtTimestamp] = post.timestamp
                    }
                }
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error inserting posts for $influencerEmail" }
                null
            }
        }

    suspend fun updatePosts(postsToUpdate: List<Post>) : CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    postsToUpdate.forEach { post ->
                        PostTable.update({ PostTable.igId eq post.id }) {
                            it[caption] = post.caption
                            it[hashtags] = post.hashTags
                            it[mentions] = post.mentions
                            it[permalink] = post.permalink
                        }
                    }
                }
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error updating posts" }
                null
            }
        }

    suspend fun deletePosts(oldPostIgIds: Set<String>): CallSuccess? =
        withContext(Dispatchers.IO) {
            try {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)
                    PostTable.deleteWhere { igId inList oldPostIgIds }
                }
                CallSuccess
            } catch (e: Exception) {
                logger.error(e) { "error deleting posts" }
                null
            }
        }

}