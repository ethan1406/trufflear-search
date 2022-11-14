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

    suspend fun signup() {
        val signupRequest = signupRequest {  }

        val request = connectIgUserMediaRequest {
            instagramAuthCode = "AQDBGGGIttbqjYCkCQh7xUo9iLCC22IaCglbyoJgZxPucisy8tz44dXGMIrGS1rwEKwaqNiPZXh568YkrQIoWgOav0WrDjgC-NS3KOCrWoE1OAMJB74zvr5diGKa-M3pzpbQeOE3BRW0TUoBXP4ibl3hlwdV26oeCqOyMofozD_M8O8-7fWzLvZUP_gruop7NSsGqnlP8V3UTj4FWxnsCeqcR6jAcGDogqs8kyXzyuJQJw"
        }

        //val response = accountStub.signup(signupRequest)
        val response = accountIgStub.connectIgUserMedia(request)

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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiI3OWMyZTljMi04MDc1LTRkYmEtOWM4OS0yMzEzMDdhNzE0NWYiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiMGJlMDFmYmYtMjgwZi00NTMzLWI5ODUtMzg5NzFjMTBmYzdhIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjgzODI4NTIsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2ODM4NjQ1MiwiaWF0IjoxNjY4MzgyODUyLCJqdGkiOiJjZjU2NTA2Mi0wZDYxLTQ4NGEtYjEwNC02YmIwZTY3YjYzZDUiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.wUTvaXiI4bohQLwZ-TxD-pKcUmHwUIj2utl7UAygTMNpRCuaQqGM_4_Bdjm2qwcqIr6ktgQ_u9DqCa6WimneM23hqjv-K5slITBtrCzQeE3t1UNq7g6_UtYnTqrGV0R1tzmjSn8ITq4Oa2_0z9o-r3jVRIJM8lUFUIpc1e2He2gIoEZGMysSKOVrH3dG-aHIUNa70mCNB-fnbLBcmhSStgyucc39yoYx62kK-Wv1DRDJn6Lf80AqnxIM9G-Mr2qRai-IfVTfAkeceF0f_6YsYE3_Q_G1ZrxzTiQBFQEHiaTtHi-pbkn_JPnNQZahv9dH0_Ez-5niCyAkt361qKZVeQ")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}