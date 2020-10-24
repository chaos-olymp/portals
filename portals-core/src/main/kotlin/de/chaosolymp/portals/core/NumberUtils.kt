package de.chaosolymp.portals.core

object NumberUtils {
    private val numberRegex = Regex("-?\\d+(\\.\\d+)?")
    private val unsignedNumberRegex = Regex("\\d+(\\.\\d+)?")

    fun isNumber(string: String) = numberRegex.matches(string)

    fun isUnsignedNumber(string: String) = unsignedNumberRegex.matches(string)
}