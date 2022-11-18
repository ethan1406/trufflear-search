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
            username = "oyster_bamboo2"
        }
        val request = connectIgUserMediaRequest {
            instagramAuthCode = "AQCW5vADY2sytFAN9nuYTjqSkHYc3x9vNO0oxd6Ulrce5b9UAvmVEduo-L7cq1sohS32OvEdVr-XAGbJm866a9nequmhAIR54x3MRccTjpOR4va71_SmpsVVDXIB057tC5vQPSXG51GkW8n6ERu2Ca4wfrZIiBJ8WDUjJpqfxO7QV0AP6Z1fFDrn8eKw2LKIM2oZ2X2p33aUINulKIxY9zFj8_inEz9WJFfdf2kFciuPmQ"
        }


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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiIxNjRlYzM5YS1iYTg3LTQ5MGUtYjg5Yi0yYWRlYTlhZTUxMGYiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjE2NGVjMzlhLWJhODctNDkwZS1iODliLTJhZGVhOWFlNTEwZiIsIm9yaWdpbl9qdGkiOiJjZDk4NTRhZC01MjBjLTQxZGItYTE3NS0xNjE0ZWE4ZGJkZmUiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiYWEyMjVjYWMtNjE1My00NzI2LTljY2MtNDU5M2IwMDAwMzc2IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2Njg4MTI1NDYsIm5hbWUiOiJZYWZlbmcgV2FuZyIsImV4cCI6MTY2ODgxNjE0NiwiaWF0IjoxNjY4ODEyNTQ2LCJqdGkiOiIxZmYxMGY3ZS1lZGVmLTQyYjktOWIyNS03YzZhNTI2MDdjYTIiLCJlbWFpbCI6ImVjaGFuZ0BseWZ0LmNvbSJ9.O5yYDW4zbxUR0qfgMJnMtVdy02YAsFAju5yE1Pr9ytBxPptKMX7NrVHbV6c_MnHgphjKmIrmSmNYjYXCnK6XnbxPDQkpMEglOJEjs5HW0EeFcakjl96W6QliBttvfzXbwBuEzHlyz68WS7sz2Qz62q9VzVI-sKjp0hr_RpxWHFQiDOW4oG7bNzcpYiCVPf72TDTMXnl-0B23O5b56L3_Nj0nadziVSStxF4Pj0eHetEbj_bgacV7_HMyWZv6-dMpo7uAq_8gx5wrRnTsPvu_tA0Fe9hVv04CV8GRp0ej4mxMguN6hfjj7nygE51LhykbpALZsmuZu7cvvUM_jcYI4A")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
       // .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}