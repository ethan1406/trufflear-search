package com.trufflear.search.influencer.database.scripts

import com.trufflear.search.influencer.database.models.InfluencerDbDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

object CreateInfluencerScript {

    fun createInfluencer(dataSource: DataSource) {
        transaction (Database.connect(dataSource)){
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(InfluencerDbDto)

//            InfluencerDbDto.insert {
//                it[name] = "Ethan Chang"
//                it[email] = "test@gmail.com"
//                it[isEmailVerified] = false
//                it[bioDescription] = "test"
//                it[title] = "test"
//                it[profileImageUrl] = "tst"
//            }

        }
    }

}