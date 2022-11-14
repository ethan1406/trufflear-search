package com.trufflear.search.client

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

fun main() {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    val parsedDate = dateFormat.parse("2022-11-12 23:12:41.000000")

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

//    println(timestamp)
//    println(reverseTimestamp)
//    println(instant)
//    println(dateFormat.format(date))
//    println(timestamp.time)
    println(parsedDate.time)
//    println(instant.toEpochMilli())

//    val test = "#3434 #testing123 #dasdf\r 超快速蕃茄牛肉麵簡易食譜 beef noodle  只要給我20分鐘 就給你一碗好吃到噴淚的牛肉麵！  #這款食譜一定要珍藏！  \uD83D\uDCCD材料 ①Kindfood 深夜麻油秘製牛腱心 ②洋蔥、蒜頭、鮮菇、蕃茄 ③醬油、雞粉、番茄醬 ④泡麵或其他麵類皆可  \uD83D\uDCCD步驟 ①將kindfood牛肉調理包放入熱水中浸泡20分鐘至溫熱即可。  ②將蕃茄、蒜末、鮮菇、洋蔥、及上述調味料放入滾水中煮至軟爛。  ③加入泡麵煮到你想要的軟度。  ④放入kindfood牛肉調理包，搭配你想要的配料即可。  追蹤我看更多食譜\uD83D\uDC49 @cook_by_yourselfff （Kindfood 深夜麻油秘製牛腱心可至我的首頁點選連結購買）  ——————————————————————————— #懶人料理\n#懶人食譜 #懶人廚房 #食譜分享 #簡易食譜 #簡單料理 #料理初心者 #餐桌日記 #餐桌風景 #晚餐自己煮 #午餐吃什麼 #便當料理 #上班族便當 #健康便當 #便當人生 #牛丼弁当 #牛丼 #健康食譜 #家常菜食譜 #家常料理 #今日晚餐 #晚餐日常 #料理 #家常料理 #台式料理"
//    val test2 = "\\u9999\\u8178\\u7092\\u8c46\\u4e7e\\n#\\u5e38\\u5099\\u83dc #\\u5976\\u5976\\u7684\\u83dc #\\u6599\\u7406\\n\\u5a46\\u5a46\\u536f\\u8d77\\u4f86\\u8cb7\\u4e86\\u7d041/5\\u500b\\u51b7\\u51cd\\u5eab\\u7684\\u9ebb\\u8fa3\\u9999\\u8178\\uff08\\u7d55\\u7121\\u8a87\\u98fe\\uff0c\\u8fa3\\u5ea6\\u9ad8\\u5230\\u4e00\\u4eba\\u9802\\u591a\\u5403\\u534a\\u689d\\uff0c\\u6d88\\u8017\\u7684\\u901f\\u5ea6\\u904e\\u65bc\\u7de9\\u6162\\u3002\\n\\n\\u7a81\\u7136\\u60f3\\u8d77\\u5c0f\\u6642\\u5019\\u5976\\u5976\\u90fd\\u6703\\u7092\\u5f88\\u591a\\u9e79\\u9e79\\u6cb9\\u6cb9\\u53c8\\u4e0b\\u98ef\\u7684\\u83dc\\uff0c\\u62dc\\u62dc\\u5b8c\\u7684\\u9999\\u8178\\u7092\\u8c46\\u4e7e\\uff0c\\u6216\\u8005\\u5403\\u5269\\u4e00\\u9ede\\u9ede\\u7684\\u83dc\\u812f\\u7092\\u9999\\u8178\\uff0c\\u76f8\\u7576\\u4e0b\\u98ef\\u5440\\uff01\\n\\n\\u8fa3\\u5230\\u5403\\u4e0d\\u5b8c\\u7684\\u9ebb\\u8fa3\\u9999\\u8178\\uff0c\\u4e5f\\u53ef\\u4ee5\\u7528\\u540c\\u6a23\\u7684\\u65b9\\u5f0f\\u89e3\\u6c7a\\u4ed6\\u5427\\uff01\\n\\n\\u6750\\u6599\\n\\u9999\\u8178 \\u96a8\\u559c\\n\\u8c46\\u4e7e \\u96a8\\u559c\\n\\u83dc\\u812f \\u96a8\\u559c\\n\\u7cd6 \\u96a8\\u559c\\n\\u91ac\\u6cb9 \\u4e00\\u9ede\\u5c31\\u597d\\n\\n\\u6bcf\\u6b21\\u90fd\\u4ee5\\u70ba\\u53ef\\u4ee5\\u5403\\u4e00\\u9031\\uff0c\\u5927\\u6982\\u5169\\u4e09\\u9910\\u5c31\\u88ab\\u6eab\\u5c2a\\u5403\\u5149\\u5149\\uff01\\n\\n\\u6700\\u5f8c\\u9084\\u5077\\u7528\\u4e86 \\u0040lees_is_me \\u7684\\u53f0\\u8a5e\\uff0c\\u8b9a\\u8b9a\\u63b0\\u63b0\\u3002\\n\\n#\\u651d\\u5f71\\u5e2b \\u0040willy20629\\n#\\u6599\\u7406\\u65e5\\u5e38 #\\u9910\\u684c\\u98a8\\u666f #\\u665a\\u9910\\u5403\\u4ec0\\u9ebc #\\u98df\\u7269\\u651d\\u5f71"
//    val regex = "(#[^\\s\\\\]+)".toRegex()
//    val oldRegex = "(#[a-zA-Z\\d-+_.]+)".toRegex()
//    //val regex2 = "/(^|\\W)(#[a-z\\d][\\w-]*)/ig".toRegex()
//    val result = regex.findAll(test).toList().map {
//        it.value
//    }.joinToString(separator = " ")
//    //val result = regex.replace(test, "BOAGAN")
//
//    println(result)
}