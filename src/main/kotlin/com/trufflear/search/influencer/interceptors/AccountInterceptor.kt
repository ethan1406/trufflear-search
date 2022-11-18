package com.trufflear.search.influencer

import com.trufflear.search.config.audience
import com.trufflear.search.config.issuer
import com.trufflear.search.config.jsonWebKeySet
import com.trufflear.search.influencer.domain.Influencer
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.kotlin.CoroutineContextServerInterceptor
import mu.KotlinLogging
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwk.VerificationJwkSelector
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.ErrorCodes
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.security.Key
import kotlin.coroutines.CoroutineContext

private val idTokenKey = "id-token"

data class InfluencerCoroutineElement(val influencer: Influencer): CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<InfluencerCoroutineElement>

    override val key: CoroutineContext.Key<InfluencerCoroutineElement>
        get() = Key
}

class AccountInterceptor: CoroutineContextServerInterceptor() {

    private val logger = KotlinLogging.logger {}

    override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        val idToken = headers.get(Metadata.Key.of(idTokenKey, Metadata.ASCII_STRING_MARSHALLER))
            ?: throw StatusException(Status.UNAUTHENTICATED)

        val influencer = validateTokenAndGetUser(idToken).getOrElse { throw StatusException(Status.UNAUTHENTICATED) }

        return InfluencerCoroutineElement(influencer)
    }

    private fun validateTokenAndGetUser(token: String): Result<Influencer> {
        val jsonWebSignature = getJsonWebSig()

        return runCatching {
            jsonWebSignature.compactSerialization = token
            val key = selectWebKey(jsonWebSignature)
            jsonWebSignature.key = key

            verifyClaimsAndGetInfluencer(token, key)
        }.onFailure { throwable ->
            logger.error(throwable) { "Invalid JWT" }
            if (throwable is InvalidJwtException) {
                if (throwable.hasExpired()) {
                    logger.error("JWT expired at  ${throwable.jwtContext.jwtClaims.expirationTime}")
                }
                if (throwable.hasErrorCode(ErrorCodes.AUDIENCE_INVALID)) {
                    logger.error("JWT had wrong audience  ${throwable.jwtContext.jwtClaims.audience}")
                }
            }
        }
    }
    

    private fun selectWebKey(signature: JsonWebSignature): Key {
        val jwkSelector = VerificationJwkSelector()
        val jsonWebKey = jwkSelector.select(signature, jsonWebKeySet.jsonWebKeys)
            ?: throw Exception("No web key matches with token header")
        
        return jsonWebKey.key
    }   
    
    private fun verifyClaimsAndGetInfluencer(token: String, key: Key): Influencer {
        val jwtConsumer = getJwtConsumer(key)

        val claims = jwtConsumer.processToClaims(token)
        return mapToInfluencer(claims.claimsMap)
    }

    private fun mapToInfluencer(claimMap: Map<String, Any>): Influencer {
        val name = (claimMap["name"] as? String) ?: throw Exception("name is not in the token")
        val email = (claimMap["email"] as? String) ?: throw Exception("email is not in the token")
        val emailVerified = (claimMap["email_verified"] as? Boolean) ?: throw Exception("email verification is not in the token")

        return Influencer(
            name = name,
            email = email,
            emailVerified = emailVerified
        )
    }

    private fun getJwtConsumer(key: Key) = JwtConsumerBuilder()
        .setRequireExpirationTime()
        .setExpectedAudience(audience)
        .setExpectedIssuer(issuer)
        .setVerificationKey(key)
        .setJweAlgorithmConstraints(algorithmConstraints)
        .build()

    private fun getJsonWebSig() = JsonWebSignature().apply {
        setAlgorithmConstraints(algorithmConstraints)
    }

    private val algorithmConstraints = AlgorithmConstraints(
        AlgorithmConstraints.ConstraintType.PERMIT,
        AlgorithmIdentifiers.RSA_USING_SHA256
    )
}