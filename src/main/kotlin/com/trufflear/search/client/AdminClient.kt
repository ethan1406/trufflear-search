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
//            instagramAuthCode = "AQDMP0lYrXeusvWLpyrWRhWPIY34Awzyh4iJ2r7KTi7WMX5e64F_gEhx9uIaWtekJ9QlXHDHEnY5bqMJUmHhXA4yQWfTI6ED4S9k9Al1MQJF2mTzSu2d1TDl2xYTj5ylETGmqPq2mGgx0SzEYONXbIv8pKOnyVpjLpqXJf4NjhEgrd8xDAoT9zsPm2cPTbt9qON279AqE8u_Q3NEKxoeA2Tus63wts3PxmSdERutzliqTA"
//        }
//
//        //val response = accountStub.signup(signupRequest)
//        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
//        //val response = accountStub.getProfile(getProfileRequest {})
//        //val response = accountStub.updateProfile(updateProfileRequest)
//        val response = accountIgStub.connectIgUserMedia(request)
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
//    val port = System.getenv("PORT")?.toInt() ?: 50052
//
//    val metadata = Metadata()
//
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJxeUpjc1ZKQUY1em9HeSt3WDhicXJScjZLbmQxTk5nRFpVU1RWV29uOGVvPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI0MzU1ODQ5Zi01OGFiLTRjYWMtOTM4Ni1kYmM1ZGM3MDA2NjIiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfa1NhaGl2cHVwIiwiY29nbml0bzp1c2VybmFtZSI6IjQzNTU4NDlmLTU4YWItNGNhYy05Mzg2LWRiYzVkYzcwMDY2MiIsIm9yaWdpbl9qdGkiOiJlYzcxOTcyYS0wZDc1LTQwNjUtODA1YS0yMDdjOGM5YzNlY2EiLCJhdWQiOiI5aGZxcnFrZTFuM2M0b2dyaWo3MzVsZjVpIiwiZXZlbnRfaWQiOiI4MTM1YjFhNi0zOTUzLTRmZjUtODBkYy03ZWY3M2MzMGQ2YmMiLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTY3MDQ3NzkyNiwibmFtZSI6IkV0aGFuIENoYW5nIiwiZXhwIjoxNjcwNDgxNTI2LCJpYXQiOjE2NzA0Nzc5MjYsImp0aSI6IjlhNjMzMmY0LTgwZTUtNDkwNy05ZGIyLWFiOGFjNTlmNjUzMSIsImVtYWlsIjoiZXRoYW4xNDA2QGdtYWlsLmNvbSJ9.lcDeEVvZ7B6VwA4yH-GMVG9z6C8Yiy5ZNaAAfTOf4Fn1nNTBaXRDv8TgQuRmgze6X8RqPy2aJBP_jnplt0XIGXnlYkadd9o5XP_wCRs2qux0zDinpYWC9-o8-ko_nX5IUlt4U5Pwe2oV1mkHznyyWMWvQD4mILdYCi_X7nqNADm732-3kqgFvapxDJVrQFm3hDPKxgm_pfb5Y8hzMbyHCPEskUd-WA7ywBg1iIQEI4nbZeM87AM0rou0U1xgSR_rOi7QO6RJjTnszz4yt5iEtKatJxDvOihJ5LpbdD9slwPwdC5D9ZZdk-J-BCw3VXnOeAqCeCZWK-GmVARzotCvfw")
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