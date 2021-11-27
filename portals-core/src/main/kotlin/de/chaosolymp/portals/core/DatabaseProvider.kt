package de.chaosolymp.portals.core

import java.sql.Connection

interface DatabaseProvider {
    val connection: Connection
}

inline fun <R> DatabaseProvider.useConnection(block: (Connection) -> R): R {
    return connection.use {
        return@use block(it)
    }
}