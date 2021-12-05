package de.chaosolymp.portals.core

fun interface PrepareStatementCallback {
    fun callback(sql: String)
}