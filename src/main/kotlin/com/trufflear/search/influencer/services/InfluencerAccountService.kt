package com.trufflear.search.influencer.services

import com.trufflear.search.influencer.GetProfileImageUploadUrlRequest
import com.trufflear.search.influencer.GetProfileImageUploadUrlResponse
import com.trufflear.search.influencer.GetProfileRequest
import com.trufflear.search.influencer.GetProfileResponse
import com.trufflear.search.influencer.ImageUploadSuccessRequest
import com.trufflear.search.influencer.ImageUploadSuccessResponse
import com.trufflear.search.influencer.InfluencerAccountServiceGrpcKt
import com.trufflear.search.influencer.InfluencerCoroutineElement
import com.trufflear.search.influencer.SetProfileLiveRequest
import com.trufflear.search.influencer.SetProfileLiveResponse
import com.trufflear.search.influencer.SignupRequest
import com.trufflear.search.influencer.SignupResponse
import com.trufflear.search.influencer.UpdateProfileRequest
import com.trufflear.search.influencer.UpdateProfileRequestResponse
import com.trufflear.search.influencer.getProfileImageUploadUrlResponse
import com.trufflear.search.influencer.getProfileResponse
import com.trufflear.search.influencer.imageUploadSuccessResponse
import com.trufflear.search.influencer.influencerProfile
import com.trufflear.search.influencer.network.service.CollectionCreation
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.InsertResult
import com.trufflear.search.influencer.repositories.ProfileRequest
import com.trufflear.search.influencer.repositories.ProfileResult
import com.trufflear.search.influencer.repositories.SearchIndexRepository
import com.trufflear.search.influencer.setProfileLiveResponse
import com.trufflear.search.influencer.signupResponse
import com.trufflear.search.influencer.updateProfileRequestResponse
import io.grpc.Status
import io.grpc.StatusException

import mu.KotlinLogging

import kotlin.coroutines.coroutineContext

internal class InfluencerAccountService(
    private val influencerRepository: InfluencerProfileRepository,
    private val searchIndexRepository: SearchIndexRepository,
    private val storageService: StorageService
) : InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override suspend fun signup(request: SignupRequest): SignupResponse {
        logger.debug { "user signing up" }
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        when (influencerRepository.insertInfluencer(influencer)) {
            is InsertResult.Unknown -> throw StatusException(Status.UNKNOWN)
            is InsertResult.UserAlreadyExists -> {
                throw StatusException(Status.ALREADY_EXISTS)
            }
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


        if (influencerRepository.userExists(influencer.email).not()) {
            throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))
        }

        influencerRepository.updateInfluencerProfile(
            influencerEmail = influencer.email,
            title = request.profileTitle,
            professionCategory = request.professionCategory,
            description = request.bioDescription
        ) ?: throw StatusException(Status.UNKNOWN)

        return updateProfileRequestResponse { }
    }

    override suspend fun getProfile(request: GetProfileRequest): GetProfileResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        val profileRequest = ProfileRequest.WithEmail(influencer.email)
        return when (val result = influencerRepository.getPublicProfile(profileRequest)) {
            is ProfileResult.Unknown, ProfileResult.NotFound -> throw StatusException(Status.UNKNOWN)
            is ProfileResult.Success -> {
                getProfileResponse {
                    influencerProfile = influencerProfile {
                        profilePicUrl = storageService.getUrl(result.profile.profilePicObjectKey).orEmpty()
                        profileTitle = result.profile.profileTitle
                        categoryTitle = result.profile.professionCategory
                        bioDescription = result.profile.bioDescription
                        email = result.profile.email
                    }
                    isProfileLive = result.profile.isProfileLive
                }
            }
        }
    }

    override suspend fun getProfileImageUploadUrl(request: GetProfileImageUploadUrlRequest): GetProfileImageUploadUrlResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        val profileRequest = ProfileRequest.WithEmail(influencer.email)
        return when (val result = influencerRepository.getPublicProfile(profileRequest)) {
            is ProfileResult.Unknown, ProfileResult.NotFound -> throw StatusException(Status.UNKNOWN)
            is ProfileResult.Success -> {
                val username = result.profile.username
                if (username.isEmpty()) {
                    throw StatusException(Status.PERMISSION_DENIED.withDescription("user must connect with instagram first"))
                }

                val presignedUrl = storageService.getProfileImageUploadUrl(result.profile.username)
                    ?: throw StatusException(Status.UNKNOWN)

                getProfileImageUploadUrlResponse {
                    url = presignedUrl
                }
            }
        }
    }

    override suspend fun succeedInImageUpload(request: ImageUploadSuccessRequest): ImageUploadSuccessResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        val profileRequest = ProfileRequest.WithEmail(influencer.email)
        return when (val result = influencerRepository.getPublicProfile(profileRequest)) {
            is ProfileResult.Unknown, ProfileResult.NotFound -> throw StatusException(Status.UNKNOWN)
            is ProfileResult.Success -> {
                val username = result.profile.username
                if (username.isEmpty()) {
                    throw StatusException(Status.PERMISSION_DENIED.withDescription("user must connect with instagram first"))
                }
                storageService.saveProfileImageKey(username) ?: throw StatusException(Status.UNKNOWN)

                imageUploadSuccessResponse { }
            }
        }
    }

    override suspend fun setProfileLive(request: SetProfileLiveRequest): SetProfileLiveResponse {
        val influencer = coroutineContext[InfluencerCoroutineElement]?.influencer
            ?: throw StatusException(Status.UNAUTHENTICATED)

        if (influencerRepository.userExists(influencer.email).not()) {
            throw StatusException(Status.PERMISSION_DENIED.withDescription("user must sign up first"))
        }

        influencerRepository.setProfileLive(
            setLive = request.setProfileLive,
            influencerEmail = influencer.email
        ) ?: throw StatusException(Status.UNKNOWN)

        return setProfileLiveResponse { }
    }
}