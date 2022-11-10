package com.trufflear.search.influencer.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object PostDbDto: LongIdTable("INFLUENCER_POST") {
    val igId = varchar("igId", 50)
    val influencerEmail = varchar("email", 50)
    val username = varchar("username", 30)
    val caption = varchar("caption", 3000)
    val hashtags = varchar("hashtags", 4200)
    val mentions = varchar("mentions", 800)
    val mediaType = varchar("media_type", 20)
    val mediaUrl = varchar("media_url", 1000)
    val thumbnailUrl = varchar("thumbnail_url", 1000)
    val permalink = varchar("permalink", 200)
    val createdAtDateTime = timestamp("createdAtDateTime")
}
