package com.trufflear.search.frameworks.impl

import java.io.Reader
import java.lang.reflect.Type

interface IJsonSerializer {

    fun <T> fromJson(json: String, classOfT: Class<T>): T

    fun <T> fromJson(json: Reader, classOfT: Class<T>): T

    fun <T> fromJson(json: Reader, typeOfT: Type, defaultValue: T): T

    fun <T> fromJson(json: String, typeOfT: Type): T

    fun <T> toJson(src: T, classOfT: Class<T>): String

    fun <T> toJson(src: T, typeOfT: Type): String

    fun <T> toJson(src: T): String
}
