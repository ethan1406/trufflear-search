package com.trufflear.search.influencer.network.service

import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.frameworks.Result

interface StorageService {

    fun getProfileImageUploadUrl(username: String): String?

    fun getPresignedUrl(objectKey: String): String?

    fun uploadImageToKey(imageUrl: String, objectKey: String): Result<Unit, Unit>

    fun deleteObject(objectKey: String): Result<Unit, Unit>

    fun getThumbnailObjectKey(username: String, postId: String): String

    suspend fun saveProfileImageKey(username: String): CallSuccess?
}