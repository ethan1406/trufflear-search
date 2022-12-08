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
//            instagramAuthCode = "AQCqdcOyo332_-LYe_xK3SDYk60wIIwSpehd0hoog7mG4JGOdoU-hCJM-uTHdMsHsPZbnKm0REVY4YGn0IoIv5YPVeUqQKnurvXUV4Aby52R6ucpFvvuLnWJETJeWxsRT2W0zJmhK1CDS6r2HPrefbfn8HYlU9PE70Zd7LGnD4sjUs6bz_jjTc3hlk-zm3_7oCzkDxdZp6bUxL-_-zw_76XF6qurFb5JLyymk9Fq1A1iVA"
//        }
//
//
//        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
//        //val response = accountStub.getProfile(getProfileRequest {})
//        //val response = accountStub.updateProfile(updateProfileRequest)
//        //val response = accountIgStub.connectIgUserMedia(request)
//        //val response = publicStub.getInfluencerPublicProfile(getAccountRequest)
//
//        println("got it")
//        println(accountStub.signup(signupRequest))
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
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJxeUpjc1ZKQUY1em9HeSt3WDhicXJScjZLbmQxTk5nRFpVU1RWV29uOGVvPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI0MzU1ODQ5Zi01OGFiLTRjYWMtOTM4Ni1kYmM1ZGM3MDA2NjIiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfa1NhaGl2cHVwIiwiY29nbml0bzp1c2VybmFtZSI6IjQzNTU4NDlmLTU4YWItNGNhYy05Mzg2LWRiYzVkYzcwMDY2MiIsIm9yaWdpbl9qdGkiOiI5NzhhMzMyYi0xOTAyLTRlYzctODY0NC0wNWFjOTMyNGU5OWYiLCJhdWQiOiI5aGZxcnFrZTFuM2M0b2dyaWo3MzVsZjVpIiwiZXZlbnRfaWQiOiI0NTJjNzJhZC00ZTE4LTQ5NmQtYTYxZi1hYWE1MzUwNmJmMTkiLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTY3MDQ2NzU5MCwibmFtZSI6IkV0aGFuIENoYW5nIiwiZXhwIjoxNjcwNDcxMTkwLCJpYXQiOjE2NzA0Njc1OTAsImp0aSI6IjU2YjliNjIzLTQ3OTYtNDRjNy04MmEzLWZhZTRhMGI5OTQzMyIsImVtYWlsIjoiZXRoYW4xNDA2QGdtYWlsLmNvbSJ9.vPKXWmQzrBwN765es09uoI0qL7fiTq1RBVszaA9NRSmbaPU7tgX_xZaC2cv8ECct2O6FdL-M9CJo98tTPZIRiMGIIUpu253ekAwHgfD1kDq3HcwhE12lZ1LaXkyzJZb7IrDfmv7Ddl5lTVr5IZSEy5YfQhJsezHQN_vpEjT9msmPOp8KdKic6ZKL5xXFgCeupggklVJ9eNcwMPorQivhnKVigWr73NA55lohUQdM2cvrmyyXarUzhC3l0OoyRHgN777--FjvZbkGhnF2fKC97fbOisJ90yjnCF2VgFWnPjjo0fXS-fS5UY3a2f0Sptu2J6HnfcD-SqRDbeMaxapJ_g")
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