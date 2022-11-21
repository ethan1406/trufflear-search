package com.trufflear.search.influencer.mappers

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.igDateFormat
import java.sql.Timestamp
import java.time.Instant

internal fun IgPost.toPostDomain(captionParser: CaptionParser) =
    Post(
        caption = caption.orEmpty(),
        hashTags = caption?.let { captionParser.getHashTags(it) }.orEmpty(),
        mentions = caption?.let { captionParser.getMentions(it) }.orEmpty(),
        mediaType = mediaType,
        mediaUrl = mediaUrl,
        thumbnailUrl = getCorrectThumbnailUrl(
            thumbnailUrl = thumbnailUrl,
            mediaUrl = mediaUrl,
            mediaType = mediaType
        ),
        permalink = permalink,
        username = username,
        id = id,
        timestamp = convertIgTimeToInstant(timestamp)
    )

private fun getCorrectThumbnailUrl(
    thumbnailUrl: String?,
    mediaUrl: String,
    mediaType: String
): String =
    if (mediaType == IgMediaType.IMAGE.name || mediaType == IgMediaType.CAROUSEL_ALBUM.name) {
        mediaUrl
    } else if (mediaType == IgMediaType.VIDEO.name && thumbnailUrl != null) {
        thumbnailUrl
    } else { "" }

private fun convertIgTimeToInstant(dateTimeString: String): Instant {
    val createdAtTime = igDateFormat.parse(dateTimeString).time
    return Timestamp(createdAtTime).toInstant()
}