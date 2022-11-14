package com.trufflear.search.influencer.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object InfluencerDbDto: Table("INFLUENCER") {
    val name = varchar("name", 50)
    val email = varchar("email", 50)
    val isEmailVerified = bool("email_verified")
    val isProfileLive = bool("is_profile_live").default(true)
    val bioDescription = varchar("bio_description", 200).default("")
    val profileImageUrl = varchar("image_url", 1000).default("")
    val profileTitle = varchar("profile_title", 30).default("")
    val username = varchar("username", 30).default("")
    val categoryTitle = varchar("category_title", 30).default("")
    val igUserId = varchar("ig_user_id", 50).default("")
    val igMediaCount = integer("ig_media_count").default(0)
    val igAccountType = varchar("ig_account_type", 20).default("")
    val igLongLivedAccessToken = varchar("ig_long_lived_access_token", 500).default("")
    val igLongLivedAccessTokenExpiresIn = long("ig_long_lived_access_token_expires_in").default(0L)
    val dateCreated = timestamp("date_created").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(email)
}