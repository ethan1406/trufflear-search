package com.trufflear.search


import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.trufflear.search.config.igApiSubdomainBaseUrl
import com.trufflear.search.config.igGraphSubdomainBaseUrl
import com.trufflear.search.influencer.AccountInterceptor
import com.trufflear.search.influencer.services.InfluencerPostHandlingService
import com.trufflear.search.influencer.database.scripts.CreateInfluencerScript
import com.trufflear.search.influencer.services.InfluencerAccountConnectIgService
import com.trufflear.search.influencer.services.InfluencerAccountService
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import com.trufflear.search.influencer.network.service.StorageService
import com.trufflear.search.influencer.network.service.InstagramService
import com.trufflear.search.influencer.network.service.S3Service
import com.trufflear.search.influencer.network.service.SearchIndexService
import com.trufflear.search.influencer.repositories.InfluencerPostRepository
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.SearchIndexRepository
import com.trufflear.search.influencer.services.IgHandlingService
import com.trufflear.search.influencer.services.InfluencerPublicProfileService
import com.trufflear.search.influencer.util.CaptionParser
import com.trufflear.search.influencer.util.hashTagRegex
import com.trufflear.search.influencer.util.mentionTagRegex
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import org.apache.log4j.BasicConfigurator
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class TruffleSearchApplication(
    private val port: Int,
    influencerProfileRepository: InfluencerProfileRepository,
    searchIndexRepository: SearchIndexRepository,
    instagramService: InstagramService,
    igHandlingService: IgHandlingService,
    storageService: StorageService
) {

    val server: Server = ServerBuilder
        .forPort(port)
        .addService(ServerInterceptors.intercept(
            InfluencerAccountService(influencerProfileRepository, searchIndexRepository, storageService), AccountInterceptor())
        )
        .addService(ServerInterceptors.intercept(
            InfluencerAccountConnectIgService(
                influencerProfileRepository, instagramService, igHandlingService
            ), AccountInterceptor())
        )
        .addService(InfluencerPublicProfileService(influencerProfileRepository, storageService))
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@TruffleSearchApplication.stop()
                println("*** server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}

fun main() {
    val datasource = getHikariDataSource()
    CreateInfluencerScript.createInfluencer(datasource)

    BasicConfigurator.configure()
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val profileRepository = InfluencerProfileRepository(datasource)
    val storageService = S3Service(
        getS3Client(), profileRepository
    )

    val instagramService = InstagramService(igAuthService(), igGraphService())

    val server = TruffleSearchApplication(
        port,
        profileRepository,
        SearchIndexRepository(SearchIndexService()),
        instagramService,
        IgHandlingService(
            captionParser = CaptionParser(
                hashTagRegex = hashTagRegex,
                mentionTagRegex = mentionTagRegex,
            ),
            igService = instagramService,
            storageService = storageService,
            influencerProfileRepository = profileRepository,
            influencerPostHandlingService = InfluencerPostHandlingService(InfluencerPostRepository(datasource))
        ),
        storageService
    )
    server.start()
    server.blockUntilShutdown()
}

private fun getS3Client() = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.US_WEST_1)
    .withCredentials(
        AWSStaticCredentialsProvider(
            BasicAWSCredentials(
                System.getenv("S3_ACCESS_KEY"),
                System.getenv("S3_SECRET_KEY")
            )
        )
    )
    .build()

private fun getIgApiSubdomainRetrofit() =
    Retrofit.Builder()
        .baseUrl(igApiSubdomainBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

private fun igAuthService() = getIgApiSubdomainRetrofit().create(IgAuthService::class.java)

private fun getIgGraphSubdomainRetrofit() =
    Retrofit.Builder()
        .baseUrl(igGraphSubdomainBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

private fun igGraphService() = getIgGraphSubdomainRetrofit().create(IgGraphService::class.java)

private fun getHikariDataSource() =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}"
            username = System.getenv("DB_USER")
            password = System.getenv("DB_PASSWORD")
            driverClassName = "com.mysql.cj.jdbc.Driver"
            addDataSourceProperty("rewriteBatchedStatements", "true")
        }
    )