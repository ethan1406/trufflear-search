package com.trufflear.search.influencer.domain

data class Influencer(
    val emailVerified: Boolean,
    val name: String,
    val email: String
)

data class InfluencerProfile(
    val username: String,
    val email: String,
    val profilePicObjectKey: String,
    val profileTitle: String,
    val professionCategory: String,
    val bioDescription: String,
    val isProfileLive: Boolean
)