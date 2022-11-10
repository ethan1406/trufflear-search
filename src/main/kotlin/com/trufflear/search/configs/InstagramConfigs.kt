package com.trufflear.search.configs

internal const val clientId = "581169537105981"

internal const val appSecret = "faa9243a2cda424e7287fd9dbcc24e69"

internal const val redirectUrl = "https://www.trufflear.com/auth"

internal const val apiInstagramSubdomain = "api.instagram.com"

internal const val authPath = "oauth/authorize"

internal object InstagramApiParams {
    const val clientId = "client_id"
    const val redirectUri = "redirect_uri"
    const val responseType = "response_type"
    const val scope = "scope"
}

internal object InstagramResponseTypeFields {
    const val code = "code"
}

internal object InstagramAuthScopeFields {
    const val userProfile = "user_profile"
    const val userMedia = "user_media"
}
