package com.trufflear.search.trufflesearch.influencer.mappers

import com.trufflear.search.config.IgMediaType
import com.trufflear.search.influencer.mappers.toPostDomain
import com.trufflear.search.influencer.network.model.IgPost
import com.trufflear.search.influencer.util.CaptionParser
import mu.KLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val username = "cooking_bobo"

class IgPostToPostMapperTest {

    private val captionParser = mock<CaptionParser>{
        on { getHashTags(any()) } doReturn ""
        on { getMentions(any()) } doReturn ""
    }

    private val logger = mock<KLogger>()


    @Test
    fun `to domain should map to current instant if timestamp is malformed`() {
        // ARRANGE
        val igPost = IgPost(
            caption = "caption",
            mediaType = IgMediaType.IMAGE.name,
            mediaUrl = "url1",
            thumbnailUrl = null,
            permalink = "link",
            username = username,
            id = "1",
            timestamp = "malformed timestamp"
        )
        // ACT
        val post = igPost.toPostDomain(captionParser, logger)

        // ASSERT
        assertThat(post.timestamp).isNotNull
    }

    @Test
    fun `to domain should map to empty caption, hashtag and mention if caption is null`() {
        // ARRANGE
        val igPost = IgPost(
            caption = null,
            mediaType = IgMediaType.IMAGE.name,
            mediaUrl = "url1",
            thumbnailUrl = null,
            permalink = "link",
            username = username,
            id = "1",
            timestamp = "malformed timestamp"
        )
        // ACT
        val post = igPost.toPostDomain(captionParser, logger)

        // ASSERT
        assertThat(post.caption).isEmpty()
        assertThat(post.hashTags).isEmpty()
        assertThat(post.mentions).isEmpty()
    }

    @Test
    fun `to domain should use media url as thumbnail if media type is image or album`() {
        // ARRANGE
        val thumbnailUrl = "thumbnail"
        val mediaUrl = "media"
        val igPost1 = IgPost(
            caption = "caption",
            mediaType = IgMediaType.IMAGE.name,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            permalink = "link",
            username = username,
            id = "1",
            timestamp = "malformed timestamp"
        )

        val igPost2 = igPost1.copy(mediaType = IgMediaType.CAROUSEL_ALBUM.name)

        val igPost3 = igPost1.copy(mediaType = IgMediaType.VIDEO.name)
        // ACT
        val post1 = igPost1.toPostDomain(captionParser, logger)
        val post2 = igPost2.toPostDomain(captionParser, logger)
        val post3 = igPost3.toPostDomain(captionParser, logger)

        // ASSERT
        assertThat(post1.thumbnailUrl).isEqualTo(mediaUrl)
        assertThat(post2.thumbnailUrl).isEqualTo(mediaUrl)
        assertThat(post3.thumbnailUrl).isEqualTo(thumbnailUrl)
    }
}