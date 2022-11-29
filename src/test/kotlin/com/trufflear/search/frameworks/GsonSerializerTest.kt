package com.trufflear.search.frameworks

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.trufflear.search.frameworks.interfaces.GsonSerializer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList

class GsonSerializerTest {
    private val gsonSerializer = GsonSerializer(
        GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create()
    )

    @Test
    fun arrayListOfStringsToJson() {
        val stringList: MutableList<String> = ArrayList()
        stringList.add("1")
        stringList.add("two")
        stringList.add("3")
        val json = gsonSerializer.toJson<List<String>>(stringList)
        Assertions.assertThat(json).isEqualTo("[\"1\",\"two\",\"3\"]")
    }

    @Test
    fun arrayListOfStringsFromJson() {
        val json = "[\"1\",\"two\",\"3\"]"
        val type = object : TypeToken<List<String?>?>() {}.type
        val stringList = gsonSerializer.fromJson<List<String>>(json, type)
        Assertions.assertThat(stringList).containsExactly("1", "two", "3")
    }

    @Test
    fun primitiveIntToJson() {
        val i = 100
        val json = gsonSerializer.toJson(i)
        Assertions.assertThat(json).isEqualTo("100")
    }

    @Test
    fun primitiveIntFromJson() {
        val json = "100"
        val i = gsonSerializer.fromJson(json, Int::class.java)
        Assertions.assertThat(i).isEqualTo(100)
    }

    @Test
    fun nonPrimitiveIntToJson() {
        val i = 100
        val json = gsonSerializer.toJson(i)
        Assertions.assertThat(json).isEqualTo("100")
    }

    @Test
    fun nonPrimitiveIntFromJson() {
        val json = "100"
        val i = gsonSerializer.fromJson(json, Int::class.java)
        Assertions.assertThat(i).isEqualTo(Integer.valueOf(100))
    }

    @Test
    fun nestedArrayToJson() {
        val nestedList: MutableList<List<String>> = ArrayList()
        nestedList.add(Arrays.asList("1", "2"))
        nestedList.add(Arrays.asList("3", "4"))
        val json = gsonSerializer.toJson<List<List<String>>>(nestedList)
        Assertions.assertThat(json).isEqualTo("[[\"1\",\"2\"],[\"3\",\"4\"]]")
    }

    @Test
    fun nestedArrayFromJson() {
        val json = "[[\"1\",\"2\"],[\"3\",\"4\"]]"
        val type = object : TypeToken<List<List<String?>?>?>() {}.type
        val nestedList = gsonSerializer.fromJson<List<List<String>>>(json, type)
        Assertions.assertThat(nestedList).containsExactly(Arrays.asList("1", "2"), Arrays.asList("3", "4"))
    }

    internal class PersonWithFinalFields(val name: String, val age: Int)

    @Test
    fun classWithFinalFieldsToJson() {
        val person = PersonWithFinalFields("ben", 10)
        val json = gsonSerializer.toJson(person)
        Assertions.assertThat(json)
            .contains("\"name\":\"ben\"")
            .contains("\"age\":10")
    }

    @Test
    fun classWithFinalFieldsFromJson() {
        val json = "{\"name\":\"ben\",\"age\":10}"
        val person = gsonSerializer.fromJson(
            json,
            PersonWithFinalFields::class.java
        )
        Assertions.assertThat(person).isNotNull
        Assertions.assertThat(person.age).isNotNull
            .isEqualTo(10)
        Assertions.assertThat(person.name).isEqualTo("ben")
    }


    @Test
    fun deserializeEmpty() {
        val json = ""
        Assertions.assertThatExceptionOfType(JsonSyntaxException::class.java)
            .isThrownBy {
                gsonSerializer.fromJson(
                    json,
                    PersonWithFinalFields::class.java
                )
            }
            .withMessage(
                ("Gson deserialization returned a null object. Was looking for type: class "
                        + PersonWithFinalFields::class.java.name)
            )
    }

    internal class ClassWithPrivateFields {
        internal var i = 0
    }

    @Test
    fun privateFieldsToJson() {
        val classWithPrivateFields = ClassWithPrivateFields()
        classWithPrivateFields.i = 100
        val json = gsonSerializer.toJson(classWithPrivateFields)
        Assertions.assertThat(json).isEqualTo("{\"i\":100}")
    }

    @Test
    fun privateFieldsFromJson() {
        val json = "{\"i\":100}"
        val classWithPrivateFields = gsonSerializer.fromJson(
            json,
            ClassWithPrivateFields::class.java
        )
        Assertions.assertThat(classWithPrivateFields.i).isEqualTo(100)
    }

    internal open class A() {
        var a: String? = null
    }

    internal class B() : A() {
        var b: String? = null
    }

    @Test
    fun classThatExtendsAnotherClassToJson() {
        val b = B()
        b.a = "a"
        b.b = "b"
        val json = gsonSerializer.toJson(b)
        Assertions.assertThat(json)
            .contains("\"b\":\"b\"")
            .contains("\"a\":\"a\"")
    }

    @Test
    fun classThatExtendsAnotherClassFromJson() {
        val json = "{\"b\":\"b\",\"a\":\"a\"}"
        val b = gsonSerializer.fromJson(json, B::class.java)
        Assertions.assertThat(b).isNotNull
        Assertions.assertThat(b.a).isEqualTo("a")
        Assertions.assertThat(b.b).isEqualTo("b")
    }

