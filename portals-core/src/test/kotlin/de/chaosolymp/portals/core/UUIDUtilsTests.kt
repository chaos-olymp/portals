package de.chaosolymp.portals.core

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(Theories::class)
class UUIDUtilsTests {
    companion object {
        @DataPoints
        @JvmField
        val data: List<Pair<UUID, ByteArray>> = sequence {
            yield(Pair(UUID.fromString("375e2a8d-ab90-4601-adb1-23acafbd0c55"), byteArrayOf(0)))
            yield(Pair(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), byteArrayOf(0)))
        }.toList()
    }

    @Theory
    fun testGetBytesFromUUID(pair: Pair<UUID, ByteArray>) {
        val input = pair.first
        val expected = pair.second

        val actual = UUIDUtils.getBytesFromUUID(input)
        assertContentEquals(expected, actual)
    }

    @Theory
    fun testGetUUIDFromBytes(pair: Pair<UUID, ByteArray>) {
        val input = pair.second
        val expected = pair.first

        val actual = UUIDUtils.getUUIDFromBytes(input)
        assertEquals(expected, actual)
    }
}