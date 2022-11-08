package com.trufflear.search.influencer.database.models

import org.jetbrains.exposed.sql.Table

object InfluencerDbDto: Table("INFLUENCER") {
    val name = varchar("name", 50)
    val email = varchar("email", 50)
    val isEmailVerified = bool("email_verified")
    val bioDescription = varchar("bio_description", 200)
    val profileImageUrl = varchar("image_url", 1000)
    val title = varchar("title", 30)

    override val primaryKey = PrimaryKey(email)
}