    @Test
    fun classThatExtendsAnotherClass2FromJson() {
        val json = "{\"b\":\"b\",\"a\":\"a\"}"
        val a = gsonSerializer.fromJson(json, A::class.java)
        Assertions.assertThat(a).isNotNull
        Assertions.assertThat(a.a).isEqualTo("a")
    }

    internal class TransientFields() {
        @Transient
        var a = 0
        var b = 0
    }

    @Test
    fun transientFieldsAreIgnoredToJson() {
        val transientFields = TransientFields()
        transientFields.a = 1
        transientFields.b = 2
        val json = gsonSerializer.toJson(transientFields)
        Assertions.assertThat(json).isEqualTo("{\"b\":2}")
    }

    @Test
    fun transientFieldsAreIgnoredFromJson() {
        val json = "{\"a\":1,\"b\":2}"
        val transientFields = gsonSerializer.fromJson(
            json,
            TransientFields::class.java
        )
        Assertions.assertThat(transientFields.a).isEqualTo(0)
        Assertions.assertThat(transientFields.b).isEqualTo(2)
    }

    @Test
    fun nonLatinStrings() {
        val nonLatinTexts = arrayOf(
            "සිංහල ජාතිය", "日本語", "Русский", "فارسی", "한국어", "Հայերեն", "हिन्दी", "עברית", "中文", "አማርኛ", "മലയാളം",
            "ܐܬܘܪܝܐ", "მარგალური"
        )
        for (nonLatinText: String in nonLatinTexts) {
            val toJson = gsonSerializer.toJson(nonLatinText)
            Assertions.assertThat(toJson).isEqualTo("\"" + nonLatinText + "\"")
            val fromJson = gsonSerializer.fromJson(toJson, String::class.java)
            Assertions.assertThat(fromJson).isEqualTo(nonLatinText)
        }
    }

    @Test
    fun doubleTest1() {
        val s = "123"
        Assertions.assertThat(gsonSerializer.fromJson(s, Double::class.java))
            .isEqualTo(java.lang.Double.valueOf(123.0))
    }

    @Test
    fun doubleTest2() {
        val s = "123.5"
        Assertions.assertThat(gsonSerializer.fromJson(s, Double::class.java))
            .isEqualTo(java.lang.Double.valueOf(123.5))
    }

    @Test
    fun doubleTest3() {
        val s = "123.5E1"
        Assertions.assertThat(gsonSerializer.fromJson(s, Double::class.java))
            .isEqualTo(java.lang.Double.valueOf(1235.0))
    }

    @Test
    fun doubleNAN() {
        val json = "[" + Double.NaN + "]"
        val fromJson = gsonSerializer.fromJson(
            json,
            Array<Double>::class.java
        )
        Assertions.assertThat(fromJson).containsExactly(Double.NaN)
        val toJson = gsonSerializer.toJson(fromJson)
        Assertions.assertThat(toJson).isEqualTo(json)
    }

    @Test
    fun maxLong() {
        val json = "[" + Long.MAX_VALUE + "]"
        val fromJson = gsonSerializer.fromJson(json, Array<Long>::class.java)
        Assertions.assertThat(fromJson).containsExactly(Long.MAX_VALUE)
        val toJson = gsonSerializer.toJson(fromJson)
        Assertions.assertThat(toJson).isEqualTo(json)
    }

    @Test
    fun minLong() {
        val json = "[" + Long.MIN_VALUE + "]"
        val fromJson = gsonSerializer.fromJson(json, Array<Long>::class.java)
        Assertions.assertThat(fromJson).containsExactly(Long.MIN_VALUE)
        val toJson = gsonSerializer.toJson(fromJson)
        Assertions.assertThat(toJson).isEqualTo(json)
    }

    @Test
    fun minBigInt() {
        val bigInt = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)
        val json = "[$bigInt]"
        val fromJson = gsonSerializer.fromJson(
            json,
            Array<BigInteger>::class.java
        )
        Assertions.assertThat(fromJson).containsExactly(bigInt)
        val toJson = gsonSerializer.toJson(fromJson)
        Assertions.assertThat(toJson).isEqualTo(json)
    }

    @Test
    fun maxBigInt() {
        val bigInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        val json = "[$bigInt]"
        val fromJson = gsonSerializer.fromJson(
            json,
            Array<BigInteger>::class.java
        )
        Assertions.assertThat(fromJson).containsExactly(bigInt)
        val toJson = gsonSerializer.toJson(fromJson)
        Assertions.assertThat(toJson).isEqualTo(json)
    }

    internal class NullField() {
        var i: String? = null
    }

    @Test
    fun nullFieldToJson() {
        val nullField = NullField()
        nullField.i = null
        val json = gsonSerializer.toJson(nullField)
        Assertions.assertThat(json).isEqualTo("{}")
    }

    @Test
    fun nullFieldFromJson() {
        val json = "{\"i\":null}"
        val nullField = gsonSerializer.fromJson(
            json,
            NullField::class.java
        )
        Assertions.assertThat(nullField.i).isNull()
    }
}