package com.trufflear.search.influencer.network.service

import com.trufflear.search.influencer.domain.CallSuccess

interface ImageService {

    fun getProfileImageUploadUrl(username: String): String?

    fun getPresignedUrl(objectKey: String): String?

    suspend fun saveProfileImageKey(username: String): CallSuccess?
}