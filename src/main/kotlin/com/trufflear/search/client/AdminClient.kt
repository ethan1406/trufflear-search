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
    private val stub = InfluencerAccountServiceGrpcKt.InfluencerAccountServiceCoroutineStub(channel)

    suspend fun signup() {
        val request = updateUsernameRequest {
            username = "Cooking bobo"
        }

        val response = stub.updateUsername(request)

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

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiIyMDBlNzE1Ny00ZjkxLTRiMDktYjUwZi05MWE4YWI5N2EyNjIiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiNzUyZWJkOWItYTZiOS00Y2M0LWI2N2YtZmYzNTBmNmUwMWZjIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjgwNDczNjUsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2ODA1MDk2NSwiaWF0IjoxNjY4MDQ3MzY1LCJqdGkiOiIyM2I2OTc1Yy1lYmFiLTQ4Y2EtODFlYy01YWI4ZDhjOWYyOTIiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.uPH_hcygWJyRq6gsuSxKStchiqR_v-QCoIMe2ztPJTdwcGGA14PPftXhvSfsZWWH4M3UmqWpTGZ_h2mupcRA_-2QG19DpvwmhZMPkV-ubzoY8449NbV-xXj5W3798rTYUIhNJgiLXAG4GX9qgfu5ixvQSZtn1f8TJhLqdS1jIwZxfOMVbTLWkDqD_r_Wdz3t4Rx89pQ-wngHAqbW0ZcK6FZOPd1V_dH59cbW1DpSWB9gBr2JqgfMEkpxL2oYXuWFwFk4LqOWmSSHQBHPl5h7Gk4Vosv2BLWs3LZ_hsK1c64mE0KS4gm-AAOhzZeU6HXMUcd2XpQ7wYzXAHmFhq6O8A")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()
}