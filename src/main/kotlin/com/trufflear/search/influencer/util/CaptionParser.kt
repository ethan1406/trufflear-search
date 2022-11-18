package com.trufflear.search.influencer.util

class CaptionParser(
    private val hashTagRegex: Regex,
    private val mentionTagRegex: Regex
) {

    fun getHashTags(caption: String): String =
        getHashtagList(caption).joinToString(" ")
            .replace("[^\\s-]#".toRegex()) {
                it.value[0].toString() + " #"
            }

    fun getMentions(caption: String): String =
        getMentionList(caption).joinToString(" ")
            .replace("[^\\s-]@".toRegex()) {
                it.value[0].toString() + " @"
            }

    private fun getHashtagList(caption: String): List<String> =
        hashTagRegex.findAll(caption)
            .toList()
            .map {
                it.value
            }

    private fun getMentionList(caption: String): List<String> =
        mentionTagRegex.findAll(caption)
            .toList()
            .map {
                it.value
            }
}