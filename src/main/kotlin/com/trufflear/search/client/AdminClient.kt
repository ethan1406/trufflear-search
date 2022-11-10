package com.trufflear.search.client

import com.trufflear.search.config.IgMediaFields
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
        val request = connectIgUserMediaRequest {
            instagramAuthCode = "AQDdPcscZ8C4SZBw5e340Hey3-xLxdN7AxpMJmWFTrlgaBik6X8AU-YpRKXym--lsfZJpqw48EkR2KYtnrbtDaUaIcjbfMxPfArOgCt4lLFz8EYNoViVdQtmgI1AAEHUVkbqATpY2zFh-9ti_Ua8__RYI-25crYo5Lg0n22khEYGfDzVx-pYsMF1APhW_vPVsKDWB-JCXUHfNBYNd1hr9yD1pcBi0QCH_KV8GFMGeQA0tg"
        }

//        val response = stub.connectInstagramUserMedia(request)
//
//        println("got it")
//        println(response)
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

suspend fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val metadata = Metadata()

    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiJlY2M4NTEzNC1kYjdiLTRjOWUtYWI0ZC0zNWIxYTlmODFiZGEiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiYWQ3YWJmMGUtNmVhYS00ZDViLTk1ODgtZDRhZTA3NDEyOWI0IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjgwNTgwNDAsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2ODA2MTY0MCwiaWF0IjoxNjY4MDU4MDQwLCJqdGkiOiI3ZTBkYTQzOS1iMGNkLTRkMmMtODUzMi03OWRkZDQ3ZTY2ZmYiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.v-o7Kx6oCmDVdxk4wNXbOp2cpRO2MB3m4vwLfPUZb2gZz3XCXzQVEoKF3LCzvOCkJ-4u7BUE5yDaH0_l_MRfqCPI3ZCmq7v11rhTMRIlQ4HdvYKimqjAZeTHlyRsRLtMOTWWyu50moiSuZMHWLqiZ8h3Tax2-jpLBCYM7TgsvHt1BY8e95_R4CcRXWbAXFAnAkBKAlGn1VyR8Aqkgx-L6lVi6FZ50OQAE2ScBfH8AYcZYA7n3M207BCerZthSxHCEQ2RO5eBw4lOwQf07nU5i2Uv3jJ9Iq9_hfrmKc1p93RVIpAC2aWdxLQD91sETokssEMfOuK3jsQZDgCTlRUtoA")

    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
        .usePlaintext()
        .executor(Dispatchers.IO.asExecutor())
        .build()

    val client = AdminClient(channel)

    client.signup()
}