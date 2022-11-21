package com.trufflear.search

import com.trufflear.search.config.igApiSubdomainBaseUrl
import com.trufflear.search.config.igGraphSubdomainBaseUrl
import com.trufflear.search.influencer.AccountInterceptor
import com.trufflear.search.influencer.database.scripts.CreateInfluencerScript
import com.trufflear.search.influencer.services.InfluencerAccountConnectIgService
import com.trufflear.search.influencer.services.InfluencerAccountService
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
import com.trufflear.search.influencer.network.service.SearchIndexService
import com.trufflear.search.influencer.repositories.InfluencerProfileRepository
import com.trufflear.search.influencer.repositories.SearchIndexRepository
import com.trufflear.search.influencer.services.InfluencerPublicProfileService
import com.trufflear.search.influencer.util.CaptionParser
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import org.apache.log4j.BasicConfigurator
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.sql.DataSource


class TruffleSearchApplication(
    private val port: Int,
    dataSource: DataSource,
    igAuthService: IgAuthService,
    igGraphService: IgGraphService,
    captionParser: CaptionParser,
    searchIndexService: SearchIndexService,
    influencerProfileRepository: InfluencerProfileRepository,
    searchIndexRepository: SearchIndexRepository
) {

    val server: Server = ServerBuilder
        .forPort(port)
        .addService(ServerInterceptors.intercept(
            InfluencerAccountService(influencerProfileRepository, searchIndexRepository), AccountInterceptor())
        )
        .addService(ServerInterceptors.intercept(
            InfluencerAccountConnectIgService(
                dataSource, igAuthService, igGraphService, captionParser
            ), AccountInterceptor())
        )
        .addService(InfluencerPublicProfileService(influencerProfileRepository))
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

    //CreateInfluencerScript.createInfluencer(datasource)

    BasicConfigurator.configure()
    val port = System.getenv("PORT")?.toInt() ?: 50051

    val datasource = getHikariDataSource()
    val service = SearchIndexService()
    val server = TruffleSearchApplication(
        port,
        datasource,
        igAuthService(),
        igGraphService(),
        CaptionParser(
            hashTagRegex = "(#[^\\s\\\\]+)".toRegex(),
            mentionTagRegex = "(@[a-zA-Z\\d-+_.]+)".toRegex(),
        ),
        service,
        InfluencerProfileRepository(datasource),
        SearchIndexRepository(service)
    )
    server.start()
    server.blockUntilShutdown()
}

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