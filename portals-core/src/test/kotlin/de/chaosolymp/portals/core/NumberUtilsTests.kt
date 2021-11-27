package de.chaosolymp.portals.core

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.FromDataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(Theories::class)
class NumberUtilsTests {
    companion object {
        private const val signedDataPointName: String = "signed"
        private const val unsignedDataPointName: String = "unsigned"

        @DataPoints(signedDataPointName)
        @JvmField
        val signedData: List<Pair<String, Boolean>> = sequence {
            yield(Pair("123", true))
            yield(Pair("400", true))
            yield(Pair("2147483647", true))
            yield(Pair("2147483648", true))
            yield(Pair("-1", true))

            yield(Pair("abc", false))
            yield(Pair("2ac", false))
            yield(Pair("a2c", false))
        }.toList()

        @DataPoints(unsignedDataPointName)
        @JvmField
        val unsignedData: List<Pair<String, Boolean>> = sequence {
            yield(Pair("123", true))
            yield(Pair("400", true))
            yield(Pair("2147483647", true))
            yield(Pair("2147483648", true))
            yield(Pair("-1", false))

            yield(Pair("abc", false))
            yield(Pair("2ac", false))
            yield(Pair("a2c", false))
        }.toList()
    }

    @Theory
    fun testIsNumber(@FromDataPoints(signedDataPointName) input: Pair<String, Boolean>) {
        val expected = input.second
        val actual = NumberUtils.isNumber(input.first)

        assertEquals(expected, actual)
    }

    @Theory
    fun testIsUnsignedNumber(@FromDataPoints(unsignedDataPointName) input: Pair<String, Boolean>) {
        val expected = input.second
        val actual = NumberUtils.isUnsignedNumber(input.first)

        assertEquals(expected, actual)
    }
}