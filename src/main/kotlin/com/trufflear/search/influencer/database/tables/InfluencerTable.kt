package com.trufflear.search.influencer.database.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object InfluencerTable: IdTable<String>("INFLUENCER") {
    val name = varchar("name", 50)
    val email = varchar("email", 50).uniqueIndex()
    val isEmailVerified = bool("email_verified")
    val isProfileLive = bool("is_profile_live").default(true)
    val bioDescription = varchar("bio_description", 200).default("")
    val profileImageObjectKey = varchar("profile_image_object_key", 1000).default("")
    val profileTitle = varchar("profile_title", 30).default("")
    val username = varchar("username", 30).default("")
    val categoryTitle = varchar("category_title", 30).default("")
    val igUserId = varchar("ig_user_id", 50).default("")
    val igMediaCount = integer("ig_media_count").default(0)
    val igAccountType = varchar("ig_account_type", 20).default("")
    val igLongLivedAccessToken = varchar("ig_long_lived_access_token", 500).default("")
    val igLongLivedAccessTokenExpiresIn = long("ig_long_lived_access_token_expires_in").default(0L)
    val dateCreated = timestamp("date_created").defaultExpression(CurrentTimestamp())

    override val id: Column<EntityID<String>> = email.entityId()
}