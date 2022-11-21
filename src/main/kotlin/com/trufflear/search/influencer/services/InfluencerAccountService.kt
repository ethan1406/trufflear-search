package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.InfluencerAccountServiceGrpcKt
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.SignupRequest
import com.trufflear.search.influencer.SignupResponse
import com.trufflear.search.influencer.UpdateProfileRequest
import com.trufflear.search.influencer.UpdateProfileRequestResponse
import com.trufflear.search.influencer.network.service.CollectionCreation
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.InsertResult
import com.trufflear.search.influencer.repositories.SearchIndexRepository
import com.trufflear.search.influencer.signupResponse
import com.trufflear.search.influencer.updateProfileRequestResponse
import io.grpc.Status
import io.grpc.StatusException

import mu.KotlinLogging

import kotlin.coroutines.coroutineContext

internal class InfluencerAccountService(
    private val influencerRepository: InfluencerProfileRepository,
    private val searchIndexRepository: SearchIndexRepository
) : InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun signup(request: SignupRequest): SignupResponse {
        logger.debug { "user signing up" }
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        when (influencerRepository.insertInfluencer(influencer)) {
            is InsertResult.Unknown -> throw StatusException(Status.UNKNOWN)
            is InsertResult.UserAlreadyExists -> throw StatusException(Status.ALREADY_EXISTS)
            is InsertResult.Success -> Unit
        }

        when (searchIndexRepository.createSearchCollectionFor(influencer.email)) {
            is CollectionCreation.Failure -> throw StatusException(Status.UNKNOWN)
            is CollectionCreation.Success -> Unit
        }

        return signupResponse { }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): UpdateProfileRequestResponse {
        logger.debug { "user updating profile" }

        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)


        influencerRepository.checkIfInfluencerExists(influencer.email)
            ?: throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))

        influencerRepository.updateInfluencerProfile(
            influencerEmail = influencer.email,
            title = request.profileTitle,
            professionCategory = request.professionCategory,
            description = request.bioDescription
        ) ?: throw StatusException(Status.UNKNOWN)

        return updateProfileRequestResponse { }
    }
}