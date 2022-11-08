package com.trufflear.search.client

import com.trufflear.search.influencer.InfluencerAccountServiceGrpcKt
import com.trufflear.search.influencer.signupRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import java.util.concurrent.TimeUnit

class AdminClient(private val channel: ManagedChannel) : Closeable {
    private val stub = InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineStub(channel)

    suspend fun signup() {
        val request = signupRequest {}

        val response = stub.signup(request)

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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiIyN2M4OTM1Mi00ZDc2LTRlMDEtOWU5Ny00ZmJjYWQwZDAxNGQiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiZmE2OTkyMDctYWQ2YS00OTYyLThhMjMtOTJiYmY4OGVhNDQzIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2Njc4OTIwNDksIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2Nzg5NTY0OSwiaWF0IjoxNjY3ODkyMDQ5LCJqdGkiOiI2MGQ3ZTZkMS0wMGZkLTQ1ZDgtYjYxMi03NTAxNWY0OWZiNDQiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.M9o0drsZF9oH98yIZh2jceTwHvqoKCmCGBJaQlrWASWzG8FvcHHNHt-G64XozqMr9Gh-qx0pvr6gTtZuOnVECNiMCHKsQLUywjYGH6YL680IRCXkGRw1MhK-pMy12V7pd5vI2QB67qOZ43CSrSERhh0NK6mWVgatp8mkvymXlrM6TBnMr_VrWunfCsF8Rz3sPRtP6rR3JuwiXSojzN8Vf2djTum6tMQ7j-vwMJVRPy0aUqfthPDKPh_25IOoJS_ZcSIaZmSo8O8qCDjWPzyIr7I8aWlKczKc48o_iKooxyfJQ1PklooweUmTkUFaOatgNMpHrOvl__RpXEWSZKLIDg")


    val channel = ManagedChannelBuilder
        .forAddress("api.trufflear.com", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .useTransportSecurity()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()
}