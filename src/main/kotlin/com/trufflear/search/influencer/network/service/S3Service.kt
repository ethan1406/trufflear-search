package com.trufflear.search.influencer.network.service

import com.amazonaws.AmazonServiceException
import com.amazonaws.HttpMethod
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.IOUtils
import com.trufflear.search.frameworks.Result
import com.trufflear.search.influencer.domain.CallSuccess
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import mu.KotlinLogging
import java.awt.Image
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.util.Date

private const val contentType = "image/jpeg"
private const val profilePicS3Directory = "prod/profile-pics"
private const val thumbnailS3Directory = "prod/post-thumbnails"
private const val imageType = "jpg"
private const val s3Bucket = "trufflear"

class S3Service(
    val client: AmazonS3,
    val profileRepository: InfluencerProfileRepository
): StorageService {

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
            logger.error(e) { "error generating presigned url for profile image upload for $username" }
            null
        } catch (e: SdkClientException) {
            logger.error(e) { "error generating presigned url for profile image upload for $username" }
            null
        } catch (e: Exception) {
            logger.error(e) { "error generating presigned url for profile image upload for $username" }
            null
        }

    override fun uploadImageToKey(imageUrl: String, objectKey: String): Result<Unit, Unit> =
        try {
            logger.debug { "uploading image to key: $objectKey" }

            val contents = IOUtils.toByteArray(URL(imageUrl).openStream())
            val stream = ByteArrayInputStream(contents)
            val metadata = ObjectMetadata().apply {
                contentType = contentType
                contentLength = contents.size.toLong()
            }

            client.putObject(s3Bucket, objectKey, stream, metadata)

            stream.close()

            Result.Success(Unit)
        } catch (e: AmazonServiceException) {
            logger.error(e) { "error uploading image to key: $objectKey" }
            Result.Error(Unit)
        } catch (e: SdkClientException) {
            logger.error(e) { "error uploading image to key: $objectKey" }
            Result.Error(Unit)
        } catch (e: IOException) {
            logger.error(e) { "error uploading image to key: $objectKey" }
            Result.Error(Unit)
        } catch (e: Exception) {
            logger.error(e) { "error uploading image to key: $objectKey" }
            Result.Error(Unit)
        }

    override fun deleteObject(objectKey: String): Result<Unit, Unit> =
        try {
            logger.debug { "deleting object with key: $objectKey" }

            client.deleteObject(s3Bucket, objectKey)

            Result.Success(Unit)
        } catch (e: AmazonServiceException) {
            logger.error(e) { "error deleting object with key: $objectKey" }
            Result.Error(Unit)
        } catch (e: SdkClientException) {
            logger.error(e) { "error deleting object with key: $objectKey" }
            Result.Error(Unit)
        } catch (e: Exception) {
            logger.error(e) { "error deleting object with key: $objectKey" }
            Result.Error(Unit)
        }

    override fun getThumbnailObjectKey(username: String, postId: String): String =
        getPostThumbnailObjectKey(username, postId)

    override suspend fun saveProfileImageKey(username: String): CallSuccess? =
        profileRepository.saveProfileImageKey(username, getProfileImageKey(username))

    override fun getPresignedUrl(objectKey: String): String? =
        try {
            logger.debug { "generating url for profile image (GET) for object key: $objectKey" }
            if (objectKey.isEmpty()) {
                null
            } else {
                val request = GeneratePresignedUrlRequest(s3Bucket, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(getExpiration())

                client.generatePresignedUrl(request).toString()
            }
        } catch (e: AmazonServiceException) {
            logger.error(e) { "error generating url for object key: $objectKey" }
            null
        } catch (e: SdkClientException) {
            logger.error(e) { "error generating url for object key: $objectKey" }
            null
        } catch (e: Exception) {
            logger.error(e) { "error generating url for object key: $objectKey" }
            null
        }

    private fun getProfileImageKey(username: String) = "$profilePicS3Directory/$username.$imageType"

    private fun getPostThumbnailObjectKey(username: String, postId: String) = "$thumbnailS3Directory/$username/$postId.$imageType"

    private fun getExpiration(): Date =
        Date().apply {
            val expTimeMillis = Instant.now().toEpochMilli() + 1000 * 60 * 15 // 15 mins
            time = expTimeMillis
        }
}