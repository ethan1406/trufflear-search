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
//
//    suspend fun signup() {
//        val signupRequest = signupRequest {  }
//
//        val request = connectIgUserMediaRequest {
//            instagramAuthCode = "AQAjBmBu0A4s_lnW7dX64Co5g2193X5IRW_350gMvRGqO34Y2_SkOJ3Qj7G2o8bx6xjRLBDx1bmi3xljh4_3X4dXO0z3eb7O_8LSeb_R8A2J8E00VVL1mMR7t8ov0oCq56OofCqEJnPKuHFIeJ-YJapoAAB1KLpg3End-y17ZW6EfL6Fs7hx3U0cnFmyUj-5TOb4hT5bezbpQyJbhPRskvOUA3Bw6aH_L-QC2yjJBPGX3Q"
//        }
//
//        //val response = accountStub.signup(signupRequest)
//        val response = accountIgStub.connectIgUserMedia(request)
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
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiIyYjBjNjhhMi1kYjExLTQ1MjktYWFmYS02YjM1YjliMmJmMTMiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiYmU2OWUyZGUtMWIxYi00Zjk4LTgxYzQtMDczMjZiZWIxN2Q5IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjgyOTUwMjUsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2ODI5ODYyNSwiaWF0IjoxNjY4Mjk1MDI1LCJqdGkiOiI0MzA5NWQ0Yi1kMGI4LTQ4MmQtOTM2Mi00NTc3ZWI0YjEwMGUiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.DKXoc5-0gdQO3fL41-50PzM631kWoBZfiXiE5kTC62beRbIiqVcyO_4pIvuQiv6NBeAE46IBZerbp6TZbz8sqFP8C2nzrT2MiDCo09F_5_hd_cM2AcTJi05-mFLDZK5RbQVYzRoAJ1oKebcM0kiLk02Iw1VfJePStuQGjYL3D2MneengbVE28OXGO8AeKkwirVciUf8wkg5vtsZL6bBsZ0wJg9jsEMJwwN0EZr3vnAcA7Qwj9e1V2VbLKUxUCtyGhf4CCiIfpTcxra265h21m7rCMq3qj0b9GkotUwZli3HdzxT7Z9m1X1n0U23CwMDMl-u1zzNLLXKJUvX0C1mJWQ")
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