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
//            instagramAuthCode = "AQBToRqjG3ZufTUdQzsuggcDsuvCpEH80ex_IDovbhw9aEAiQn-mQlFJEMwZrs4hRK5tzlC9W1Y6Po8yLtKwVhkcKAR0nt-X5LIu7TN-V9FXjehyrTxgcmbhDUx0EALuOTE1wpv6kaNNlypAJzWot1mbxx6STcEzbdJR8CKmFMxbYyqMBDKVHIwheIho11wW25nvV1NZ03deWjf7B8OOBi9CEIRqHlLcsGRQndSEg_sMrg"
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
//    metadata.put(Metadata.Key.of("id-token", Metadata.ASCII_STRING_MARSHALLER), "eyJraWQiOiJ3R1d3NVRVRVdMRW9na2dSTUJOcGE4dnlLVjV5ZWlaT2h6TWRJV1FNdVJFPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI5MWFkZWZiZS1hODhhLTRmYjctOWE2MS00NDRkNTNhYzMzYTMiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMS5hbWF6b25hd3MuY29tXC91cy13ZXN0LTFfNEVROERzNkpjIiwiY29nbml0bzp1c2VybmFtZSI6IjkxYWRlZmJlLWE4OGEtNGZiNy05YTYxLTQ0NGQ1M2FjMzNhMyIsIm9yaWdpbl9qdGkiOiI2MjhhYWViYS0zOGNmLTQ3ODYtOTRmMy03NDdlNDM2YzVmYWQiLCJhdWQiOiIyYXVwMWR2cTFrYTUyMmlqdGVtcmEybzhzcSIsImV2ZW50X2lkIjoiMjJjNjNkMTYtNGQ0YS00OWI3LWJlODAtMDAzNzg2NDQ4NDdjIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjgxMjE2NDcsIm5hbWUiOiJFdGhhbiBDaGFuZyIsImV4cCI6MTY2ODEyNTI0NywiaWF0IjoxNjY4MTIxNjQ3LCJqdGkiOiJmZjIyNjBjNi05MmZjLTRlMzUtODM4My1kOTI2N2FkZjNjNmUiLCJlbWFpbCI6InRlYW1AdHJ1ZmZsZWFyLmNvbSJ9.kEIs5Y0Deuwe0sEhDzaMR_svyM2m0F153ezWwkKrpKKChmJ69RUYAoBvYpSdQ913iC1HjHuD6me5DWqAFWNPkiB6c6BzEJKE4pCcummOISaIo944yjHxkXJrR3GmVKzuws_TYwrLIrAKklNtxSSSjU97TnV9b0THX4GoWSSrHWsK50zTqAV6fbGxAi8ZfhZory-g9aHgcrn2WRkzZfOXaVbgS7t2jJF9JqjZ6VNLHbVUFJ6Gaa7KVi-ixRY2rqlPvAVLOM3lwIOEWoTJTXpfLVJzVv8S0P8ahL69Eg-KB65ZOh5QpDl83voD6L_fUEJJUACvMdEglgUJ1Qdf1ljYzA")
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