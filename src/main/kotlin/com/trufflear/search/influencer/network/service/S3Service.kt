package com.trufflear.search.influencer.network.service

import com.amazonaws.AmazonServiceException
import com.amazonaws.HttpMethod
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import mu.KotlinLogging
import java.time.Instant
import java.util.Date

private const val contentType = "image/jpeg"
private const val profilePicS3Directory = "prod/profile-pics"
private const val imageType = "jpg"
private const val s3Bucket = "trufflear"

class S3Service(
    val client: AmazonS3,
    val profileRepository: InfluencerProfileRepository
): ImageService {

    private val logger = KotlinLogging.logger {}

    override fun getProfileImageUploadUrl(username: String): String? =
        try {
            logger.debug { "generating presigned url for profile image upload" }

            val request = GeneratePresignedUrlRequest(s3Bucket, getProfileImageKey(username))
                .withMethod(HttpMethod.PUT)
                .withContentType(contentType)
                .withExpiration(getExpiration())

            client.generatePresignedUrl(request).toString()
        } catch (e: AmazonServiceException) {
            logger.error(e) { "error generating presigned url" }
            null
        } catch (e: SdkClientException) {
            logger.error(e) { "error generating presigned url" }
            null
        } catch (e: Exception) {
            logger.error(e) { "error generating presigned url" }
            null
        }

    override suspend fun saveProfileImageKey(username: String): CallSuccess? =
        profileRepository.saveProfileImageKey(username, getProfileImageKey(username))

    override fun getPresignedUrl(objectKey: String): String? =
        try {
            logger.debug { "generating presigned url for profile image (GET)" }
            if (objectKey.isEmpty()) {
                null
            } else {
                val request = GeneratePresignedUrlRequest(s3Bucket, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(getExpiration())

                client.generatePresignedUrl(request).toString()
            }
        } catch (e: AmazonServiceException) {
            logger.error(e) { "error generating presigned url" }
            null
        } catch (e: SdkClientException) {
            logger.error(e) { "error generating presigned url" }
            null
        } catch (e: Exception) {
            logger.error(e) { "error generating presigned url" }
            null
        }

    private fun getProfileImageKey(username: String) = "$profilePicS3Directory/$username.$imageType"

    private fun getExpiration(): Date =
        Date().apply {
            val expTimeMillis = Instant.now().toEpochMilli() + 1000 * 60 * 15 // 15 mins
            time = expTimeMillis
        }
}