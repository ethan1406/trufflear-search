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
//        //val response = accountStub.getProfile(getProfileRequest {})
//        //val response = accountStub.updateProfile(updateProfileRequest)
//        val response = accountIgStub.refreshIgUserMedia(refreshIgUserMediaRequest {  })
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
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiIzNzA5NTAwNS00NDliLTQ5ZjUtYTQ5Ny03MjRkMmMzYWQ3MDciLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiMTIzNjQ0NDctM2FlNy00ZmY4LWI2YmItMjNkNDg5MTM5M2QyIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjkzMjQ5MjQsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTMyODUyNCwiaWF0IjoxNjY5MzI0OTI0LCJqdGkiOiI5NzM2ZGM5Zi0wMzgwLTQ4NTctODAyZS02MDJiZDY4YTdhZTQiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.RPZ7jbSrNy1qAtEQr4KWZQwP27Hhzcm4zLsl-zpy8s0ULv95iDSb9QZUeSE9b7yIM_89hco3nWogeWBtSqNzPu3JdaJwNdUerYrrZwRiBJ9VUwpDy7GgIP0NAvUi9FTlZvMrR2hw4GXAuS7fOMMy0o4p1oiMRz1SN2JS3_QhpafthQHqSLYFwjdrJtarLN-pG5IECbI3R8gbvIcFarps8GZmN2dHoMjNXjtpsyi0VLr7WkSY5xPNu0FHtNvdztiNVwxxTYVbtmB0qxY-tizYBEH9VpX4f3Mv4OfkOOyjSSXglKzw9uydVsg-8qIxldHyUtI40ZjXg0sIOHYPkAdMkQ")
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