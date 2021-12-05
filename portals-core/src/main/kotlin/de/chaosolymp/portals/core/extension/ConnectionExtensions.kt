package de.chaosolymp.portals.core.extension

import de.chaosolymp.portals.core.PrepareStatementCallback
import java.sql.Connection
import java.sql.PreparedStatement

fun Connection.prepareAndLogStatement(callback: PrepareStatementCallback?, sql: String): PreparedStatement {
    val statement = prepareStatement(sql)
    callback?.callback(sql)

    return statement
}

fun Connection.prepareAndLogStatement(callback: PrepareStatementCallback?, sql: String, autoGeneratedKeys: Int): PreparedStatement {
    val statement = prepareStatement(sql, autoGeneratedKeys)
    callback?.callback(sql)

    return statement
}