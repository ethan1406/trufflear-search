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
//            instagramAuthCode = "AQCpvwcJt44LXHfC-geAf83f5sC5cRNaBZN4iiVuo7oCW4AHxDMZqFBj1n4BlrOJrlJXvey7RA4qFuV6FE2ET5Bj5mL8gyfK5b4fKIKoe4stmtDlvit3muZD9HCoyGtt_1JkhosqW4VEKMnQXlkGveo1Zq2l8-DAYCnbe4ph9UTxVBs8qz-UDoJK9G093-7_oFA45cvlt37tnyngIFeGAgbYXniwYRsSE2_jkZGIdaxOHQ"
//        }
//
//
//        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
//        //val response = accountStub.getProfile(getProfileRequest {})
//        //val response = accountStub.updateProfile(updateProfileRequest)
//        //val response = accountIgStub.refreshIgUserMedia(refreshIgUserMediaRequest {  })
//        val response = publicStub.getInfluencerPublicProfile(getAccountRequest)
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
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiJlNjg0YWNlOS0zM2E5LTRjYTItODFlMC0xNmJjMDA0OGJjMDEiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiMzg2YTJmNzgtMTgzYS00MjY3LWIyODQtZGU2ZjlkNTEzYjY0IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2Njk4NTIxNTQsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTg1NTc1NCwiaWF0IjoxNjY5ODUyMTU0LCJqdGkiOiJiNDIxZDM4Yi1kYjk4LTRmMzEtOTVkMi00ZTQxYWUxNDM5YWYiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.32FEymdtU9_wpK_8nbSWVj_QQ-4JFpctEihRKdN4njT2IF9fRQRXl4bztGE1PsAQ44Hijd1nHSmHkjidRtz8HA-h35hXptDj2m_FtO4A6ExDVorY1hCkCMoUg74BEPkjFMrLdYWvbRCUPLL8AjfCvGGVtFa2ohY-QQKyoSrn-sU2kuIpC8FABRXXqYXlqu_16u2asu-oF50xgugj0rd_Uom3AOD6NE8vTjHzMDy1TFpHGnhBzQ4rjQnCbHx6d84r7yRh_CGRxiuuZRsKVje2Br9fNanioBQoxQZXPFydV2npF0mnaKIL8KJn68t84vg9VSRHCp2am6a2Xx70BP8DhA")
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