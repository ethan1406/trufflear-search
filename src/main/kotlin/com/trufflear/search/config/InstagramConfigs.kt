package com.trufflear.search.config

internal const val clientId = "581169537105981"

internal const val appSecret = "faa9243a2cda424e7287fd9dbcc24e69"

internal const val redirectUri = "https://www.trufflear.com/auth"

private const val apiIgSubdomain = "api.instagram.com"

internal const val authPath = "oauth/authorize"

internal val igGraphSubdomainBaseUrl = "$https_host://graph.instagram.com/"

internal val igApiSubdomainBaseUrl = "$https_host://$apiIgSubdomain/"

internal object IgApiParams {
    const val clientId = "client_id"
    const val clientSecret = "client_secret"
    const val grantType = "grant_type"
    const val redirectUri = "redirect_uri"
    const val responseType = "response_type"
    const val scope = "scope"
    const val code = "code"
    const val accessToken = "access_token"
    const val fields = "fields"
}

internal object IgMediaFields {
    const val caption = "caption"
    const val mediaType = "media_type"
    const val mediaUrl = "media_url"
    const val permalink = "permalink"
    const val thumbnailUrl = "thumbnail_url"
    const val timestamp = "timestamp"
    const val username = "username"
    const val id = "id"
}


internal object IgCodeGrantType {
    const val authCodeGrantType = "authorization_code"
    const val exchangeTokenType = "ig_exchange_token"
}

internal object IgResponseTypeFields {
    const val code = "code"
}

internal object IgAuthScopeFields {
    const val userProfile = "user_profile"
    const val userMedia = "user_media"
}
