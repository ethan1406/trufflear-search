package com.trufflear.search.influencer.services.util

import com.trufflear.search.influencer.database.models.InfluencerDbDto
import com.trufflear.search.influencer.domain.Influencer
import io.grpc.Status
import io.grpc.StatusException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

internal fun checkIfUserExists(dataSource: DataSource, influencer: Influencer) {
    transaction(Database.connect(dataSource)) {
        val userNotCreated = InfluencerDbDto.select { InfluencerDbDto.email eq influencer.email }.empty()
        if (userNotCreated) {
            println("user doesn't exist")
            throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))
        }
    }
}