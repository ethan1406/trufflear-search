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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiJiZGRhZTZkMy00MzRkLTQ4M2MtYTdmNC1jYTVhZjM3YWY0ZDciLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiNWU5ZGYwN2MtM2Q3NC00YTMxLThjNWItZGY0MTA3ZTQ5ZDc3IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjkyNzcxODksIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTI4MDc4OSwiaWF0IjoxNjY5Mjc3MTg5LCJqdGkiOiJmNDA2ZmZiNi0yOTkyLTQ5ZWItOWQyOC0zZGYyMDZiYmVmZGIiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.ERmOVrXh4C67n0qF8bNlx_aChu-REL-8lSW1UdRbzpkFh4bktTEwmWLsMFiiGNqhqVmrpw8YVXuKso1Grk-Ijlem_bU-L5CGF1SXDtyrERcGuc6heMENJL0xcaPbxA8ms52cMNe-9CkFa0fgPdePc1Vdsmrrn7iAM_FVXprxjI1rPoXcIQntxUHLIjwnyTy38WNY7Tk1HTbUx9tkPgn2vMxJB9imfU1QeQwHVnXRw5R86__IyHwNsL3jsHRLgZdYFvlGHBzBcw0pxSh8LTEng-JBLPd-8CVHmObnqBOo0GX03Iqt-lJI_xkcSffQsz3fjKhG2x18R87jysr6SuvcHg")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}