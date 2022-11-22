//package com.trufflear.search.client
//
//import com.trufflear.search.influencer.*
//import io.grpc.ManagedChannel
//import io.grpc.ManagedChannelBuilder
//import io.grpc.Metadata
//import io.grpc.stub.MetadataUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.asExecutor
//import java.io.Closeable
//import java.util.concurrent.TimeUnit
//
//class AdminClient(private val channel: ManagedChannel) : Closeable {
//    private val accountStub = InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineStub(channel)
//    private val accountIgStub = InfluencerAccountConnectIgServiceGrpcKt.InfluencerAccountConnectIgServiceCoroutineStub(channel)
//    private val publicStub = InfluencerPublicProfileServiceGrpcKt.InfluencerPublicProfileServiceCoroutineStub(channel)
//
//    suspend fun signup() {
//        val signupRequest = signupRequest {  }
//        val getAccountRequest = getInfluencerPublicProfileRequest {
//            username = "oyster_bamboo"
//        }
//        val request = connectIgUserMediaRequest {
//            instagramAuthCode = "AQC7AKR8Fxp6YVuJTQ2XgsTIO35pC8J9G7HtOrSRsHQnjclsp5NlQJgzAbp87wieP02Riawn1XXepgYB-kCRbXXHfBjEv27gHzrv4P9lC7pTz0F9H3v0nJ3pN8CXZktLfQA53taN2BCx_h_2tRJWDbfSHAdmK8VL9X-bk35MigDDM4ik8O0BO-dSok88YN0kRA7VQUvLB73zF817VvlQ1KiZrci0rhXLoVusn7hlHoBIyQ"
//        }
//
//
//        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
//        val response = accountStub.setProfileLive(setProfileLiveRequest { setProfileLive = true })
//        //val response = accountStub.updateProfile(updateProfileRequest)
//        //val response = accountIgStub.connectIgUserMedia(request)
//        //val response = publicStub.getInfluencerPublicProfile(getAccountRequest)
//
//        println("got it")
//        println(response)
//    }
//
//    override fun close() {
//        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
//    }
//}
//
//suspend fun main() {
//    val port = System.getenv("PORT")?.toInt() ?: 50051
//
//    val metadata = Metadata()
//
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiJlOTY3N2RhOC1hMjkzLTRkYzYtYjlmMi1iYzU0MWUzYjNlYjAiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiZGE0MDY1OTAtMDIzMS00YTY2LWE4MGItYTM0ZGRmYzFmZDRhIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjkwOTUyMTgsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTA5ODgxOCwiaWF0IjoxNjY5MDk1MjE4LCJqdGkiOiIwNmE2YzJjNS02MGJjLTQwYzktYjlmNi02OTRkMmRlMjY3NGIiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.BeXI9vfPx_1mTPJdfXIPLn2UGhpzZZ-m0lgY-uFDqt6hrlV39r-LMFqrAQLBQC-uwceXs4qnWA6JL6KWQd4ChptVWkMxayRalwXwDfZKVk2UZlkIOrLtKI03ZRZRNXoGEsx9nSm-jGp1bf3i72peAiVpY__YYj638yT_RxNjDCZd5nQsP9EO0kK1Q9bBn-1BrEvkg0XH3jPYElJclc842UQRYQisW_DujaZyloA6yshxQKk4GPNgtKhODDUo0qmlnyKJcrXcoFzC3V_di8qKpg0kCobkUlZ-I0i8E8OhL76IfREDJbQI4RsALMPaM9TRd65UC0UosjLfFjWQa8-PaQ")
//
//    val channel = ManagedChannelBuilder
//        .forAddress("localhost", port)
//        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
//        .usePlaintext()
//        .executor(Dispatchers.IO.asExecutor())
//        .build()
//
//    val client = AdminClient(channel)
//
//    client.signup()
//
//}