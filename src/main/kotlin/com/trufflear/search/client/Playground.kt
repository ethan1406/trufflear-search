//package com.trufflear.search.client
//
//import java.sql.Date
//import java.sql.Timestamp
//import java.text.SimpleDateFormat
//
//fun main() {
//    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
//    val parsedDate = dateFormat.parse("2019-12-09T07:00:48+0000")
//
//    val timestamp = Timestamp(parsedDate.time)
//    val instant = timestamp.toInstant()
//    val date = Date.from(instant)
//    val reverseTimestamp = Timestamp.from(instant)
//
//    val instantString = instant.toString()
//    val dateFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
//    val parsedDate2 = dateFormat2.parse(instantString)
//
//    println(parsedDate2.time)
//    println(parsedDate.time)
//
////    println(timestamp)
////    println(reverseTimestamp)
////    println(instant)
////    println(dateFormat.format(date))
////    println(timestamp.time)
////    println(parsedDate.time)
////    println(instant.toEpochMilli())
//
////    val test = "Just looking for that text back. #joshuatr.ee PC: @victoria_chu.ang"
////    val regex = "(#[a-zA-Z\\d-+_.]+)".toRegex()
////    //val regex2 = "/(^|\\W)(#[a-z\\d][\\w-]*)/ig".toRegex()
////    val result = regex.findAll(test).toList().map {
////        it.value
////    }.joinToString(separator = " ")
////    //val result = regex.replace(test, "BOAGAN")
//
//    //println(result)
//}