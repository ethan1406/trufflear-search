package com.trufflear.search.influencer.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object InfluencerDbDto: Table("INFLUENCER") {
    val name = varchar("name", 50)
    val email = varchar("email", 50)
    val isEmailVerified = bool("email_verified")
    val bioDescription = varchar("bio_description", 200)
    val profileImageUrl = varchar("image_url", 1000)
    val username = varchar("username", 30)
    val igUserId = varchar("ig_user_id", 50)
    val igLongLivedAccessToken = varchar("ig_long_lived_access_token", 500)
    val igLongLivedAccessTokenExpiresIn = long("ig_long_lived_access_token_expires_in")
    val igRefreshAccessToken = varchar("ig_refresh_access_token", 500)
    val dateCreated = datetime("date_created").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(email)
}