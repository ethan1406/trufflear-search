package com.trufflear.search.util

import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.hashTagRegex
import com.trufflear.search.influencer.util.mentionTagRegex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CaptionParserTest {

    private val parser = CaptionParser(hashTagRegex, mentionTagRegex)


    @Test
    fun `parse hashtags should parse correctly`() {
        // ARRANGE
        val caption = "#test-1 #test_2\r超快牛肉麵#這款食譜一#test3定要珍藏！  \uD83D\uDCCD材料②洋蔥  \uD83D\uDCCD步驟 ①" +
                "將kindfo熱即可。食譜\uD83D\uDC49 @cook_by_yourselfff ——— #懶人料理\n#懶人食譜#test-4"

        // ACT
        val hashTags = parser.getHashTags(caption)

        // ASSERT
        assertThat(hashTags).isEqualTo(
            "#test-1 #test_2 #這款食譜一 #test3定要珍藏！ #懶人料理 #懶人食譜 #test-4"
        )
    }

    @Test
    fun `parse mentions should parse correctly`() {
        // ARRANGE
        val caption = "@test#美食推薦#餐廳推薦 @beefnoodle@testing123@西班牙海鮮燉飯@tapas#paella#美食日記"

        // ACT
        val hashTags = parser.getMentions(caption)

        // ASSERT
        assertThat(hashTags).isEqualTo(
            "@test @beefnoodle @testing123 @tapas"
        )
    }

}