package com.trufflear.search

import com.trufflear.search.influencer.AccountInterceptor
import com.trufflear.search.influencer.InfluencerAccountService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import javax.sql.DataSource


class TruffleSearchApplication(
    private val port: Int,
    dataSource: DataSource
) {

    val server: Server = ServerBuilder
        .forPort(port)
        .addService(ServerInterceptors.intercept(
            InfluencerAccountService(dataSource), AccountInterceptor())
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

private fun getHikariDataSource() =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}"
            username = System.getenv("DB_USER")
            password = System.getenv("DB_PASSWORD")
            driverClassName = "com.mysql.cj.jdbc.Driver"
        }
    )

fun main() {
    val datasource = getHikariDataSource()

    //CreateInfluencerScript.createInfluencer(datasource)

    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = TruffleSearchApplication(port, datasource)
    server.start()
    server.blockUntilShutdown()
}
