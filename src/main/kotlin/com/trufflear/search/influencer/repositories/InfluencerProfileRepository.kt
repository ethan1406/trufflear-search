package com.trufflear.search.influencer.repositories

import com.trufflear.search.influencer.database.tables.InfluencerTable
import com.trufflear.search.influencer.domain.InfluencerPublicProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class InfluencerProfileRepository(
    private val dataSource: DataSource
) {
    private val logger = KotlinLogging.logger {}

    internal suspend fun getPublicProfile(username: String): Result<InfluencerPublicProfile?> =
        withContext(Dispatchers.IO) {
            runCatching {
                transaction(Database.connect(dataSource)) {
                    addLogger(StdOutSqlLogger)

                    InfluencerTable
                        .slice(
                            InfluencerTable.profileTitle, InfluencerTable.categoryTitle,
                            InfluencerTable.bioDescription, InfluencerTable.profileImageUrl,
                            InfluencerTable.isProfileLive
                        )
                        .select { InfluencerTable.username eq username }
                        .map {
                            InfluencerPublicProfile(
                                profilePicUrl = it[InfluencerTable.profileImageUrl],
                                profileTitle = it[InfluencerTable.profileTitle],
                                professionCategory = it[InfluencerTable.categoryTitle],
                                bioDescription = it[InfluencerTable.bioDescription],
                                isProfileLive = it[InfluencerTable.isProfileLive]
                            )
                        }.firstOrNull()
                }
            }
        }
}