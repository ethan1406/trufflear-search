package com.trufflear.search.influencer.domain

import java.time.Instant

data class Post(
    val caption: String,
    val hashTags: String,
    val mentions: String,
    val mediaType: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val permalink: String,
    val username: String,
    val id: String,
    val timestamp: Instant,
)