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
            instagramAuthCode = "AQCqdcOyo332_-LYe_xK3SDYk60wIIwSpehd0hoog7mG4JGOdoU-hCJM-uTHdMsHsPZbnKm0REVY4YGn0IoIv5YPVeUqQKnurvXUV4Aby52R6ucpFvvuLnWJETJeWxsRT2W0zJmhK1CDS6r2HPrefbfn8HYlU9PE70Zd7LGnD4sjUs6bz_jjTc3hlk-zm3_7oCzkDxdZp6bUxL-_-zw_76XF6qurFb5JLyymk9Fq1A1iVA"
        }


        //val response = accountIgStub.getIgAuthorizationWindowUrl(getIgAuthorizationWindowUrlRequest {  })
        //val response = accountStub.getProfile(getProfileRequest {})
        //val response = accountStub.updateProfile(updateProfileRequest)
        val response = accountIgStub.connectIgUserMedia(request)
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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiI5ZGM2Njc5NC1hOWNlLTQ5ZTEtODM0YS03NDQ3YTM5NDM1ZTEiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiMjNkM2E4MWQtYWM2YS00NmUzLTlkYmItNWNlM2IxZjU4ZmM0IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2Njk4NDQ1MTMsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2OTg0ODExMywiaWF0IjoxNjY5ODQ0NTEzLCJqdGkiOiIzNTYyZGIzMy0zMzg4LTRlMmEtYWI4YS1mMjIyZjFlM2IyYWYiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.APo4yLRize7JaHGOgcF-GCFym1Ld8Z3nW0Wr5ge7PRxURx4IDkNvYWhcIAk-VFZK0hNzZ_wYUMHwWewKZGu6ojowbRAjXaMCblmwiospTAgf3oa50_3gUMYbEJ18GXIo1Rm5mM9k5gwkmfDDsIrLqkMkWtJypukmcLJFG-DBfbxTv81YLPnp8svcCKrqj0uBXSIhwuSwbx9YfdeQKqlNY6VZOevXbK6AE63V6LIUPhiMVJzBDUTLfcSucK4TAb_yCUkBPOu_7OI_Alh9Bb5_ImXxOfAteFi0YCx62XnWaYdqg2vNMLv2-TxqLVsWC2WAB1Q3iJodEAV6VHk8LM8dvw")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()

}