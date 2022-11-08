//package com.trufflear.search
//
//import org.typesense.api.Client
//import org.typesense.api.Configuration
//import org.typesense.model.SearchParameters
//import org.typesense.resources.Node
//import java.time.Duration
//
//
//fun main() {
//
//    val nodes: ArrayList<Node> = ArrayList<Node>()
//    nodes.add(
//        Node(
//            "http",  // For Typesense Cloud use https
//            "localhost",  // For Typesense Cloud use xxx.a1.typesense.net
//            "8108" // For Typesense Cloud use 443
//        )
//    )
//
//    val configuration = Configuration(nodes, Duration.ofSeconds(2), "KingOysterBoo")
//
//    val client = Client(configuration)
//
//    val hmap = HashMap<String, Any>()
//    hmap["countryName"] = "India Kingdom"
//    hmap["capital"] = "Delhi King"
//    hmap["gdp"] = 20
//
//    val searchParameters = SearchParameters()
//        .q("India Kingdom")
//        .queryBy("countryName,capital")
//        .prefix("true,false")
//    val searchResult = client.collections("Countries").documents().search(searchParameters)
//
//    println(searchResult)
//
//}
