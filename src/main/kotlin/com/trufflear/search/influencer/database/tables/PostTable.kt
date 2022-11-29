package com.trufflear.search.influencer.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object PostTable: LongIdTable("INFLUENCER_POST") {
    val igId = varchar("igId", 50)
    val influencerEmail = reference("influencer_email", InfluencerTable)
    val username = varchar("username", 30)
    val caption = varchar("caption", 3000).default("")
    val hashtags = varchar("hashtags", 4200).default("")
    val mentions = varchar("mentions", 800).default("")
    val mediaType = varchar("media_type", 20)
    val thumbnailObjectKey = varchar("thumbnail_object_key", 1000)
    val permalink = varchar("permalink", 200)
    val createdAtTimestamp = timestamp("created_at_timestamp")
}
