package com.trufflear.search.frameworks.interfaces

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.trufflear.search.frameworks.impl.IJsonSerializer
import java.io.Reader
import java.lang.reflect.Type

class GsonSerializer(val gson: Gson) : IJsonSerializer {
    override fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return firstNonNull<T>(gson.fromJson(json, classOfT), null, classOfT)
    }

    override fun <T> fromJson(json: Reader, classOfT: Class<T>): T {
        return firstNonNull(gson.fromJson(json, classOfT), null, classOfT)
    }

    override fun <T> fromJson(json: String, typeOfT: Type): T {
        val fromJson: T = gson.fromJson(json, typeOfT)
        return firstNonNull(fromJson, null, typeOfT)
    }

    override fun <T> fromJson(json: Reader, typeOfT: Type, defaultValue: T): T {
        return firstNonNull(gson.fromJson(json, typeOfT), defaultValue, typeOfT)
    }

    override fun <T> toJson(src: T, classOfT: Class<T>): String {
        return gson.toJson(src, classOfT)
    }

    override fun <T> toJson(src: T, typeOfT: Type): String {
        return gson.toJson(src, typeOfT)
    }

    override fun <T> toJson(src: T): String {
        return gson.toJson(src)
    }

    companion object {
        private const val DESERIALIZATION_ERROR_MESSAGE_FORMAT =
            "Gson deserialization returned a null object. Was looking for type: %s"

        private fun <T> firstNonNull(item1: T?, item2: T?, messageFormatArg: Any): T {
            if (item1 == null) {
                if (item2 == null) {
                    throw JsonSyntaxException(
                        formatString(
                            DESERIALIZATION_ERROR_MESSAGE_FORMAT,
                            messageFormatArg
                        )
                    )
                }
                return item2
            }
            return item1
        }
        private fun formatString(message: String, vararg args: Any?): String = if (args.isEmpty()) message else message.format(*args)
    }
}
