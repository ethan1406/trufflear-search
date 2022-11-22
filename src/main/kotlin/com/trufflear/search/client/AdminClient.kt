package com.trufflear.search.client

import com.trufflear.search.influencer.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import java.util.concurrent.TimeUnit

class AdminClient(private val channel: ManagedChannel) : Closeable {
    private val accountStub = InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineStub(channel)
    private val accountIgStub = InfluencerAccountConnectIgServiceGrpcKt.InfluencerAccountConnectIgServiceCoroutineStub(channel)
    private val publicStub = InfluencerPublicProfileServiceGrpcKt.InfluencerPublicProfileServiceCoroutineStub(channel)

    suspend fun signup() {
        val signupRequest = signupRequest {  }
        val getAccountRequest = getInfluencerPublicProfileRequest {
            username = "oyster_bamboo"
        }
        val request = connectIgUserMediaRequest {
            instagramAuthCode = "AQC7AKR8Fxp6YVuJTQ2XgsTIO35pC8J9G7HtOrSRsHQnjclsp5NlQJgzAbp87wieP02Riawn1XXepgYB-kCRbXXHfBjEv27gHzrv4P9lC7pTz0F9H3v0nJ3pN8CXZktLfQA53taN2BCx_h_2tRJWDbfSHAdmK8VL9X-bk35MigDDM4ik8O0BO-dSok88YN0kRA7VQUvLB73zF817VvlQ1KiZrci0rhXLoVusn7hlHoBIyQ"
        }


        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
        //val response = accountStub.signup(signupRequest)
        //val response = accountStub.updateProfile(updateProfileRequest)
        //val response = accountIgStub.connectIgUserMedia(request)
        val response = publicStub.getInfluencerPublicProfile(getAccountRequest)

        println("got it")
        println(response)
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

suspend fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val metadata = Metadata()

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiIwNTk5OTY3ZC03NmUzLTRkYjYtYjZkYi1mZGJlYWUxMDEyZTEiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiNDJiZWRiMTktNmFiNS00ZTIyLTljNWEtYzQ4MzI4YTI4ZWVmIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjkwNzA0NjIsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTA3NDA2MiwiaWF0IjoxNjY5MDcwNDYyLCJqdGkiOiJhNjNhMWU1Yy00YzQ1LTRiMDctYTc5OS1lMDY3MjA1YmI1NTEiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.sJqmc-WSHR2wkAbB_lCI-kytLttJuHTc-dZRnTrg2KexqR1abgE-fvqwT5vnaU525L7py1eRtRi1PWWh-DJcpFhq7fUjFwl4a8O7k2SEoeyHhzQQRAcvmGVG4KHSbfdgnS4i0VXuDiToWPoRsrGwqN1__w65tp4vM15EE3aikZFNDBAd-IOdBigM1IrhP7w0skx9kQTpyacjIpogEK8u7xuIYqrA4DmJzZ0QzxG37CQVT238aWoSp05qDGTaHbct7v6l-rvel7_3RxREH5h0Vrk_dj4uQD8nTQBXGA6oS9zrxXfNkQHW8guSQMFPLGAtD4hdrq1YVyqTVAoR9u4ssg")

    val channel = ManagedChannelBuilder
        .forAddress("api.trufflear.com", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}