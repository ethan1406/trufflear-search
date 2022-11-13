package com.trufflear.search

import com.trufflear.search.config.igApiSubdomainBaseUrl
import com.trufflear.search.config.igGraphSubdomainBaseUrl
import com.trufflear.search.influencer.AccountInterceptor
import com.trufflear.search.influencer.InfluencerAccountConnectIgService
import com.trufflear.search.influencer.InfluencerAccountService
import com.trufflear.search.influencer.database.scripts.CreateInfluencerScript
import com.trufflear.search.influencer.network.service.IgAuthService
import com.trufflear.search.influencer.network.service.IgGraphService
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
    captionParser: CaptionParser
) {

    val server: Server = ServerBuilder
        .forPort(port)
        .addService(ServerInterceptors.intercept(
            InfluencerAccountService(dataSource), AccountInterceptor())
        )
        .addService(ServerInterceptors.intercept(
            InfluencerAccountConnectIgService(
                dataSource, igAuthService, igGraphService, captionParser
            ), AccountInterceptor())
        )
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

    //CreateInfluencerScript.createInfluencer(datasource)

    BasicConfigurator.configure()
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = TruffleSearchApplication(
        port,
        datasource,
        igAuthService(),
        igGraphService(),
        CaptionParser(
            hashTagRegex = "(#[a-zA-Z\\d-+_.]+)".toRegex(),
            mentionTagRegex = "(@[a-zA-Z\\d-+_.]+)".toRegex(),
        )
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