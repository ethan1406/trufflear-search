package com.trufflear.search.influencer.mappers

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.domain.Post
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.igDateFormat
import mu.KLogger
import java.sql.Timestamp
import java.text.ParseException
import java.time.Instant

internal fun IgPost.toPostDomain(captionParser: CaptionParser, logger: KLogger) =
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
        timestamp = convertIgTimeToInstant(timestamp, logger)
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

private fun convertIgTimeToInstant(dateTimeString: String, logger: KLogger): Instant =
    try {
        val createdAtTime = igDateFormat.parse(dateTimeString).time
        Timestamp(createdAtTime).toInstant()
    } catch (e: ParseException) {
        logger.error(e) { "error parsing date" }
        Instant.now()
    }
