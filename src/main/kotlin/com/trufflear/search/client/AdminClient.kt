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
        val response = accountStub.getProfile(getProfileRequest {})
        //val response = accountStub.updateProfile(updateProfileRequest)
        //val response = accountIgStub.connectIgUserMedia(request)
        //val response = publicStub.getInfluencerPublicProfile(getAccountRequest)

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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiI3YjllNTA0Yi0zNTNhLTQ5NDctOWJiMy1hMTMyYmVhNjFkOTQiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiNjJiYzJlOGUtYWU0Ny00OWQ4LTgxNTktYmZiZDVlYzgxMTVjIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjkxMDExODYsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTEwNDc4NiwiaWF0IjoxNjY5MTAxMTg2LCJqdGkiOiI5NzQwM2QzNy1iYTYyLTQ4MzAtYmRiNS1mOTllZWRhNWEwM2EiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.Xyp42Vq6GtQti3-O_T8gkW4DFcrJQ631-JQiguyCvEKxynFSoE7wlnKfJw_uaa9ZlXmfHoJMWNUbjTvcyusoxsnFPFl5Wwt3DasxXBkV7RbICOR5kz3GDkd4OxzrnoWSwjxRAs1CVqj35ESAn81tl-HM_D1CiEbNj8skAWqBXeqJX4Pyt30e6XMtK1W_dBplUPbqjq8zazv53__LWW98RoiP9MMkqMsz8GYlBsRPNUv8v1iOOmCQWyAiv6KXTWAzFBisL_jQsSkkEjE1ZbHB9Gc1F2sMSt8KNEsRT9q6-Lt0jDvIvBjUufnLVJcnRH91MFnW45WZDhSC2sJXD4Y9ug")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}