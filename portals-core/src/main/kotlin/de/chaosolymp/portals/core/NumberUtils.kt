package de.chaosolymp.portals.core

object NumberUtils {
    private val numberRegex = Regex("-?\\d+(\\.\\d+)?")

    fun isNumber(string: String) = numberRegex.matches(string)
}